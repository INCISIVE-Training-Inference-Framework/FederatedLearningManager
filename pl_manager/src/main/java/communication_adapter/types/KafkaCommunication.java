package communication_adapter.types;

import ai_engine_adapter.AIEngineAdapter;
import communication_adapter.CommunicationAdapter;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.AIEngineException;
import exceptions.BadConfigurationException;
import exceptions.CommunicationException;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaCommunication implements CommunicationAdapter {

    private static final Logger logger = LogManager.getLogger(KafkaCommunication.class);
    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = CommunicationAdapter.getEnvironmentVariables();
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_BOOTSTRAP_SERVERS_CONFIG", EnvironmentVariableType.STRING));
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_POLL_TIME_OUT", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_MAX_INITIALIZATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_MAX_ITERATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_MAX_MODEL_SIZE", EnvironmentVariableType.INTEGER)); // KB
        return abstractClassVariables;
    }

    private final String jobCompleteId;
    private final int numberOfPods;

    private final String bootstrap_servers_config;
    private final long pollTimeOut;
    private final long maxInitializationTime;
    private final long maxIterationTime;
    private final long maxModelSize;

    private Producer<String, String> statusProducer;
    private Producer<String, byte[]> modelsToClientsProducer;
    private Consumer<String, byte[]> modelsToManagerConsumer;
    private Consumer<String, String> statusConsumer;

    public KafkaCommunication(Map<String, Object> config, String jobCompleteId, int numberOfPods) throws BadConfigurationException {
        this.jobCompleteId = jobCompleteId;
        this.numberOfPods = numberOfPods;

        this.bootstrap_servers_config = (String) config.get("KAFKA_BOOTSTRAP_SERVERS_CONFIG");
        this.pollTimeOut = (long) config.get("KAFKA_POLL_TIME_OUT");
        this.maxInitializationTime = (long) config.get("KAFKA_MAX_INITIALIZATION_TIME") * 1000;
        this.maxIterationTime = (long) config.get("KAFKA_MAX_ITERATION_TIME") * 1000;
        this.maxModelSize = (int) config.get("KAFKA_MAX_MODEL_SIZE") * 1000;
    }

    @Override
    public void initialize() throws CommunicationException {
        createConsumers();
        createProducers();
    }

    @Override
    public void sendManagerInitializationMessage(boolean success) throws CommunicationException {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    "status",
                    this.jobCompleteId,  // key
                    String.valueOf(success)  // value
            );
            record.headers().add("message_name", "manager_initialization".getBytes(StandardCharsets.UTF_8));
            this.statusProducer.send(record).get();
            logger.debug("Manager initialization message sent");
        } catch (InterruptedException | ExecutionException e) {
            throw new CommunicationException("Error while sending manager initialization message", e);
        }

        this.statusProducer.close();
    }

    @Override
    public void waitForAllClientInitializationMessages() throws CommunicationException {
        int numberOfReceivedMessages = 0;

        Timestamp startTime = Timestamp.from(Instant.now());
        while (numberOfReceivedMessages < numberOfPods) {
            ConsumerRecords<String, String> consumerRecords = this.statusConsumer.poll(Duration.ofSeconds(this.pollTimeOut));

            if (consumerRecords.count() > 0) {
                for (ConsumerRecord<String, String> record: consumerRecords) {
                    // key -> executionId (from manager) or executionId_podId (from client)
                    // value -> true or false
                    boolean fromClient = record.key().contains("_");
                    if (fromClient) {
                        boolean fromThisExecution = this.jobCompleteId.equals(record.key().split("_")[0]);
                        if (fromThisExecution) {
                            String podId = record.key().split("_")[1];
                            boolean success = Boolean.parseBoolean(record.value());
                            if (success) {
                                logger.debug("Started iteration message received from POD with id " + podId);
                                ++numberOfReceivedMessages;
                            } else {
                                throw new CommunicationException("The POD with id " + podId + " did not start well its iteration", null);
                            }
                        }
                    }
                }
            }

            Timestamp currentTime = Timestamp.from(Instant.now());
            if (currentTime.getTime() > startTime.getTime() + this.maxInitializationTime) {
                throw new CommunicationException("Some POD did not communicate the start of its iteration", null);
            }
        }

        try {
            this.statusConsumer.commitSync();
        } catch (CommitFailedException e) {
            throw new CommunicationException("Error while committing", e);
        }

        this.statusConsumer.close();
    }

    @Override
    public void waitForAllEndedIterationMessages(int iterationIndex, AIEngineAdapter aiEngineAdapter) throws CommunicationException, AIEngineException {
        int numberOfFinishedPods = 0;

        Timestamp startTime = Timestamp.from(Instant.now());
        while (numberOfFinishedPods < numberOfPods) {
            final ConsumerRecords<String, byte[]> consumerRecords = this.modelsToManagerConsumer.poll(Duration.ofSeconds(this.pollTimeOut));

            if (consumerRecords.count() > 0) {
                for (ConsumerRecord<String, byte[]> record: consumerRecords) {
                    // key -> executionId_podId
                    // value -> model or null
                    String podId = record.key().split("_")[1];
                    byte[] model = record.value();
                    if (model != null && model.length != 0) {
                        logger.debug("Ended iteration message received from POD with id " + podId);
                        aiEngineAdapter.saveUnMergedModel(podId, model);
                        ++numberOfFinishedPods;
                    } else {
                        throw new CommunicationException("The POD with id " + podId + " did not end well its iteration", null);
                    }
                }
            }

            Timestamp currentTime = Timestamp.from(Instant.now());
            if (currentTime.getTime() > startTime.getTime() + this.maxIterationTime) {
                throw new CommunicationException("Some POD did not communicate the end of its iteration", null);
            }
        }

        try {
            this.modelsToManagerConsumer.commitSync();
        } catch (CommitFailedException e) {
            throw new CommunicationException("Error while committing", e);
        }
    }

    @Override
    public void sendStartIterationMessage(int iterationIndex, byte[] model) throws CommunicationException {
        try {
            final ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                    this.jobCompleteId + "_models_to_clients",
                    jobCompleteId,  // key
                    model  // value
            );
            record.headers().add("message_name", "start_iteration".getBytes(StandardCharsets.UTF_8));
            record.headers().add("iteration_index", String.valueOf(iterationIndex).getBytes(StandardCharsets.UTF_8));
            this.modelsToClientsProducer.send(record).get();
            logger.debug("Start iteration message sent");
        } catch (InterruptedException | ExecutionException e) {
            throw new CommunicationException("Error while sending iteration start message", e);
        }
    }

    @Override
    public void cleanEnvironment() throws CommunicationException {
        this.modelsToClientsProducer.close();
        this.modelsToManagerConsumer.close();
    }

    private void createProducers() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, jobCompleteId + "_manager");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.statusProducer = new KafkaProducer<>(properties);

        properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, jobCompleteId + "_manager");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, this.maxModelSize + "");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        this.modelsToClientsProducer = new KafkaProducer<>(properties);
    }

    private void createConsumers() throws CommunicationException {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.jobCompleteId + "_manager");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, this.maxIterationTime + "");
        properties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, this.maxModelSize + "");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        this.modelsToManagerConsumer = new KafkaConsumer<>(properties);
        List<TopicPartition> partitions = new ArrayList<>();
        List<PartitionInfo> partitionInfos = this.modelsToManagerConsumer.partitionsFor(this.jobCompleteId + "_models_to_manager");
        if (partitionInfos == null || partitionInfos.size() == 0) throw new CommunicationException("Topic " + this.jobCompleteId + "_models_to_manager not already created", null);
        for (PartitionInfo partition : partitionInfos) partitions.add(new TopicPartition(partition.topic(), partition.partition()));
        this.modelsToManagerConsumer.assign(partitions);  // standalone consumer

        properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.jobCompleteId + "_manager");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        this.statusConsumer = new KafkaConsumer<>(properties);
        partitions = new ArrayList<>();
        partitionInfos = this.statusConsumer.partitionsFor("status");
        if (partitionInfos == null || partitionInfos.size() == 0) throw new CommunicationException("Topic status not already created", null);
        for (PartitionInfo partition : partitionInfos) partitions.add(new TopicPartition(partition.topic(), partition.partition()));
        this.statusConsumer.assign(partitions);  // standalone consumer
    }
}
