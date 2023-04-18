package communication.kafka.client;

import communication_adapter.client.ClientCommunicationAdapter;
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
class TestSendClientInitializationMessage {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    @Test
    void sendClientInitializationMessageSuccess() throws Exception {
        String executionId = "executionId";
        String podId = "podId";
        createTopics(KAFKA_CONTAINER, executionId);
        ClientCommunicationAdapter kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendClientInitializationMessage(true);

        String topicCommand = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic status " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", topicCommand).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + "_" + podId + ":true"));
    }

    @Test
    void sendClientInitializationMessageFailure() throws Exception {
        String executionId = "executionId";
        String podId = "podId";
        createTopics(KAFKA_CONTAINER, executionId);
        ClientCommunicationAdapter kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendClientInitializationMessage(false);

        String topicCommand = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic status " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", topicCommand).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + "_" + podId + ":false"));
    }

    @Test
    void sendClientInitializationMessageWithoutTopics() throws Exception {
        String executionId = "executionId";
        String podId = "podId";
        createTopics(KAFKA_CONTAINER, executionId);
        ClientCommunicationAdapter kafkaCommunicator = createKafkaClientCommunicationClass(executionId, podId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        deleteTopics(KAFKA_CONTAINER, executionId);

        Exception exception = assertThrows(CommunicationException.class, () -> kafkaCommunicator.sendClientInitializationMessage(true));

        String expectedMessage = "Internal exception: Communication exception: Error while sending client initialization message";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

}