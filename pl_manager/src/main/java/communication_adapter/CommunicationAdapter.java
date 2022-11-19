package communication_adapter;

import ai_engine_adapter.AIEngineAdapter;
import config.EnvironmentVariable;
import exceptions.AIEngineException;
import exceptions.CommunicationException;

import java.util.ArrayList;
import java.util.List;

public interface CommunicationAdapter {

    static List<EnvironmentVariable> getEnvironmentVariables() {
        return new ArrayList<>();
    }

    void initialize() throws CommunicationException;

    void sendManagerInitializationMessage(boolean success) throws CommunicationException;

    void waitForAllClientInitializationMessages() throws CommunicationException;

    void waitForAllEndedIterationMessages(int iterationIndex, AIEngineAdapter aiEngineAdapter) throws CommunicationException, AIEngineException;

    void sendStartIterationMessage(int iterationIndex, byte[] model) throws CommunicationException;

    void cleanEnvironment() throws CommunicationException;

}
