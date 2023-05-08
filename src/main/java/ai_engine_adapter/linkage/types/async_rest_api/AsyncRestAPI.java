package ai_engine_adapter.linkage.types.async_rest_api;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.AIEngineException;
import exceptions.InternalException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AsyncRestAPI implements AIEngineLinkageAdapter {

    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = new ArrayList<>();
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_RETRIES", EnvironmentVariableType.INTEGER));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", EnvironmentVariableType.STRING, "127.0.0.1:8080"));  // ipv4
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", EnvironmentVariableType.STRING, "127.0.0.1:8081"));  // ipv4
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", EnvironmentVariableType.STRING, "/api/ping"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", EnvironmentVariableType.STRING, "/api/run"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_END_URL", EnvironmentVariableType.STRING, "/api/end"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", EnvironmentVariableType.STRING, "/api/callback"));
        return abstractClassVariables;
    }

    private final RunAIEngine runAIEngine;
    private final EndAIEngine endAIEngine;

    public AsyncRestAPI(Map<String, Object> config) {
        long maxIterationTime = (long) config.get("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME");
        long maxInitializationTime = (long) config.get("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME");
        long maxFinalizationTime = (long) config.get("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_TIME");
        int maxFinalizationRetries = (int) config.get("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_RETRIES");
        String clientHost = (String) config.get("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST");
        String serverHost = (String) config.get("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST");
        String pingUrl = (String) config.get("AI_ENGINE_LINKAGE_ADAPTER_PING_URL");
        String runUrl = (String) config.get("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL");
        String endUrl = (String) config.get("AI_ENGINE_LINKAGE_ADAPTER_END_URL");
        String callbackUrl = (String) config.get("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL");

        this.runAIEngine = new RunAIEngine(
                maxIterationTime,
                maxInitializationTime,
                clientHost,
                serverHost,
                pingUrl,
                runUrl,
                callbackUrl
        );

        this.endAIEngine = new EndAIEngine(
                maxFinalizationTime,
                maxFinalizationRetries,
                clientHost,
                pingUrl,
                endUrl
        );
    }

    @Override
    public void initialize() throws AIEngineException {
        try {
            this.runAIEngine.initialize();
        } catch (InternalException e) {
            throw new AIEngineException(e.getMessage(), e.getException());
        }
    }

    @Override
    public void waitAIEngineToBeReady() throws AIEngineException {
        try {
            this.runAIEngine.waitAIEngineToBeReady();
        } catch (InternalException e) {
            throw new AIEngineException(e.getMessage(), e.getException());
        }
    }

    @Override
    public void run(String useCase) throws AIEngineException {
        try {
            this.runAIEngine.run(useCase);
        } catch (InternalException e) {
            throw new AIEngineException(e.getMessage(), e.getException());
        }
    }

    @Override
    public void end() throws AIEngineException {
        try {
            this.endAIEngine.end();
        } catch (InternalException e) {
            throw new AIEngineException(e.getMessage(), e.getException());
        }
    }

    @Override
    public void clean() throws AIEngineException {
        try {
            this.runAIEngine.clean();
        } catch (InternalException e) {
            throw new AIEngineException(e.getMessage(), e.getException());
        }
    }

}
