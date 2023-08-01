import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import communication_adapter.client.ClientCommunicationAdapter;
import communication_adapter.server.ServerCommunicationAdapter;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import domain.Client;
import domain.Server;
import exceptions.BadConfigurationException;
import exceptions.BadInputParametersException;
import exceptions.FailureEndSignal;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static config.EnvironmentVariable.loadEnvironmentVariables;

// TODO modify logger level
// TODO do not import all dependencies in production? (change maven command in dockerfile)
// TODO reread https://www.baeldung.com/java-logging-intro async logs and appenders, file logging

public class Application {

    private static final Logger logger = LogManager.getLogger(Application.class);
    public static List<EnvironmentVariable> getInitialEnvironmentVariables() {
        return Arrays.asList(
                new EnvironmentVariable("COMMUNICATION_ADAPTER", EnvironmentVariableType.STRING, "KAFKA"),
                new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER", EnvironmentVariableType.STRING, "ASYNC_REST_API"),
                new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER", EnvironmentVariableType.STRING, "DEFAULT"),
                new EnvironmentVariable("PLATFORM_ADAPTER", EnvironmentVariableType.STRING, "INCISIVE")
        );
    }

    public enum Behaviour {
        CLIENT,
        SERVER
    }

    public static void main(String[] args) {
        try {
            // parse input parameters
            Namespace parsedArgs = parseInputArgs(args);

            // load main environmental variables
            Map<String, Object> initialConfig = loadEnvironmentVariables(Application.getInitialEnvironmentVariables());

            if (parsedArgs.get("behaviour").equals(Behaviour.CLIENT)) {
                // load chosen adapter implementations
                ClientCommunicationAdapter clientCommunicationAdapter = Factory.selectClientCommunicationAdapter(initialConfig, parsedArgs);
                AIEngineLinkageAdapter aiEngineLinkageAdapter = Factory.selectAIEngineLinkageAdapter(initialConfig, parsedArgs);
                AIEngineClientModelManagementAdapter aiEngineClientModelManagementAdapter = Factory.selectAIEngineClientModelManagementAdapter(initialConfig, parsedArgs);
                PlatformAdapter platformAdapter = Factory.selectPlatformAdapter(initialConfig, parsedArgs);

                // run main application
                Client client = new Client(clientCommunicationAdapter, aiEngineLinkageAdapter, aiEngineClientModelManagementAdapter, platformAdapter);
                client.run(parsedArgs.get("numberOfIterations"), parsedArgs.get("useCase"), parsedArgs.get("failure_endpoint"));
            } else {
                // load chosen adapter implementations
                ServerCommunicationAdapter serverCommunicationAdapter = Factory.selectServerCommunicationAdapter(initialConfig, parsedArgs);
                AIEngineLinkageAdapter aiEngineLinkageAdapter = Factory.selectAIEngineLinkageAdapter(initialConfig, parsedArgs);
                AIEngineServerModelManagementAdapter aiEngineServerModelManagementAdapter = Factory.selectAIEngineServerModelManagementAdapter(initialConfig, parsedArgs);
                PlatformAdapter platformAdapter = Factory.selectPlatformAdapter(initialConfig, parsedArgs);

                // run main application
                Server server = new Server(serverCommunicationAdapter, aiEngineLinkageAdapter, aiEngineServerModelManagementAdapter, platformAdapter);
                server.run(parsedArgs.get("numberOfIterations"), parsedArgs.get("useCase"), parsedArgs.get("failure_endpoint"));

            }

        } catch (BadInputParametersException | BadConfigurationException e) {
            logger.error(e.getMessage());
            System.exit(1);
        } catch (FailureEndSignal e) {

            try {
                // extract error message to file
                Path path = Paths.get("error_message.txt");
                String errorMessage = e.getMessage().replaceAll("[\"']", ""); // cleans error message to avoid following parsing errors
                byte[] strToBytes = errorMessage.getBytes();
                Files.write(path, strToBytes);
            } catch (IOException e2) {
                logger.error(e2.getMessage());
                e2.printStackTrace();
            }

            System.exit(1);
        }
    }

    private static Namespace parseInputArgs(String[] args) throws BadInputParametersException {
        ArgumentParser parser = ArgumentParsers.newFor("federated_learning").build();
        Subparsers subparsers = parser.addSubparsers()
                .title("behaviours")
                .description("The application can act as the client or the server of the federated learning");

        Subparser clientParser = subparsers.addParser("client").setDefault("behaviour", Behaviour.CLIENT);
        clientParser.addArgument("executionId").type(String.class).help(
                "The identifier of the execution"
        );
        clientParser.addArgument("dataPartner").type(String.class).help(
                "The corresponding data partner of the client"
        );
        clientParser.addArgument("numberOfIterations").type(Integer.class).help(
                "The number of iterations to do during the execution"
        );
        clientParser.addArgument("useCase").type(String.class).help(
                "The use case to run on every iteration"
        );
        clientParser.addArgument("--failure-endpoint").type(String.class).help(
                "the endpoint to hit when an error occurs"
        );

        Subparser serverParser = subparsers.addParser("server").setDefault("behaviour", Behaviour.SERVER);
        serverParser.addArgument("executionId").type(String.class).help(
                "The identifier of the execution"
        );
        serverParser.addArgument("numberOfIterations").type(Integer.class).help(
                "The number of iterations to do during the execution"
        );
        serverParser.addArgument("numberOfClients").type(Integer.class).help(
                "The number of clients present in the execution"
        );
        serverParser.addArgument("useCase").type(String.class).help(
                "The use case to run on every iteration"
        );
        serverParser.addArgument("--failure-endpoint").type(String.class).help(
                "the endpoint to hit when an error occurs"
        );

        try {
            Namespace parsedArgs = parser.parseArgs(args);
            logger.info(parsedArgs);
            return parsedArgs;
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            throw new BadInputParametersException("Argument parser exception");
        }

    }

}
