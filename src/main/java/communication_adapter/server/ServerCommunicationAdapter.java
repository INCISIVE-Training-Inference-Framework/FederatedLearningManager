package communication_adapter.server;

import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import communication_adapter.CommunicationAdapter;
import config.EnvironmentVariable;
import exceptions.AIEngineException;
import exceptions.CommunicationException;

import java.util.ArrayList;
import java.util.List;

public interface ServerCommunicationAdapter extends CommunicationAdapter {

    static List<EnvironmentVariable> getEnvironmentVariables() {
        return new ArrayList<>();
    }

    void initialize() throws CommunicationException;

    void sendManagerInitializationMessage(boolean success) throws CommunicationException;

    void waitForAllClientInitializationMessages() throws CommunicationException;

    void waitForAllEndedIterationMessages(int iterationIndex, boolean isModel, AIEngineServerModelManagementAdapter aiEngineServerModelManagementAdapter) throws CommunicationException, AIEngineException;

    void sendStartIterationMessage(int iterationIndex, byte[] model) throws CommunicationException;

}
