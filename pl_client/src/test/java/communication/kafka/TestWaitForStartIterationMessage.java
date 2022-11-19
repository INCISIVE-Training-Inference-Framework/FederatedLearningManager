package communication.kafka;

import ai_engine_adapter.types.Dummy;
import communication_adapter.CommunicationAdapter;
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
class TestWaitForStartIterationMessage {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    private static final String executionId = "executionId";
    private static final String podId = "podId";
    private static Dummy aiEngineAdapterDummy;
    private CommunicationAdapter kafkaCommunicator;

    @BeforeAll
    static void beforeAll() {
        TestWaitForStartIterationMessage.aiEngineAdapterDummy = new Dummy();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        createTopics(KAFKA_CONTAINER, executionId);
        kafkaCommunicator = createKafkaCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
    }

    @Test
    void waitForStartIterationMessageSuccess() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId, "model", executionId + "_models_to_clients"));
        kafkaCommunicator.waitForStartIterationMessage(1, aiEngineAdapterDummy);
    }

    @Test
    void waitForStartIterationMessageNotArrived() throws Exception {
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForStartIterationMessage(1, aiEngineAdapterDummy);
        });

        String expectedMessage = "Internal exception: Communication exception: The manager did not communicate the start of the iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForStartIterationMessageFailure() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId, "", executionId + "_models_to_clients"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForStartIterationMessage(0, aiEngineAdapterDummy);
        });

        String expectedMessage = "Internal exception: Communication exception: Start iteration message without value";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForStartIterationMessageWithoutTopics() throws Exception {
        deleteTopics(KAFKA_CONTAINER, executionId);
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForStartIterationMessage(1, aiEngineAdapterDummy);
        });

        String expectedMessage = "Internal exception: Communication exception: The manager did not communicate the start of the iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }
}