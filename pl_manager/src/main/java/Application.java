import ai_engine_adapter.AIEngineAdapter;
import ai_engine_adapter.types.Dummy;
import ai_engine_adapter.types.KubernetesApi;
import communication_adapter.CommunicationAdapter;
import communication_adapter.types.KafkaCommunication;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.BadConfigurationException;
import exceptions.BadInputParametersException;
import exceptions.FailureEndSignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import platform_adapter.PlatformAdapter;
import platform_adapter.types.Incisive;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO modify logger level
// TODO do not import all dependencies in production?
// TODO reread https://www.baeldung.com/java-logging-intro async logs and appenders, file logging
// TODO failures work arounds

public class Application {

    private static final Logger logger = LogManager.getLogger(Application.class);
    public static List<EnvironmentVariable> getInitialEnvironmentVariables() {
        return Arrays.asList(
                new EnvironmentVariable("COMMUNICATION_ADAPTER", EnvironmentVariableType.STRING, "KAFKA"),
                new EnvironmentVariable("AI_ENGINE_ADAPTER", EnvironmentVariableType.STRING, "KUBERNETES_API"),
                new EnvironmentVariable("PLATFORM_ADAPTER", EnvironmentVariableType.STRING, "INCISIVE")
        );
    }

    public record InputParameters(
            String jobId,
            String jobCompleteId,
            String jobName,
            int numberOfIterations,
            int numberOfPods,
            JSONObject modelMetadata
    ) {

        public String getJobId() {
            return jobId;
        }

        public String getJobCompleteId() {
            return jobCompleteId;
        }

        public String getJobName() {
            return jobName;
        }

        public int getNumberOfIterations() {
            return numberOfIterations;
        }

        public int getNumberOfPods() {
            return numberOfPods;
        }

        public JSONObject getModelMetadata() {
            return modelMetadata;
        }

        @Override
        public String toString() {
            return "InputParameters{" +
                    "jobId='" + jobId + '\'' +
                    ", jobCompleteId='" + jobCompleteId + '\'' +
                    ", jobName='" + jobName + '\'' +
                    ", numberOfIterations=" + numberOfIterations +
                    ", numberOfPods=" + numberOfPods +
                    ", modelMetadata=" + modelMetadata +
                    '}';
        }
    }

    public static void main(String[] args) {
        try {
            // parse input parameters
            InputParameters inputParameters = parseInputParameters(args);
            logger.debug(inputParameters);

            // load chosen adapter implementations
            Map<String, Object> initialConfig = loadEnvironmentVariables(Application.getInitialEnvironmentVariables());
            CommunicationAdapter communicationAdapter = selectCommunicationAdapter(initialConfig, inputParameters);
            AIEngineAdapter aiEngineAdapter = selectAIEngineAdapter(initialConfig, inputParameters);
            PlatformAdapter platformAdapter = selectPlatformAdapter(initialConfig, inputParameters);

            // run main application
            Domain domain = new Domain(communicationAdapter, aiEngineAdapter, platformAdapter);
            domain.run(inputParameters.getNumberOfIterations());
        } catch (BadInputParametersException | BadConfigurationException e) {
            logger.error(e);
            System.exit(1);
        } catch (FailureEndSignal e) {
            System.exit(1);
        }
    }

    private static InputParameters parseInputParameters(String[] args) throws BadInputParametersException {
        if (args.length != 6) throw new BadInputParametersException(
                // TODO refactor input parameters
                "There should be six input parameters -> " +
                        "job_id(String), " +  // global for all pods
                        "job_complete_id(String), " +  // global for all pods
                        "job_name(String), " +  // specific for the manager
                        "number_of_iterations(int), " +
                        "number_of_pods(int) " +
                        "and model_metadata(JSON)");
        String jobId = args[0];
        String jobCompleteId = args[1];
        String jobName = args[2];
        int numberOfIterations;
        try {
            numberOfIterations = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException ex){
            throw new BadInputParametersException("The number of iterations must be an integer number");
        }
        int numberOfPods;
        try {
            numberOfPods = Integer.parseInt(args[4]);
        }
        catch (NumberFormatException ex){
            throw new BadInputParametersException("The number of pods must be an integer number");
        }
        JSONObject modelMetadata;
        try {
            modelMetadata = new JSONObject(args[5]);
        } catch (JSONException e) {
            throw new BadInputParametersException("The model metadata is not a valid JSON: " + e.getMessage());
        }
        return new InputParameters(jobId, jobCompleteId, jobName, numberOfIterations, numberOfPods, modelMetadata);
    }

    private static Map<String, Object> loadEnvironmentVariables(List<EnvironmentVariable> environmentVariables) throws BadConfigurationException {
        Map<String, Object> config = new HashMap<>();
        for (EnvironmentVariable environmentVariable: environmentVariables) {
            config.put(environmentVariable.name, environmentVariable.load());
        }
        return config;
    }

    private static CommunicationAdapter selectCommunicationAdapter(Map<String, Object> initialConfig, InputParameters inputParameters) throws BadConfigurationException {
        String communicationAdapterImplementation = (String) initialConfig.get("COMMUNICATION_ADAPTER");
        switch (communicationAdapterImplementation) {
            case "KAFKA":
                Map<String, Object> config = loadEnvironmentVariables(KafkaCommunication.getEnvironmentVariables());
                return new KafkaCommunication(config, inputParameters.getJobCompleteId(), inputParameters.getNumberOfPods());
            default:
                throw new BadConfigurationException("Communication adapter implementation unknown: " + communicationAdapterImplementation + ". Available: KAFKA");
        }
    }

    private static AIEngineAdapter selectAIEngineAdapter(Map<String, Object> initialConfig, InputParameters inputParameters) throws BadConfigurationException {
        String aiEngineAdapterImplementation = (String) initialConfig.get("AI_ENGINE_ADAPTER");
        Map<String, Object> config;
        switch (aiEngineAdapterImplementation) {
            case "KUBERNETES_API":
                config = loadEnvironmentVariables(KubernetesApi.getEnvironmentVariables());
                return new KubernetesApi(config, inputParameters.getJobName());
            case "DUMMY":
                return new Dummy();
            default:
                throw new BadConfigurationException("AI engine adapter implementation unknown: " + aiEngineAdapterImplementation + ". Available: KUBERNETES_API");
        }
    }

    private static PlatformAdapter selectPlatformAdapter(Map<String, Object> initialConfig, InputParameters inputParameters) throws BadConfigurationException {
        String platformAdapterImplementation = (String) initialConfig.get("PLATFORM_ADAPTER");
        Map<String, Object> config;
        switch (platformAdapterImplementation) {
            case "INCISIVE":
                config = loadEnvironmentVariables(Incisive.getEnvironmentVariables());
                return new Incisive(config, inputParameters.getJobId(), inputParameters.getModelMetadata());
            case "DUMMY":
                return new platform_adapter.types.Dummy();
            default:
                throw new BadConfigurationException("Platform adapter implementation unknown: " + platformAdapterImplementation + ". Available: INCISIVE");
        }
    }

}
