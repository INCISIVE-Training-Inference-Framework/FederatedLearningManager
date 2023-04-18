package communication.kafka.client;

import communication_adapter.client.ClientCommunicationAdapter;
import exceptions.CommunicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static communication.kafka.KafkaUtils.*;
import static org.junit.Assert.assertThrows;

@Testcontainers
class TestWaitForManagerInitializationMessage {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")).withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    private static final String executionId = "executionId";
    private static final String podId = "podId";
    private ClientCommunicationAdapter kafkaCommunicator;

    @BeforeEach
    void beforeEach() throws Exception {
        createTopics(KAFKA_CONTAINER, executionId);
        kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
    }

    @Test
    void waitForManagerInitializationMessageSuccess() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId, "true", "status"));
        kafkaCommunicator.waitForManagerInitializationMessage();
    }

    @Test
    void waitForManagerInitializationMessageNotArrived() throws Exception {
        Exception exception = assertThrows(CommunicationException.class, () -> kafkaCommunicator.waitForManagerInitializationMessage());

        String expectedMessage = "Internal exception: Communication exception: The manager did not communicate the correct initialization";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForManagerInitializationMessageFromOtherExecutionNotArrived() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "other", "true", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForManagerInitializationMessage();
        });

        String expectedMessage = "Internal exception: Communication exception: The manager did not communicate the correct initialization";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForManagerInitializationMessageFromAClientNotArrived() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "_podId0", "true", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForManagerInitializationMessage();
        });

        String expectedMessage = "Internal exception: Communication exception: The manager did not communicate the correct initialization";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForManagerInitializationMessageFailure() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId, "false", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForManagerInitializationMessage();
        });

        String expectedMessage = "Internal exception: Communication exception: The manager did not start well its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForManagerInitializationMessageWithoutTopics() throws Exception {
        deleteTopics(KAFKA_CONTAINER, executionId);
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForManagerInitializationMessage();
        });

        String expectedMessage = "Internal exception: Communication exception: The manager did not communicate the correct initialization";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }
}
