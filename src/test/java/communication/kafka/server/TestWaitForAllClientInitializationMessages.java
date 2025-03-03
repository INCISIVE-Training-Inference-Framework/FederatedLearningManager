package communication.kafka.server;

import ai_engine_adapter.model_management.server.types.Dummy;
import communication_adapter.server.ServerCommunicationAdapter;
import exceptions.CommunicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static communication.kafka.KafkaUtils.*;
import static org.junit.Assert.assertThrows;

@Testcontainers
class TestWaitForAllClientInitializationMessages {

    @Container
    private KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    private final String messageSeparator = "///MESSAGE_SEP///";

    private static final String executionId = "executionId";
    private static Dummy aiEngineAdapterDummy;
    private ServerCommunicationAdapter kafkaCommunicator;

    @BeforeAll
    static void beforeAll() {
        TestWaitForAllClientInitializationMessages.aiEngineAdapterDummy = new Dummy();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        createTopics(KAFKA_CONTAINER, executionId);
        kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
    }

    @Test
    void waitForAllClientInitializationMessagesSuccess() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + messageSeparator + "podId-0", "true", "status"));
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + messageSeparator + "podId-1", "true", "status"));
        kafkaCommunicator.waitForAllClientInitializationMessages();
    }

    @Test
    void waitForAllClientInitializationMessagesNotArrived() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + messageSeparator + "podId-0", "true", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForAllClientInitializationMessages();
        });

        String expectedMessage = "Internal exception: Communication exception: Some client did not communicate the start of its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForAllClientInitializationMessagesIncludingManagerNotArrived() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + messageSeparator + "podId-0", "true", "status"));
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId, "true", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForAllClientInitializationMessages();
        });

        String expectedMessage = "Internal exception: Communication exception: Some client did not communicate the start of its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForAllClientInitializationMessagesIncludingOtherExecutionNotArrived() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + messageSeparator + "podId-0", "true", "status"));
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "other" + messageSeparator + "podId-0", "true", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForAllClientInitializationMessages();
        });

        String expectedMessage = "Internal exception: Communication exception: Some client did not communicate the start of its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForAllClientInitializationMessagesFailure() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + messageSeparator + "podId-0", "false", "status"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForAllClientInitializationMessages();
        });

        String expectedMessage = "Internal exception: Communication exception: The client with id podId-0 did not start well its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }
}