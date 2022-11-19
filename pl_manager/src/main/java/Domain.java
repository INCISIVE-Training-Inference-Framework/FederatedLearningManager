import ai_engine_adapter.AIEngineAdapter;
import communication_adapter.CommunicationAdapter;
import exceptions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import platform_adapter.PlatformAdapter;

public class Domain {

    private static final Logger logger = LogManager.getLogger(Domain.class);

    private final CommunicationAdapter communicatorAdapter;
    private final AIEngineAdapter aiEngineAdapter;
    private final PlatformAdapter platformAdapter;

    public Domain(
            CommunicationAdapter communicatorAdapter,
            AIEngineAdapter aiEngineAdapter,
            PlatformAdapter platformAdapter
    ) {
        this.communicatorAdapter = communicatorAdapter;
        this.aiEngineAdapter = aiEngineAdapter;
        this.platformAdapter = platformAdapter;
    }

    public void run(int numberOfIterations) throws FailureEndSignal {
        logger.info("Started");

        // perform initialization
        try {
            this.communicatorAdapter.initialize();
            this.aiEngineAdapter.initialize();
            this.aiEngineAdapter.waitAIEngineToBeReady();
        } catch (CommunicationException | AIEngineException e1) {
            printException(e1);
            try {
                // send initialization message to all clients with failed status
                logger.debug("Sending initialization message with failed status");
                this.communicatorAdapter.sendManagerInitializationMessage(false);
            } catch (CommunicationException e2) {
                printException(e2);
            } finally {
                finishExecution(false);  // throws FailureEndSignal -> ends method execution
            }
        }

        // send initialization message to all clients with success status
        try {
            logger.debug("Sending initialization message with success status");
            this.communicatorAdapter.sendManagerInitializationMessage(true);
        } catch (CommunicationException e) {
            printException(e);
            finishExecution(false);  // throws FailureEndSignal -> ends method execution
        }

        // wait for all client initialization messages
        try {
            logger.debug("Waiting for all client initialization messages");
            this.communicatorAdapter.waitForAllClientInitializationMessages();
        } catch (CommunicationException e) {
            printException(e);
            finishExecution(false);  // throws FailureEndSignal -> ends method execution
        }

        // do iteration logic
        for (int i = 0; i < numberOfIterations; ++i) {
            logger.info("Starting iteration " + i);

            // wait for all clients to end the iteration and save their models
            try {
                logger.debug("Waiting for all ended iteration messages");
                this.communicatorAdapter.waitForAllEndedIterationMessages(i, aiEngineAdapter);
            } catch (CommunicationException | AIEngineException e) {
                printException(e);
                finishExecution(false);  // throws FailureEndSignal -> ends method execution
            }

            boolean lastIteration = (i == (numberOfIterations - 1));
            if (!lastIteration) {
                byte[] model = null;

                // call AI Engine API to merge the models and wait until is finished
                try {
                    logger.debug("Running the AI Engine");
                    this.aiEngineAdapter.mergeModels();
                    model = this.aiEngineAdapter.loadMergedModel();

                    logger.debug("Cleaning AI Engine files");
                    this.aiEngineAdapter.cleanDirectories();
                } catch (AIEngineException e1) {
                    printException(e1);
                    // send start iteration message to all clients with null model
                    try {
                        logger.debug("Sending start iteration message with null value");
                        this.communicatorAdapter.sendStartIterationMessage(i, null);
                    } catch (CommunicationException e2) {
                        printException(e2);
                    } finally {
                        finishExecution(false);  // throws FailureEndSignal -> ends method execution
                    }
                }

                // send start iteration message
                try {
                    logger.debug("Sending start iteration message");
                    this.communicatorAdapter.sendStartIterationMessage(i, model);
                } catch (CommunicationException e) {
                    printException(e);
                    finishExecution(false);  // throws FailureEndSignal -> ends method execution
                }

            } else {
                byte[] model = null;

                // call AI Engine API to merge the models and wait until is finished
                try {
                    logger.debug("Running the AI Engine");
                    this.aiEngineAdapter.mergeModels();
                    model = this.aiEngineAdapter.loadMergedModel();
                } catch (AIEngineException e) {
                    printException(e);
                    finishExecution(false);  // throws FailureEndSignal -> ends method execution
                }

                // upload final model to platform
                try {
                    logger.debug("Uploading model to the platform");
                    this.platformAdapter.uploadModel(model);
                } catch (PlatformException e) {
                    printException(e);
                    finishExecution(false);  // throws FailureEndSignal -> ends method execution
                }

                // end execution successfully
                logger.debug("Ending execution successfully");
                finishExecution(true);
            }
        }
    }

    private void finishExecution(boolean success) throws FailureEndSignal {
        boolean exceptionsThrown = false;
        try {
            // communicate end of the execution to the platform
            logger.debug("Communicating execution end to the platform with status: " + success);
            this.platformAdapter.communicateExecutionEnd(success);
        } catch (PlatformException e) {
            printException(e);
            exceptionsThrown = true;
        }

        try {
            // clean communication adapter environment
            logger.debug("Cleaning environment");
            this.communicatorAdapter.cleanEnvironment();
        } catch (CommunicationException e) {
            printException(e);
            exceptionsThrown = true;
        }

        try {
            // end AI Engine execution
            logger.debug("Ending AI Engine execution");
            this.aiEngineAdapter.endExecution();
        } catch (AIEngineException e) {
            printException(e);
            exceptionsThrown = true;
        }

        if (exceptionsThrown || !success) throw new FailureEndSignal();
    }

    private void printException(InternalException e) {
        logger.error(e.getMessage());
        if (e.getException() != null) {
            logger.error(e.getException().getMessage());
            e.getException().printStackTrace();
        }
    }
}
