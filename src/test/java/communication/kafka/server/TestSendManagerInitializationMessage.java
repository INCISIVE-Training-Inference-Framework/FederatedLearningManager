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
class TestSendManagerInitializationMessage {

    @Container
    private final KafkaContainer KAFKA_CONTAINER = createKafkaContainer();

    @Test
    void SendManagerInitializationMessageSuccess() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);
        ServerCommunicationAdapter kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendManagerInitializationMessage(true);

        String command = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic status " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", command).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + ":true"));
    }

    @Test
    void SendManagerInitializationMessageFailed() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);
        ServerCommunicationAdapter kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        kafkaCommunicator.sendManagerInitializationMessage(false);

        String command = "/usr/bin/kafka-console-consumer " +
                "--bootstrap-server=" + bootstrapLocalhost + " " +
                "--topic status " +
                "--property print.key=true " +
                "--property key.separator=: " +
                "--max-messages 1 " +
                "--timeout-ms 5000 " +
                "--key-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--value-deserializer org.apache.kafka.common.serialization.StringDeserializer " +
                "--from-beginning";
        String stdout = KAFKA_CONTAINER.execInContainer("/bin/sh", "-c", command).getStdout();

        MatcherAssert.assertThat(stdout, containsString(executionId + ":false"));
    }

    @Test
    void SendManagerInitializationMessageWithoutTopics() throws Exception {
        String executionId = "executionId";
        createTopics(KAFKA_CONTAINER, executionId);
        ServerCommunicationAdapter kafkaCommunicator = createKafkaServerCommunicationClass(executionId, KAFKA_CONTAINER.getBootstrapServers());
        kafkaCommunicator.initialize();
        deleteTopics(KAFKA_CONTAINER, executionId);
        Exception exception = assertThrows(CommunicationException.class, () -> kafkaCommunicator.sendManagerInitializationMessage(false));

        String expectedMessage = "Internal exception: Communication exception: Error while sending manager initialization message";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

}