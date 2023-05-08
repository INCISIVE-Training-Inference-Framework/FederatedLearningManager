package communication_adapter.server.types;

import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import communication_adapter.server.ServerCommunicationAdapter;
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

public class KafkaServerCommunication implements ServerCommunicationAdapter {

    private static final Logger logger = LogManager.getLogger(KafkaServerCommunication.class);
    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = ServerCommunicationAdapter.getEnvironmentVariables();
        abstractClassVariables.add(new EnvironmentVariable("COMMUNICATION_ADAPTER_BOOTSTRAP_SERVERS_CONFIG", EnvironmentVariableType.STRING));
        abstractClassVariables.add(new EnvironmentVariable("COMMUNICATION_ADAPTER_POLL_TIME_OUT", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("COMMUNICATION_ADAPTER_MAX_INITIALIZATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("COMMUNICATION_ADAPTER_MAX_ITERATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("COMMUNICATION_ADAPTER_MAX_MODEL_SIZE", EnvironmentVariableType.INTEGER)); // KB
        return abstractClassVariables;
    }

    private final String messageSeparator = "///MESSAGE_SEP///";

    private final String executionId;
    private final int numberOfClients;

    private final String bootstrap_servers_config;
    private final long pollTimeOut;
    private final long maxInitializationTime;
    private final long maxIterationTime;
    private final long maxModelSize;

    private Producer<String, String> statusProducer;
    private Producer<String, byte[]> modelsToClientsProducer;
    private Consumer<String, byte[]> modelsToManagerConsumer;
    private Consumer<String, String> statusConsumer;

    public KafkaServerCommunication(Map<String, Object> config, String executionId, int numberOfClients) throws BadConfigurationException {
        this.executionId = executionId;
        this.numberOfClients = numberOfClients;

        this.bootstrap_servers_config = (String) config.get("COMMUNICATION_ADAPTER_BOOTSTRAP_SERVERS_CONFIG");
        this.pollTimeOut = (long) config.get("COMMUNICATION_ADAPTER_POLL_TIME_OUT");
        this.maxInitializationTime = (long) config.get("COMMUNICATION_ADAPTER_MAX_INITIALIZATION_TIME") * 1000;
        this.maxIterationTime = (long) config.get("COMMUNICATION_ADAPTER_MAX_ITERATION_TIME") * 1000;
        this.maxModelSize = (int) config.get("COMMUNICATION_ADAPTER_MAX_MODEL_SIZE") * 1000;
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
                    this.executionId,  // key
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
        while (numberOfReceivedMessages < numberOfClients) {
            ConsumerRecords<String, String> consumerRecords = this.statusConsumer.poll(Duration.ofSeconds(this.pollTimeOut));

            if (consumerRecords.count() > 0) {
                for (ConsumerRecord<String, String> record: consumerRecords) {
                    // key -> executionId (from manager) or executionId_clientId (from client)
                    // value -> true or false
                    boolean fromClient = record.key().contains(messageSeparator);
                    if (fromClient) {
                        boolean fromThisExecution = this.executionId.equals(record.key().split(messageSeparator)[0]);
                        if (fromThisExecution) {
                            String clientId = record.key().split(messageSeparator)[1];
                            boolean success = Boolean.parseBoolean(record.value());
                            if (success) {
                                logger.debug("Started iteration message received from client with id " + clientId);
                                ++numberOfReceivedMessages;
                            } else {
                                throw new CommunicationException("The client with id " + clientId + " did not start well its iteration", null);
                            }
                        }
                    }
                }
            }

            Timestamp currentTime = Timestamp.from(Instant.now());
            if (currentTime.getTime() > startTime.getTime() + this.maxInitializationTime) {
                throw new CommunicationException("Some client did not communicate the start of its iteration", null);
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
    public void waitForAllEndedIterationMessages(int iterationIndex, boolean isModel, AIEngineServerModelManagementAdapter aiEngineServerModelManagementAdapter) throws CommunicationException, AIEngineException {
        int numberOfFinishedClients = 0;

        Timestamp startTime = Timestamp.from(Instant.now());
        while (numberOfFinishedClients < numberOfClients) {
            final ConsumerRecords<String, byte[]> consumerRecords = this.modelsToManagerConsumer.poll(Duration.ofSeconds(this.pollTimeOut));

            if (consumerRecords.count() > 0) {
                for (ConsumerRecord<String, byte[]> record: consumerRecords) {
                    // key -> executionId_clientId
                    // value -> model or null
                    String clientId = record.key().split(messageSeparator)[1];
                    byte[] bytes = record.value();
                    if (bytes != null && bytes.length != 0) {
                        logger.debug("Ended iteration message received from client with id " + clientId);
                        if (isModel) aiEngineServerModelManagementAdapter.saveUnMergedModel(clientId, bytes);
                        else aiEngineServerModelManagementAdapter.saveEvaluationMetrics(clientId, bytes);
                        ++numberOfFinishedClients;
                    } else {
                        throw new CommunicationException("The client with id " + clientId + " did not end well its iteration", null);
                    }
                }
            }

            Timestamp currentTime = Timestamp.from(Instant.now());
            if (currentTime.getTime() > startTime.getTime() + this.maxIterationTime) {
                throw new CommunicationException("Some client did not communicate the end of its iteration", null);
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
                    this.executionId + "_models_to_clients",
                    executionId,  // key
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
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, executionId + "_manager");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.statusProducer = new KafkaProducer<>(properties);

        properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, executionId + "_manager");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, this.maxModelSize + "");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        this.modelsToClientsProducer = new KafkaProducer<>(properties);
    }

    private void createConsumers() throws CommunicationException {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.executionId + "_manager");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, this.maxIterationTime + "");
        properties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, this.maxModelSize + "");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        this.modelsToManagerConsumer = new KafkaConsumer<>(properties);
        List<TopicPartition> partitions = new ArrayList<>();
        List<PartitionInfo> partitionInfos = this.modelsToManagerConsumer.partitionsFor(this.executionId + "_models_to_manager");
        if (partitionInfos == null || partitionInfos.size() == 0) throw new CommunicationException("Topic " + this.executionId + "_models_to_manager not already created", null);
        for (PartitionInfo partition : partitionInfos) partitions.add(new TopicPartition(partition.topic(), partition.partition()));
        this.modelsToManagerConsumer.assign(partitions);  // standalone consumer

        properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrap_servers_config);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.executionId + "_manager");
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
