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

    void sendClientInitializationMessage(boolean success) throws CommunicationException;

    void waitForManagerInitializationMessage() throws CommunicationException;

    void sendEndedIterationMessage(int iterationIndex, byte[] model) throws CommunicationException;

    void waitForStartIterationMessage(int iterationIndex, AIEngineAdapter aiEngineAdapter) throws CommunicationException, AIEngineException;

    void cleanEnvironment() throws CommunicationException;

}
