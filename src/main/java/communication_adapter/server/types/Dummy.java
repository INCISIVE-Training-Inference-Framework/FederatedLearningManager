package communication_adapter.server.types;

import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import communication_adapter.server.ServerCommunicationAdapter;
import exceptions.AIEngineException;
import exceptions.CommunicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dummy implements ServerCommunicationAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void initialize() throws CommunicationException {
        logger.debug("initialize method called");
    }

    @Override
    public void sendManagerInitializationMessage(boolean success) throws CommunicationException {
        logger.debug("sendManagerInitializationMessage method called");
    }

    @Override
    public void waitForAllClientInitializationMessages() throws CommunicationException {
        logger.debug("waitForAllClientInitializationMessages method called");
    }

    @Override
    public void waitForAllEndedIterationMessages(int iterationIndex, boolean isModel, AIEngineServerModelManagementAdapter aiEngineServerModelManagementAdapter) throws CommunicationException, AIEngineException {
        logger.debug("waitForAllEndedIterationMessages method called");
    }

    @Override
    public void sendStartIterationMessage(int iterationIndex, byte[] model) throws CommunicationException {
        logger.debug("sendStartIterationMessage method called");
    }

    @Override
    public void cleanEnvironment() {
        logger.debug("cleanEnvironment method called");
    }

}
