package communication_adapter.types;

import ai_engine_adapter.AIEngineAdapter;
import communication_adapter.CommunicationAdapter;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.AIEngineException;
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
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_INITIALIZATION_MESSAGE_TIME_OUT", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_START_ITERATION_MESSAGE_TIME_OUT", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_MAX_ITERATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KAFKA_MAX_MODEL_SIZE", EnvironmentVariableType.INTEGER));  // KB
        return abstractClassVariables;
    }

    private final String jobCompleteId;
    private final String podId;
    private final String bootstrapServersConfig;
    private final long pollTimeOut;
    private final long initializationMessageTimeOut;
    private final long startIterationMessageTimeOut;
    private final long maxIterationTime;
    private final long maxModelSize;

    private Consumer<String, String> statusConsumer;
    private Consumer<String, byte[]> modelsToClientsConsumer;
    private Producer<String, byte[]> modelsToManagerProducer;
    private Producer<String, String> statusProducer;

    public KafkaCommunication(Map<String, Object> config, String jobCompleteId, String podId) {
        this.jobCompleteId = jobCompleteId;
        this.podId = podId;

        this.bootstrapServersConfig = (String) config.get("KAFKA_BOOTSTRAP_SERVERS_CONFIG");
        this.pollTimeOut = (long) config.get("KAFKA_POLL_TIME_OUT");
        this.initializationMessageTimeOut = (long) config.get("KAFKA_INITIALIZATION_MESSAGE_TIME_OUT") * 1000;
        this.startIterationMessageTimeOut = (long) config.get("KAFKA_START_ITERATION_MESSAGE_TIME_OUT") * 1000;
        this.maxIterationTime = (long) config.get("KAFKA_MAX_ITERATION_TIME") * 1000;
        this.maxModelSize = (int) config.get("KAFKA_MAX_MODEL_SIZE") * 1000;
    }

    @Override
    public void initialize() throws CommunicationException {
        createProducers();
        createConsumers();
    }

    @Override
    public void sendClientInitializationMessage(boolean success) throws CommunicationException {
        try {
            final ProducerRecord<String, String> record = new ProducerRecord<>(
                    "status",
                    this.jobCompleteId + "_" + this.podId,  // key
                    String.valueOf(success)  // value
            );
            record.headers().add("message_name", "client_initialization".getBytes(StandardCharsets.UTF_8));
            this.statusProducer.send(record).get();
            logger.debug("Client initialization message sent");
        } catch (InterruptedException | ExecutionException e) {
            throw new CommunicationException("Error while sending client initialization message", e);
        }

        this.statusProducer.close();
    }

    @Override
    public void waitForManagerInitializationMessage() throws CommunicationException {
        boolean received = false;

        Timestamp startTime = Timestamp.from(Instant.now());
        while (!received) {
            final ConsumerRecords<String, String> consumerRecords = this.statusConsumer.poll(Duration.ofSeconds(this.pollTimeOut));

            if (consumerRecords.count() > 0) {
                for (ConsumerRecord<String, String> record: consumerRecords) {
                    // key -> executionId (from manager) or executionId_podId (from client)
                    // value -> true or false
                    boolean fromManager = !record.key().contains("_");
                    if (fromManager) {
                        boolean fromThisExecution = this.jobCompleteId.equals(record.key());
                        if (fromThisExecution) {
                            boolean success = Boolean.parseBoolean(record.value());
                            if (success) {
                                logger.debug("Manager initialization message received");
                                received = true;
                            } else {
                                throw new CommunicationException("The manager did not start well its iteration", null);
                            }
                        }
                    }
                }
            }

            Timestamp currentTime = Timestamp.from(Instant.now());
            if (currentTime.getTime() > startTime.getTime() + this.initializationMessageTimeOut) {
                throw new CommunicationException("The manager did not communicate the correct initialization", null);
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
    public void sendEndedIterationMessage(int iterationIndex, byte[] model) throws CommunicationException {
        try {
            final ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                    this.jobCompleteId + "_models_to_manager",
                    this.jobCompleteId + "_" + this.podId,  // key
                    model  // value
            );
            record.headers().add("message_name", "ended_iteration".getBytes(StandardCharsets.UTF_8));
            record.headers().add("iteration_index", String.valueOf(iterationIndex).getBytes(StandardCharsets.UTF_8));
            this.modelsToManagerProducer.send(record).get();
            logger.debug("Ended iteration message sent");
        } catch (InterruptedException | ExecutionException e) {
            throw new CommunicationException("Error while sending iteration ended message", e);
        }
    }

    @Override
    public void waitForStartIterationMessage(int iterationIndex, AIEngineAdapter aiEngineAdapter) throws CommunicationException, AIEngineException {
        boolean received = false;

        Timestamp startTime = Timestamp.from(Instant.now());
        while (!received) {
            final ConsumerRecords<String, byte[]> consumerRecords = this.modelsToClientsConsumer.poll(Duration.ofSeconds(this.pollTimeOut));

            if (consumerRecords.count() > 0) {
                ConsumerRecord<String, byte[]> record = consumerRecords.iterator().next(); // not possible to receive more than one message
                if (record.value() != null && record.value().length != 0) {
                    logger.debug("Start iteration message received");
                    aiEngineAdapter.saveMergedModel(record.value());
                    received = true;
                } else {
                    throw new CommunicationException("Start iteration message without value", null);
                }
            }
            this.modelsToClientsConsumer.commitAsync();

            Timestamp currentTime = Timestamp.from(Instant.now());
            if (currentTime.getTime() > startTime.getTime() + this.startIterationMessageTimeOut) {
                throw new CommunicationException("The manager did not communicate the start of the iteration", null);
            }
        }

        try {
            this.modelsToClientsConsumer.commitSync();
        } catch (CommitFailedException e) {
            throw new CommunicationException("Error while committing", e);
        }
    }

    @Override
    public void cleanEnvironment() {
        this.modelsToClientsConsumer.close();
        this.modelsToManagerProducer.close();
    }

    private void createProducers() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServersConfig);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.jobCompleteId + "_" + this.podId + "_client");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxModelSize + "");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        this.modelsToManagerProducer = new KafkaProducer<>(properties);

        properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServersConfig);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.jobCompleteId + "_" + this.podId + "_client");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.statusProducer = new KafkaProducer<>(properties);
    }

    private void createConsumers() throws CommunicationException {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServersConfig);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.jobCompleteId + "_" + this.podId + "_client");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        this.statusConsumer = new KafkaConsumer<>(properties);
        List<TopicPartition> partitions = new ArrayList<>();
        List<PartitionInfo> partitionInfos = this.statusConsumer.partitionsFor("status");
        if (partitionInfos == null || partitionInfos.size() == 0) throw new CommunicationException("Topic status not already created", null);
        for (PartitionInfo partition : partitionInfos) partitions.add(new TopicPartition(partition.topic(), partition.partition()));
        this.statusConsumer.assign(partitions);  // standalone consumer


        properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServersConfig);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, jobCompleteId + "_" + podId + "_client");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxModelSize + "");
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxIterationTime + "");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        this.modelsToClientsConsumer = new KafkaConsumer<>(properties);
        partitions = new ArrayList<>();
        partitionInfos = modelsToClientsConsumer.partitionsFor(jobCompleteId + "_models_to_clients");
        if (partitionInfos == null || partitionInfos.size() == 0) throw new CommunicationException("Topic " + jobCompleteId + "_models_to_client not already created", null);
        for (PartitionInfo partition : partitionInfos) partitions.add(new TopicPartition(partition.topic(), partition.partition()));
        modelsToClientsConsumer.assign(partitions);  // standalone consumer
    }

}
