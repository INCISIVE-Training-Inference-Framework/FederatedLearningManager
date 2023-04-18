import config.EnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static communication.kafka.KafkaUtils.createKafkaContainer;
import static communication.kafka.KafkaUtils.createTopics;

@Testcontainers
class TestFullProcess {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    @Test
    void fullProcessSuccess() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);

        Map<String, String> envs = new HashMap<>();
        envs.put("COMMUNICATION_ADAPTER", "KAFKA");
        envs.put("AI_ENGINE_LINKAGE_ADAPTER", "DUMMY");
        envs.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER", "DUMMY");
        envs.put("PLATFORM_ADAPTER", "DUMMY");
        envs.put("COMMUNICATION_ADAPTER_BOOTSTRAP_SERVERS_CONFIG", KAFKA_CONTAINER.getBootstrapServers());
        envs.put("COMMUNICATION_ADAPTER_POLL_TIME_OUT", "2");
        envs.put("COMMUNICATION_ADAPTER_MAX_INITIALIZATION_TIME", "7");
        envs.put("COMMUNICATION_ADAPTER_INITIALIZATION_MESSAGE_TIME_OUT", "7");
        envs.put("COMMUNICATION_ADAPTER_START_ITERATION_MESSAGE_TIME_OUT", "7");
        envs.put("COMMUNICATION_ADAPTER_MAX_ITERATION_TIME", "7");
        envs.put("COMMUNICATION_ADAPTER_MAX_MODEL_SIZE", "1");
        EnvironmentVariable.setDebugEnvironment(envs);

        // client data-partner-1
        Thread thread1 = new Thread(() -> {
            try {
                //setEnv(envs);
                String[] args = {"client", executionId, "data-partner-1", "3", "training_from_scratch"};
                Application.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread1.start();

        // client data-partner-2
        Thread thread2 = new Thread(() -> {
            try {
                //setEnv(envs);
                String[] args = {"client", executionId, "data-partner-2", "3", "training_from_scratch"};
                Application.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread2.start();

        String[] args = {"server", executionId, "3", "2", "merging_models"};
        Application.main(args);
    }

}
