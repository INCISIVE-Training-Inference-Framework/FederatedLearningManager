package ai_engine_adapter.model_management.server.types;

import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import exceptions.AIEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

public class Dummy implements AIEngineServerModelManagementAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void initialize() throws AIEngineException {
        logger.debug("initialize method called");
    }

    @Override
    public void saveUnMergedModel(String clientId, byte[] bytes) throws AIEngineException {
        logger.debug("saveUnMergedModel method called");
    }

    @Override
    public void saveEvaluationMetrics(String clientId, byte[] bytes) throws AIEngineException {
        logger.debug("saveEvaluationMetrics method called");
    }

    @Override
    public byte[] loadMergedModel() throws AIEngineException {
        logger.debug("loadMergedModel method called");
        return "empty".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void cleanUnMergedModels() throws AIEngineException {
        logger.debug("cleanUnMergedModel method called");
    }

    @Override
    public void cleanMergedModel() throws AIEngineException {
        logger.debug("cleanMergedModel method called");
    }

    @Override
    public void clean() throws AIEngineException {
        logger.debug("clean method called");
    }

}
