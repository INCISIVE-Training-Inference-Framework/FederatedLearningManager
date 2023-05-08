package ai_engine_adapter.linkage;

import exceptions.AIEngineException;

public interface AIEngineLinkageAdapter {

    void initialize() throws AIEngineException;

    void waitAIEngineToBeReady() throws AIEngineException;

    void run(String useCase) throws AIEngineException;

    void end() throws AIEngineException;

    void clean() throws AIEngineException;

}
