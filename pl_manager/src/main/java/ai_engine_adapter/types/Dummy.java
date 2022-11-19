package ai_engine_adapter.types;

import ai_engine_adapter.AIEngineAdapter;
import exceptions.AIEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

public class Dummy extends AIEngineAdapter {

    private static final Logger logger = LogManager.getLogger(DummyTestFiles.class);

    @Override
    public void saveUnMergedModel(String podId, byte[] bytes) {
        logger.debug("saveUnMergedModel method called for pod " + podId + ". Model contents: " + new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public byte[] loadMergedModel() {
        logger.debug("loadMergedModel method called");
        return "Dummy model text".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void cleanDirectories() {
        logger.debug("cleanDirectories method called");
    }

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
