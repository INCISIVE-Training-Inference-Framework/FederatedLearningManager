package communication_adapter.client.types;

import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import communication_adapter.client.ClientCommunicationAdapter;
import exceptions.AIEngineException;
import exceptions.CommunicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Dummy implements ClientCommunicationAdapter {

    private static final Logger logger = LogManager.getLogger(Dummy.class);

    @Override
    public void initialize() throws CommunicationException {
        logger.debug("initialize method called");
    }

    @Override
    public void sendClientInitializationMessage(boolean success) throws CommunicationException {
        logger.debug("sendClientInitializationMessage method called");
    }

    @Override
    public void waitForManagerInitializationMessage() throws CommunicationException {
        logger.debug("waitForManagerInitializationMessage method called");
    }

    @Override
    public void sendEndedIterationMessage(int iterationIndex, byte[] model) throws CommunicationException {
        logger.debug("sendEndedIterationMessage method called");
    }

    @Override
    public void waitForStartIterationMessage(int iterationIndex, AIEngineClientModelManagementAdapter aiEngineClientModelManagementAdapter) throws CommunicationException, AIEngineException {
        logger.debug("waitForStartIterationMessage method called");
    }

    @Override
    public void cleanEnvironment() {
        logger.debug("cleanEnvironment method called");
    }

}
