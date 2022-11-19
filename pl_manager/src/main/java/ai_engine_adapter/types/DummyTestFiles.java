package ai_engine_adapter.types;

import ai_engine_adapter.AIEngineAdapter;
import exceptions.AIEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DummyTestFiles extends AIEngineAdapter {

    private static final Logger logger = LogManager.getLogger(DummyTestFiles.class);

    @Override
    public void waitAIEngineToBeReady() {
        logger.debug("waitAIEngineToBeReady method called");
    }

    @Override
    public void mergeModels() {
        logger.debug("mergeModels method called");
    }

    @Override
    public void endExecution() throws AIEngineException {
        logger.debug("endExecution method called");
    }
}
