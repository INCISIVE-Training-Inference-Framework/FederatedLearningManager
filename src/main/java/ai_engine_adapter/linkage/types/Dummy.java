package ai_engine_adapter.linkage.types;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import exceptions.AIEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dummy implements AIEngineLinkageAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void initialize() throws AIEngineException {
        logger.debug("initialize method called");
    }

    @Override
    public void waitAIEngineToBeReady() {
        logger.debug("waitAIEngineToBeReady method called");
    }

    @Override
    public void run(String useCase) {
        logger.debug("run method called");
    }

    @Override
    public void clean() throws AIEngineException {
        logger.debug("clean method called");
    }
}
