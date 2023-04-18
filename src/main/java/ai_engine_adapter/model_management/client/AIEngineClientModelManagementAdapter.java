package ai_engine_adapter.model_management.client;

import exceptions.AIEngineException;

public interface AIEngineClientModelManagementAdapter {

    void initialize() throws AIEngineException;

    void saveMergedModel(byte[] bytes) throws AIEngineException;

    byte[] loadUnMergedModel() throws AIEngineException;

    byte[] loadEvaluationMetrics() throws AIEngineException;

    void cleanDirectories() throws AIEngineException;

    void clean() throws AIEngineException;

}
