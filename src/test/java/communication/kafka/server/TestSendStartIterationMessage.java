package communication.kafka.server;

import communication_adapter.server.ServerCommunicationAdapter;
import exceptions.CommunicationException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static communication.kafka.KafkaUtils.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class TestSendStartIterationMessage {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    @Test
    void SendStartIterationMessageSuccess() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);
        ServerCommunicationAdapter kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendStartIterationMessage(0, "model".getBytes());

        String topicCommand = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic " + executionId + "_models_to_clients " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.BytesDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", topicCommand).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + ":model"));
    }

    @Test
    void SendStartIterationMessageFailed() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);
        ServerCommunicationAdapter kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendStartIterationMessage(0, null);

        String topicCommand = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic " + executionId + "_models_to_clients " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.BytesDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", topicCommand).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + ":"));
    }

    @Test
    void SendStartIterationMessageWithoutTopics() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);
        ServerCommunicationAdapter kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        deleteTopics(KAFKA_CONTAINER, executionId);
        Exception exception = assertThrows(CommunicationException.class, () -> kafkaCommunicator.sendStartIterationMessage(0, "model".getBytes()));

        String expectedMessage = "Internal exception: Communication exception: Error while sending iteration start message";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

}