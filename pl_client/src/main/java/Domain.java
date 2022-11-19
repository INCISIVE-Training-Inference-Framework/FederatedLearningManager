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

    public void run(String executionId, int numberOfIterations, String useCase) throws FailureEndSignal {
        logger.info("Started");

        // perform initialization
        try {
            this.communicatorAdapter.initialize();
            this.aiEngineAdapter.initialize();
            this.aiEngineAdapter.waitAIEngineToBeReady();
        } catch (CommunicationException | AIEngineException e1) {
            printException(e1);
            // send initialization message to all clients with failed status
            try {
                logger.debug("Sending initialization message with failed status");
                this.communicatorAdapter.sendClientInitializationMessage(false);
            } catch (CommunicationException e2) {
                printException(e2);
            } finally {
                finishExecution(false);  // throws FailureEndSignal -> ends method execution
            }
        }

        // send initialization message to manager
        try {
            logger.debug("Sending initialization message with success status");
            this.communicatorAdapter.sendClientInitializationMessage(true);
        } catch (CommunicationException e) {
            printException(e);
            finishExecution(false);  // throws FailureEndSignal -> ends method execution
        }

        // check that the manager performed correctly the initialization
        try {
            logger.debug("Waiting for manager initialization message");
            this.communicatorAdapter.waitForManagerInitializationMessage();
        } catch (CommunicationException e) {
            printException(e);
            finishExecution(false);  // throws FailureEndSignal -> ends method execution
        }

        // do iteration logic
        for (int i = 0; i < numberOfIterations; ++i) {
            logger.info("Starting iteration " + i);

            // run AI Engine and wait until is finished
            byte[] model = null;
            try {
                logger.debug("Running AI Engine");
                this.aiEngineAdapter.run(useCase);
                model = this.aiEngineAdapter.loadUnMergedModel();

                logger.debug("Cleaning AI Engine files");
                this.aiEngineAdapter.cleanDirectories();
            } catch (AIEngineException e1) {
                printException(e1);
                // send ended iteration message with failed status
                try {
                    logger.debug("Sending ended iteration message with failed status");
                    this.communicatorAdapter.sendEndedIterationMessage(i, null);
                } catch (CommunicationException e2) {
                    printException(e2);
                } finally {
                    finishExecution(false);  // throws FailureEndSignal -> ends method execution
                }
            }

            // communicate the end of the iteration
            try {
                logger.debug("Sending ended iteration message");
                this.communicatorAdapter.sendEndedIterationMessage(i, model);
            } catch (CommunicationException e) {
                printException(e);
                finishExecution(false);  // throws FailureEndSignal -> ends method execution
            }

            boolean lastIteration = (i == (numberOfIterations - 1));
            if (!lastIteration) {
                // wait for the start of the next iteration
                try {
                    logger.debug("Waiting for the start of the following iteration");
                    this.communicatorAdapter.waitForStartIterationMessage(i + 1, aiEngineAdapter);
                } catch (CommunicationException | AIEngineException e) {
                    printException(e);
                    finishExecution(false);  // throws FailureEndSignal -> ends method execution
                }

                useCase = "training_from_pretrained_model";
            } else {
                // clean environment
                finishExecution(true);
            }

        }
    }

    private void finishExecution(boolean success) throws FailureEndSignal {
        boolean exceptionsThrown = false;

        if (!success) {
            try {
                // communicate end of the execution to the platform
                logger.debug("Communicating execution end to the platform with status: " + success);
                this.platformAdapter.communicateExecutionEnd(success);
            } catch (PlatformException e) {
                printException(e);
                exceptionsThrown = true;
            }
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
