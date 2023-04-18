package domain;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import communication_adapter.server.ServerCommunicationAdapter;
import exceptions.AIEngineException;
import exceptions.CommunicationException;
import exceptions.FailureEndSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private final ServerCommunicationAdapter communicationAdapter;
    private final AIEngineLinkageAdapter aiEngineLinkageAdapter;
    private final AIEngineServerModelManagementAdapter aiEngineModelManagementAdapter;
    private final PlatformAdapter platformAdapter;

    public Server(
            ServerCommunicationAdapter communicationAdapter,
            AIEngineLinkageAdapter aiEngineLinkageAdapter,
            AIEngineServerModelManagementAdapter aiEngineModelManagementAdapter,
            PlatformAdapter platformAdapter
    ) {
        this.communicationAdapter = communicationAdapter;
        this.aiEngineLinkageAdapter = aiEngineLinkageAdapter;
        this.aiEngineModelManagementAdapter = aiEngineModelManagementAdapter;
        this.platformAdapter = platformAdapter;
    }

    public void run(int numberOfIterations, String useCase, String failureEndpoint) throws FailureEndSignal {
        logger.info("Started");

        // initialization

        performInitialization(failureEndpoint);
        sendInitializationMessage(failureEndpoint);
        waitForAllClientInitializationMessages(failureEndpoint);

        // iteration training logic

        int iterationIndex;
        for (iterationIndex = 0; iterationIndex < numberOfIterations; ++iterationIndex) {

            logger.info(String.format("Started iteration %d", iterationIndex));
            waitForAllEndedIterationMessages(iterationIndex, true, failureEndpoint);

            byte[] model = runAIEngine(iterationIndex, useCase, failureEndpoint);
            sendStartIterationMessage(iterationIndex, model, failureEndpoint);

        }

        // evaluation logic

        waitForAllEndedIterationMessages(iterationIndex, false, failureEndpoint);

        // finalization

        logger.info("Finalizing");
        finishExecution(true, null, null);

    }

    private void performInitialization(String failureEndpoint) throws FailureEndSignal {
        try {
            this.communicationAdapter.initialize();
            this.aiEngineLinkageAdapter.initialize();
            this.aiEngineModelManagementAdapter.initialize();
            this.aiEngineLinkageAdapter.waitAIEngineToBeReady();
        } catch (CommunicationException | AIEngineException e1) {
            e1.print(logger);
            try {
                // send initialization message to all clients with failed status
                logger.debug("Sending initialization message with failed status");
                this.communicationAdapter.sendManagerInitializationMessage(false);
            } catch (CommunicationException e2) {
                e2.print(logger);
            } finally {
                finishExecution(false, e1.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
            }
        }
    }

    private void sendInitializationMessage(String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Sending initialization message with success status");
            this.communicationAdapter.sendManagerInitializationMessage(true);
        } catch (CommunicationException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private void waitForAllClientInitializationMessages(String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Waiting for all client initialization messages");
            this.communicationAdapter.waitForAllClientInitializationMessages();
        } catch (CommunicationException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private void waitForAllEndedIterationMessages(int iterationIndex, boolean isModel, String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Waiting for all ended iteration messages");
            this.communicationAdapter.waitForAllEndedIterationMessages(iterationIndex, isModel, this.aiEngineModelManagementAdapter);
        } catch (CommunicationException | AIEngineException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private byte[] runAIEngine(int iterationIndex, String useCase, String failureEndpoint) throws FailureEndSignal {
        byte[] model = null;
        try {
            this.aiEngineModelManagementAdapter.cleanMergedModel();

            logger.debug("Running the AI Engine");
            this.aiEngineLinkageAdapter.run(useCase);
            model = this.aiEngineModelManagementAdapter.loadMergedModel();

            logger.debug("Cleaning AI Engine files");
            this.aiEngineModelManagementAdapter.cleanUnMergedModels();
        } catch (AIEngineException e1) {
            e1.print(logger);
            // send start iteration message to all clients with null model
            try {
                logger.debug("Sending start iteration message with null value");
                this.communicationAdapter.sendStartIterationMessage(iterationIndex, null);
            } catch (CommunicationException e2) {
                e2.print(logger);
            } finally {
                finishExecution(false, e1.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
            }
        }
        return model;
    }

    private void sendStartIterationMessage(int iterationIndex, byte[] model, String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Sending start iteration message");
            this.communicationAdapter.sendStartIterationMessage(iterationIndex, model);
        } catch (CommunicationException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private void finishExecution(boolean success, String failureMessage, String failureEndpoint) throws FailureEndSignal {
        Common.finishExecution(
                success,
                failureMessage,
                failureEndpoint,
                this.platformAdapter,
                this.communicationAdapter,
                this.aiEngineLinkageAdapter,
                logger
        );
    }

}
