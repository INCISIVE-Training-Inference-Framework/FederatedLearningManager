import ai_engine_adapter.AIEngineAdapter;
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

    public static class InputParameters {

        private final String jobId;
        private final String jobCompleteId;
        private final String jobName;
        private final String podId;
        private final int numberOfIterations;
        private final String useCase;

        public InputParameters(String jobId, String jobCompleteId, String jobName, String podId, int numberOfIterations, String useCase) {
            this.jobId = jobId;
            this.jobCompleteId = jobCompleteId;
            this.jobName = jobName;
            this.podId = podId;
            this.numberOfIterations = numberOfIterations;
            this.useCase = useCase;
        }

        public String getJobId() {
            return jobId;
        }

        public String getJobCompleteId() {
            return jobCompleteId;
        }

        public String getJobName() {
            return jobName;
        }

        public String getPodId() {
            return podId;
        }

        public int getNumberOfIterations() {
            return numberOfIterations;
        }

        public String getUseCase() {
            return useCase;
        }

        @Override
        public String toString() {
            return "InputParameters{" +
                    "jobId='" + jobId + '\'' +
                    ", jobCompleteId='" + jobCompleteId + '\'' +
                    ", jobName='" + jobName + '\'' +
                    ", podId='" + podId + '\'' +
                    ", numberOfIterations=" + numberOfIterations +
                    ", useCase='" + useCase + '\'' +
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
            domain.run(inputParameters.getJobId(), inputParameters.getNumberOfIterations(), inputParameters.getUseCase());
        } catch (BadInputParametersException | BadConfigurationException e) {
            logger.error(e.getMessage());
            System.exit(1);
        } catch (FailureEndSignal e) {
            System.exit(1);
        }
    }

    private static InputParameters parseInputParameters(String[] args) throws BadInputParametersException {
        if (args.length != 6) throw new BadInputParametersException(
                // TODO refactor input parameters
                "There should be four input parameters -> " +
                        "job_id(String), " +  // global for all pods
                        "job_complete_id(String), " +  // global for all pods
                        "job_name(String), " +  // specific for the client
                        "pod_id(String), " +
                        "number_of_iterations(int), " +
                        "and use_case(String)");
        String jobId = args[0];
        String jobCompleteId = args[1];
        String jobName = args[2];
        String podId = args[3];
        int numberOfIterations;
        try {
            numberOfIterations = Integer.parseInt(args[4]);
        }
        catch (NumberFormatException ex){
            throw new BadInputParametersException("The number of iterations must be an integer number");
        }
        String useCase = args[5];
        if (!useCase.equals("training_from_scratch") && !useCase.equals("training_from_pretrained_model")) {
            throw new BadInputParametersException("The use_case parameter can only take the following values: training_from_scratch and training_from_pretrained_model");
        }
        return new InputParameters(jobId, jobCompleteId, jobName, podId, numberOfIterations, useCase);
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
                return new KafkaCommunication(config, inputParameters.getJobCompleteId(), inputParameters.getPodId());
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
                return new ai_engine_adapter.types.Dummy();
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
                return new Incisive(config, inputParameters.getJobId());
            case "DUMMY":
                return new platform_adapter.types.Dummy();
            default:
                throw new BadConfigurationException("Platform adapter implementation unknown: " + platformAdapterImplementation + ". Available: INCISIVE");
        }
    }

}
