package communication_adapter.client;

import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import communication_adapter.CommunicationAdapter;
import config.EnvironmentVariable;
import exceptions.AIEngineException;
import exceptions.CommunicationException;

import java.util.ArrayList;
import java.util.List;

public interface ClientCommunicationAdapter extends CommunicationAdapter {

    static List<EnvironmentVariable> getEnvironmentVariables() {
        return new ArrayList<>();
    }

    void initialize() throws CommunicationException;

    void sendClientInitializationMessage(boolean success) throws CommunicationException;

    void waitForManagerInitializationMessage() throws CommunicationException;

    void sendEndedIterationMessage(int iterationIndex, byte[] model) throws CommunicationException;

    void waitForStartIterationMessage(int iterationIndex, AIEngineClientModelManagementAdapter aiEngineClientModelManagementAdapter) throws CommunicationException, AIEngineException;

}
