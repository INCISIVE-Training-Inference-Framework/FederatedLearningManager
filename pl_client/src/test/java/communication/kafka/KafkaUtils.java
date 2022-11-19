package communication.kafka;

import communication_adapter.CommunicationAdapter;
import communication_adapter.types.KafkaCommunication;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class KafkaUtils {

    public static final String bootstrapLocalhost = "localhost:9092";
    public static final String zookeeperLocalhost = "localhost:2181";

    public static String generateProducerMessage(String key, String value, String topic) {
        return "echo " + key + ":" + value + " | " +
                "/usr/bin/kafka-console-producer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic " + topic + " " +
                "--property parse.key=true " +
                "--property key.separator=:";
    }

    public static String generateCreateTopicMessage(String topic) {
        return "/usr/bin/kafka-topics " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--create " +
                "--topic " + topic;
    }

    public static String generateListTopicsMessage() {
        return "/usr/bin/kafka-topics " +
                "--zookeeper " + zookeeperLocalhost + " " +
                "--list";
    }

    public static String generateDeleteTopicMessage(String topic) {
        return "/usr/bin/kafka-topics " +
                "--zookeeper " + zookeeperLocalhost + " " +
                "--delete " +
                "--topic " + topic;
    }

    public static void createTopics(KafkaContainer KAFKA_CONTAINER, String executionId) throws Exception {
        KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", generateCreateTopicMessage("status"));
        KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", generateCreateTopicMessage(executionId + "_models_to_manager"));
        KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", generateCreateTopicMessage(executionId + "_models_to_clients"));
    }

    public static void deleteTopics(KafkaContainer KAFKA_CONTAINER, String executionId) throws Exception {
        KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", generateDeleteTopicMessage("status"));
        KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", generateDeleteTopicMessage(executionId + "_models_to_manager"));
        KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", generateDeleteTopicMessage(executionId + "_models_to_clients"));
    }

    public static KafkaContainer createKafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
                .withEnv("KAFKA_CLEANUP_POLICY", "compact,delete");
    }

    public static CommunicationAdapter createKafkaCommunicationClass(String executionId, String podId, String bootstrapServers) throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("KAFKA_BOOTSTRAP_SERVERS_CONFIG", bootstrapServers);
        config.put("KAFKA_POLL_TIME_OUT", 2L);
        config.put("KAFKA_INITIALIZATION_MESSAGE_TIME_OUT", 7L);
        config.put("KAFKA_START_ITERATION_MESSAGE_TIME_OUT", 7L);
        config.put("KAFKA_MAX_ITERATION_TIME", 7L);
        config.put("KAFKA_MAX_MODEL_SIZE", 1);
        return new KafkaCommunication(config, executionId, podId);
    }

}
