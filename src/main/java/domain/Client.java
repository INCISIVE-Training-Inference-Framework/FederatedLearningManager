package domain;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import communication_adapter.client.ClientCommunicationAdapter;
import exceptions.AIEngineException;
import exceptions.CommunicationException;
import exceptions.FailureEndSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

public class Client {

    private static final Logger logger = LogManager.getLogger(Client.class);

    private final ClientCommunicationAdapter communicationAdapter;
    private final AIEngineLinkageAdapter aiEngineLinkageAdapter;
    private final AIEngineClientModelManagementAdapter aiEngineModelManagementAdapter;
    private final PlatformAdapter platformAdapter;

    public Client(
            ClientCommunicationAdapter communicationAdapter,
            AIEngineLinkageAdapter aiEngineLinkageAdapter,
            AIEngineClientModelManagementAdapter aiEngineModelManagementAdapter,
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
        sendInitializationMessageToManager(failureEndpoint);
        waitForManagerToInitialize(failureEndpoint);

        // iteration training logic

        int iterationIndex;
        for (iterationIndex = 0; iterationIndex < numberOfIterations; ++iterationIndex) {

            logger.info(String.format("Started iteration %d", iterationIndex));
            byte[] model = runAIEngine(iterationIndex, useCase, true, failureEndpoint);
            useCase = "training_from_pretrained_model";

            communicateIterationEnd(iterationIndex, model, failureEndpoint);
            waitForNextIterationStart(iterationIndex, failureEndpoint);

        }

        // evaluation logic

        logger.info("Started evaluation");
        useCase = "evaluating_from_pretrained_model";
        byte[] evaluationMetrics = runAIEngine(iterationIndex, useCase, false, failureEndpoint);
        communicateIterationEnd(iterationIndex, evaluationMetrics, failureEndpoint);

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
            // send initialization message to all clients with failed status
            try {
                logger.debug("Sending initialization message with failed status");
                this.communicationAdapter.sendClientInitializationMessage(false);
            } catch (CommunicationException e2) {
                e2.print(logger);
            } finally {
                finishExecution(false, e1.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
            }
        }
    }

    private void sendInitializationMessageToManager(String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Sending initialization message with success status");
            this.communicationAdapter.sendClientInitializationMessage(true);
        } catch (CommunicationException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private void waitForManagerToInitialize(String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Waiting for manager initialization message");
            this.communicationAdapter.waitForManagerInitializationMessage();
        } catch (CommunicationException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private byte[] runAIEngine(int iterationIndex, String useCase, boolean isModel, String failureEndpoint) throws FailureEndSignal {
        byte[] bytes = null;
        try {
            logger.debug("Running AI Engine");
            this.aiEngineLinkageAdapter.run(useCase);
            if (isModel) bytes = this.aiEngineModelManagementAdapter.loadUnMergedModel();
            else bytes = this.aiEngineModelManagementAdapter.loadEvaluationMetrics();

            logger.debug("Cleaning AI Engine files");
            this.aiEngineModelManagementAdapter.cleanDirectories();
        } catch (AIEngineException e1) {
            e1.print(logger);
            // send ended iteration message with failed status
            try {
                logger.debug("Sending ended iteration message with failed status");
                this.communicationAdapter.sendEndedIterationMessage(iterationIndex, null);
            } catch (CommunicationException e2) {
                e2.print(logger);
            } finally {
                finishExecution(false, e1.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
            }
        }
        return bytes;
    }

    private void communicateIterationEnd(int iterationIndex, byte[] model, String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Sending ended iteration message");
            this.communicationAdapter.sendEndedIterationMessage(iterationIndex, model);
        } catch (CommunicationException e) {
            e.print(logger);
            finishExecution(false, e.getMessage(), failureEndpoint);  // throws FailureEndSignal -> ends method execution
        }
    }

    private void waitForNextIterationStart(int iterationIndex, String failureEndpoint) throws FailureEndSignal {
        try {
            logger.debug("Waiting for the start of the following iteration");
            this.communicationAdapter.waitForStartIterationMessage(iterationIndex + 1, this.aiEngineModelManagementAdapter);
        } catch (CommunicationException | AIEngineException e) {
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
