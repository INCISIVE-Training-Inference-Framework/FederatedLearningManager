package ai_engine_adapter.types;

import ai_engine_adapter.AIEngineAdapter;
import exceptions.AIEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

public class Dummy extends AIEngineAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void saveMergedModel(byte[] bytes) {
        logger.debug("saveMergedModel method called. Model contents: " + new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public byte[] loadUnMergedModel() {
        logger.debug("loadUnMergedModel method called");
        return "Dummy model text".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void waitAIEngineToBeReady() {
        logger.debug("waitAIEngineToBeReady method called");
    }

    @Override
    public void cleanDirectories() {
        logger.debug("cleanDirectories method called");
    }

    @Override
    public void run(String useCase) {
        logger.debug("run method called");
    }

    @Override
    public void endExecution() throws AIEngineException {
        logger.debug("endExecution method called");
    }
}
