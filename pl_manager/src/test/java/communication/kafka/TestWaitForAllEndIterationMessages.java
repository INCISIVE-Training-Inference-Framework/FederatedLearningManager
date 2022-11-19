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
class TestWaitForAllEndIterationMessages {

    @Container
    private KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    private static final String executionId = "executionId";
    private static Dummy aiEngineAdapterDummy;
    private CommunicationAdapter kafkaCommunicator;

    @BeforeAll
    static void beforeAll() {
        TestWaitForAllEndIterationMessages.aiEngineAdapterDummy = new Dummy();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        createTopics(KAFKA_CONTAINER, executionId);
        kafkaCommunicator = createKafkaCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
    }

    @Test
    void waitForAllEndIterationsMessagesSuccess() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "_podId-0", "model", executionId + "_models_to_manager"));
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "_podId-1", "model", executionId + "_models_to_manager"));
        kafkaCommunicator.waitForAllEndedIterationMessages(0, aiEngineAdapterDummy);
    }

    @Test
    void waitForAllEndIterationsMessagesNotArrived() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "_podId-0", "model", executionId + "_models_to_manager"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForAllEndedIterationMessages(0, aiEngineAdapterDummy);
        });

        String expectedMessage = "Internal exception: Communication exception: Some POD did not communicate the end of its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void waitForAllEndIterationsMessagesFailure() throws Exception {
        KAFKA_CONTAINER.execInContainer("sh", "-c", generateProducerMessage(executionId + "_podId-0", "", executionId + "_models_to_manager"));
        Exception exception = assertThrows(CommunicationException.class, () -> {
            kafkaCommunicator.waitForAllEndedIterationMessages(0, aiEngineAdapterDummy);
        });

        String expectedMessage = "Internal exception: Communication exception: The POD with id podId-0 did not end well its iteration";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }
}