package ai_engine_adapter.model_management.client.types;

import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import exceptions.AIEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

public class Dummy implements AIEngineClientModelManagementAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void initialize() throws AIEngineException {
        logger.debug("initialize method called");
    }

    @Override
    public void saveMergedModel(byte[] bytes) throws AIEngineException {
        logger.debug("saveMergedModel method called");
    }

    @Override
    public byte[] loadUnMergedModel() throws AIEngineException {
        logger.debug("loadUnMergedModel method called");
        return "empty".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] loadEvaluationMetrics() throws AIEngineException {
        logger.debug("loadEvaluationMetrics method called");
        return "empty".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void cleanDirectories() throws AIEngineException {
        logger.debug("cleanDirectories method called");
    }

    @Override
    public void clean() throws AIEngineException {
        logger.debug("clean method called");
    }

}
