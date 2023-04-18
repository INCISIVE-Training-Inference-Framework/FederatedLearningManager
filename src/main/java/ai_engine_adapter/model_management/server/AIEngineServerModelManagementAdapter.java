package ai_engine_adapter.model_management.server;

import exceptions.AIEngineException;

public interface AIEngineServerModelManagementAdapter {

    void initialize() throws AIEngineException;

    void saveUnMergedModel(String clientId, byte[] bytes) throws AIEngineException;

    void saveEvaluationMetrics(String clientId, byte[] bytes) throws AIEngineException;

    byte[] loadMergedModel() throws AIEngineException;

    void cleanUnMergedModels() throws AIEngineException;

    void cleanMergedModel() throws AIEngineException;

    void clean() throws AIEngineException;

}
