import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import ai_engine_adapter.linkage.types.async_rest_api.AsyncRestAPI;
import ai_engine_adapter.linkage.types.dummy.Dummy;
import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
import communication_adapter.client.ClientCommunicationAdapter;
import communication_adapter.client.types.KafkaClientCommunication;
import communication_adapter.server.ServerCommunicationAdapter;
import communication_adapter.server.types.KafkaServerCommunication;
import exceptions.BadConfigurationException;
import net.sourceforge.argparse4j.inf.Namespace;
import platform_adapter.PlatformAdapter;
import platform_adapter.types.Incisive;

import java.util.Map;

import static config.EnvironmentVariable.loadEnvironmentVariables;

public class Factory {

    public static ClientCommunicationAdapter selectClientCommunicationAdapter(Map<String, Object> initialConfig, Namespace parsedArgs) throws BadConfigurationException {
        String communicationAdapterImplementation = (String) initialConfig.get("COMMUNICATION_ADAPTER");
        switch (communicationAdapterImplementation) {
            case "KAFKA":
                Map<String, Object> config = loadEnvironmentVariables(KafkaClientCommunication.getEnvironmentVariables());
                return new KafkaClientCommunication(config, parsedArgs.get("executionId"), parsedArgs.get("dataPartner"));
            case "DUMMY":
                return new communication_adapter.client.types.Dummy();
            default:
                throw new BadConfigurationException("Communication adapter implementation unknown: " + communicationAdapterImplementation + ". Available: KAFKA");
        }
    }

    public static ServerCommunicationAdapter selectServerCommunicationAdapter(Map<String, Object> initialConfig, Namespace parsedArgs) throws BadConfigurationException {
        String communicationAdapterImplementation = (String) initialConfig.get("COMMUNICATION_ADAPTER");
        switch (communicationAdapterImplementation) {
            case "KAFKA":
                Map<String, Object> config = loadEnvironmentVariables(KafkaServerCommunication.getEnvironmentVariables());
                return new KafkaServerCommunication(config, parsedArgs.get("executionId"), parsedArgs.get("numberOfClients"));
            case "DUMMY":
                return new communication_adapter.server.types.Dummy();
            default:
                throw new BadConfigurationException("Communication adapter implementation unknown: " + communicationAdapterImplementation + ". Available: KAFKA");
        }
    }

    public static AIEngineLinkageAdapter selectAIEngineLinkageAdapter(Map<String, Object> initialConfig, Namespace parsedArgs) throws BadConfigurationException {
        String aiEngineAdapterImplementation = (String) initialConfig.get("AI_ENGINE_LINKAGE_ADAPTER");
        Map<String, Object> config;
        switch (aiEngineAdapterImplementation) {
            case "ASYNC_REST_API":
                config = loadEnvironmentVariables(AsyncRestAPI.getEnvironmentVariables());
                return new AsyncRestAPI(config);
            case "DUMMY":
                return new Dummy();
            default:
                throw new BadConfigurationException("AI engine linkage adapter implementation unknown: " + aiEngineAdapterImplementation + ". Available: KUBERNETES_API and ASYNC_REST_API");
        }
    }

    public static AIEngineClientModelManagementAdapter selectAIEngineClientModelManagementAdapter(Map<String, Object> initialConfig, Namespace parsedArgs) throws BadConfigurationException {
        String aiEngineAdapterImplementation = (String) initialConfig.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER");
        Map<String, Object> config;
        switch (aiEngineAdapterImplementation) {
            case "DEFAULT":
                config = loadEnvironmentVariables(ai_engine_adapter.model_management.client.types.Default.getEnvironmentVariables());
                return new ai_engine_adapter.model_management.client.types.Default(config);
            case "DUMMY":
                return new ai_engine_adapter.model_management.client.types.Dummy();
            default:
                throw new BadConfigurationException("AI engine client model management adapter implementation unknown: " + aiEngineAdapterImplementation + ". Available: DEFAULT");
        }
    }

    public static AIEngineServerModelManagementAdapter selectAIEngineServerModelManagementAdapter(Map<String, Object> initialConfig, Namespace parsedArgs) throws BadConfigurationException {
        String aiEngineAdapterImplementation = (String) initialConfig.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER");
        Map<String, Object> config;
        switch (aiEngineAdapterImplementation) {
            case "DEFAULT":
                config = loadEnvironmentVariables(ai_engine_adapter.model_management.server.types.Default.getEnvironmentVariables());
                return new ai_engine_adapter.model_management.server.types.Default(config);
            case "DUMMY":
                return new ai_engine_adapter.model_management.server.types.Dummy();
            default:
                throw new BadConfigurationException("AI engine server model management adapter implementation unknown: " + aiEngineAdapterImplementation + ". Available: DEFAULT");
        }
    }

    public static PlatformAdapter selectPlatformAdapter(Map<String, Object> initialConfig, Namespace parsedArgs) throws BadConfigurationException {
        String platformAdapterImplementation = (String) initialConfig.get("PLATFORM_ADAPTER");
        Map<String, Object> config;
        switch (platformAdapterImplementation) {
            case "INCISIVE":
                config = loadEnvironmentVariables(Incisive.getEnvironmentVariables());
                return new Incisive(config);
            case "DUMMY":
                return new platform_adapter.types.Dummy();
            default:
                throw new BadConfigurationException("Platform adapter implementation unknown: " + platformAdapterImplementation + ". Available: INCISIVE");
        }
    }


}
