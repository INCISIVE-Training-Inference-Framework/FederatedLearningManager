package communication.kafka.client;

import communication_adapter.client.ClientCommunicationAdapter;
import exceptions.CommunicationException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static communication.kafka.KafkaUtils.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class TestSendEndedIterationMessage {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    @Test
    void sendEndedIterationMessageSuccess() throws Exception {
        String executionId = "executionId";
        String podId = "podId";
        createTopics(KAFKA_CONTAINER, executionId);
        ClientCommunicationAdapter kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendEndedIterationMessage(0, "model".getBytes(StandardCharsets.UTF_8));

        String topicCommand = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic " + executionId + "_models_to_manager " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.BytesDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", topicCommand).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + "_" + podId + ":model"));
    }

    @Test
    void sendEndedIterationMessageFailed() throws Exception {
        String executionId = "executionId";
        String podId = "podId";
        createTopics(KAFKA_CONTAINER, executionId);
        ClientCommunicationAdapter kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendEndedIterationMessage(0, null);

        String topicCommand = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic " + executionId + "_models_to_manager " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.BytesDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", topicCommand).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + "_" + podId + ":"));
    }

    @Test
    void sendEndedIterationMessageWithoutTopics() throws Exception {
        String executionId = "executionId";
        String podId = "podId";
        createTopics(KAFKA_CONTAINER, executionId);
        ClientCommunicationAdapter kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        deleteTopics(KAFKA_CONTAINER, executionId);

        Exception exception = assertThrows(CommunicationException.class, () -> kafkaCommunicator.sendEndedIterationMessage(0, "model".getBytes(StandardCharsets.UTF_8)));

        String expectedMessage = "Internal exception: Communication exception: Error while sending iteration ended message";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

}