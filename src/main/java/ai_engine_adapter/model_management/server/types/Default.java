package ai_engine_adapter.model_management.server.types;

import ai_engine_adapter.model_management.server.AIEngineServerModelManagementAdapter;
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

public class Default implements AIEngineServerModelManagementAdapter {

    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = new ArrayList<>();
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_UNMERGED_MODELS_DIRECTORY_PATH", EnvironmentVariableType.STRING, "/usr/application/output/model/"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_MERGED_MODEL_PATH", EnvironmentVariableType.STRING, "/usr/application/input/models/"));
        abstractClassVariables.add(new EnvironmentVariable("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_EVALUATION_METRICS_DIRECTORY_PATH", EnvironmentVariableType.STRING, "/usr/application/output/evaluation_metrics/"));
        return abstractClassVariables;
    }

    private final String inputUnMergedModelsDirectoryPath;
    private final String outputMergedModelPath;
    private final String outputEvaluationMetricsDirectoryPath;

    public Default(Map<String, Object> config) {
        this.inputUnMergedModelsDirectoryPath = (String) config.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_UNMERGED_MODELS_DIRECTORY_PATH");
        this.outputMergedModelPath = (String) config.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_MERGED_MODEL_PATH");
        this.outputEvaluationMetricsDirectoryPath = (String) config.get("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_EVALUATION_METRICS_DIRECTORY_PATH");
    }

    @Override
    public void initialize() throws AIEngineException {}

    @Override
    public void saveUnMergedModel(String clientId, byte[] bytes) throws AIEngineException {
        try {
            if (!Files.exists(Paths.get(this.inputUnMergedModelsDirectoryPath))) {
                Files.createDirectory(Paths.get(this.inputUnMergedModelsDirectoryPath));
            }

            // write compressed file to disk
            Path outputModel = Paths.get(this.inputUnMergedModelsDirectoryPath + "/model_" + clientId);
            ZipCompression.unZipFile(new ByteArrayInputStream(bytes), outputModel);
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException(String.format("Error while saving unmerged model from client %s", clientId), e);
        }
    }

    @Override
    public void saveEvaluationMetrics(String clientId, byte[] bytes) throws AIEngineException {
        try {
            if (!Files.exists(Paths.get(this.outputEvaluationMetricsDirectoryPath))) {
                Files.createDirectory(Paths.get(this.outputEvaluationMetricsDirectoryPath));
            }

            // write compressed file to disk
            Path outputEvaluationMetrics = Paths.get(String.format("%s/%s.json", this.outputEvaluationMetricsDirectoryPath, clientId));
            FileMethods.saveFile(bytes, outputEvaluationMetrics);
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException(String.format("Error while saving unmerged model from client %s", clientId), e);
        }
    }

    @Override
    public byte[] loadMergedModel() throws AIEngineException {
        byte[] mergedModelBytes;
        File modelDirectory = new File(this.outputMergedModelPath);

        try {
            // compress directory
            try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                zipFile(modelDirectory, modelDirectory.getName(), outputStream);
                mergedModelBytes = outputStream.toByteArray();
            }
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while loading merged model", e);
        }

        return mergedModelBytes;
    }

    @Override
    public void cleanUnMergedModels() throws AIEngineException {
        try {
            // delete unmerged models directories
            FileUtils.cleanDirectory(new File(this.inputUnMergedModelsDirectoryPath));
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while cleaning the AI Engine directories", e);
        }
    }

    @Override
    public void cleanMergedModel() throws AIEngineException {
        try {
            // delete merged model
            FileUtils.cleanDirectory(new File(this.outputMergedModelPath));
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while cleaning the AI Engine directories", e);
        }
    }

    @Override
    public void clean() throws AIEngineException {
        // empty
    }

}
