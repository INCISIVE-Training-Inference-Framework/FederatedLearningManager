package ai_engine_adapter.model_management.client.types;

import ai_engine_adapter.model_management.client.AIEngineClientModelManagementAdapter;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.AIEngineException;
import org.apache.commons.io.FileUtils;
import utils.FileMethods;
import utils.ZipCompression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static utils.ZipCompression.zipFile;

public class Default implements AIEngineClientModelManagementAdapter {

    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = new ArrayList<>();
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_MERGED_MODEL_PATH", EnvironmentVariableType.STRING, "/usr/application/input/model/"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_UNMERGED_MODEL_PATH", EnvironmentVariableType.STRING, "/usr/application/output/model/"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_EVALUATION_METRICS_PATH", EnvironmentVariableType.STRING, "/usr/application/output/evaluation_metrics.json"));
        return abstractClassVariables;
    }

    private final String inputMergedModelPath;
    private final String outputUnmergedModelPath;
    private final String outputEvaluationMetricsPath;

    public Default(Map<String, Object> config) {
        this.inputMergedModelPath = (String) config.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_MERGED_MODEL_PATH");
        this.outputUnmergedModelPath = (String) config.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_UNMERGED_MODEL_PATH");
        this.outputEvaluationMetricsPath = (String) config.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_EVALUATION_METRICS_PATH");
    }

    @Override
    public void initialize() throws AIEngineException {}

    @Override
    public void saveMergedModel(byte[] bytes) throws AIEngineException {
        try {
            // write compressed file to disk
            ZipCompression.unZipFile(new ByteArrayInputStream(bytes), Paths.get(this.inputMergedModelPath));  // automatically creates directory if it does not exist
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while saving merged model", e);
        }
    }

    @Override
    public byte[] loadUnMergedModel() throws AIEngineException {
        byte[] mergedModelBytes;
        Path modelDirectory = Path.of(this.outputUnmergedModelPath);

        try {
            // compress directory
            try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                zipFile(String.format("%s/*", modelDirectory), outputStream);
                mergedModelBytes = outputStream.toByteArray();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new AIEngineException("Error while loading unmerged model", e);
        }

        return mergedModelBytes;
    }

    @Override
    public byte[] loadEvaluationMetrics() throws AIEngineException {
        try {
            Path path = Paths.get(this.outputEvaluationMetricsPath);
            return FileMethods.readFile(path);
        } catch (IOException e) {
            throw new AIEngineException("Error while loading evaluation metrics", e);
        }
    }

    @Override
    public void cleanDirectories() throws AIEngineException {
        try {
            // delete merged model
            if (Files.exists(Paths.get(this.inputMergedModelPath))) {  // for the first iteration of training from scratch
                FileUtils.cleanDirectory(new File(this.inputMergedModelPath));
            }

            // delete unmerged model
            FileUtils.cleanDirectory(new File(this.outputUnmergedModelPath));
        } catch (IOException | IllegalArgumentException e) {
            throw new AIEngineException("Error while cleaning the AI Engine directories", e);
        }
    }

    @Override
    public void clean() throws AIEngineException {
        // empty
    }

}
