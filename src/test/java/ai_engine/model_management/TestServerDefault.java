package ai_engine.model_management;

import ai_engine_adapter.model_management.server.types.Default;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static ai_engine.model_management.FileUtils.listDirectoryFiles;

public class TestServerDefault {

    @AfterEach
    public void afterEach() throws Exception {
        FileUtils.cleanDirectory(new File("src/test/resources"));
    }

    @Test
    void saveAndLoadModelsSuccess() throws Exception {
        String inputUnMergedModelsDirectoryPath = "src/test/resources/input/models/";
        String outputMergedModelPath = "src/test/resources/output/model/";

        // load adapter
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_UNMERGED_MODELS_DIRECTORY_PATH", inputUnMergedModelsDirectoryPath);
        config.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_MERGED_MODEL_PATH", outputMergedModelPath);
        Default modelManagement = new Default(config);

        // create model files and write something into them
        Files.createDirectories(Paths.get(outputMergedModelPath));
        Files.createDirectories(Paths.get(inputUnMergedModelsDirectoryPath));
        Path modelFile = Paths.get(outputMergedModelPath + "/model.pt");
        Path configFile = Paths.get(outputMergedModelPath + "/config.json");
        Files.createFile(modelFile);
        Files.createFile(configFile);
        List<String> lines = Arrays.asList("test model content 1", "test model content 2");
        Files.write(modelFile, lines, StandardCharsets.UTF_8);
        lines = List.of("{\"test\": \"dummy_json\"}");
        Files.write(configFile, lines, StandardCharsets.UTF_8);

        // use adapter to transform them to a byte array
        byte[] bytes = modelManagement.loadMergedModel();

        // use adapter to save them again
        modelManagement.saveUnMergedModel("pod_id", bytes);

        // assure files are ok
        List<String> directoryFiles = listDirectoryFiles(outputMergedModelPath);
        Assertions.assertEquals(Arrays.asList("model.pt", "config.json"), directoryFiles);
        directoryFiles = listDirectoryFiles(inputUnMergedModelsDirectoryPath);
        Assertions.assertEquals(Arrays.asList("model_pod_id", "model.pt", "config.json"), directoryFiles);

        // assure model files contents are ok
        lines = Files.lines(Paths.get(outputMergedModelPath + "/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
        lines = Files.lines(Paths.get(outputMergedModelPath + "/config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);
        lines = Files.lines(Paths.get(inputUnMergedModelsDirectoryPath + "/model_pod_id/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
        lines = Files.lines(Paths.get(inputUnMergedModelsDirectoryPath + "/model_pod_id/config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);

        // clean directories
        modelManagement.cleanMergedModel();
        modelManagement.cleanUnMergedModels();

        // assure they are cleaned
        directoryFiles = listDirectoryFiles(outputMergedModelPath);
        Assertions.assertEquals(new ArrayList<>(), directoryFiles);
        directoryFiles = listDirectoryFiles(inputUnMergedModelsDirectoryPath);
        Assertions.assertEquals(new ArrayList<>(), directoryFiles);
    }

    @Test
    void saveEvaluationMetricsSuccess() throws Exception {
        String outputEvaluationMetricsDirectoryPath = "src/test/resources/output/evaluation_metrics/";
        Files.createDirectories(Paths.get(outputEvaluationMetricsDirectoryPath));

        // load adapter
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_EVALUATION_METRICS_DIRECTORY_PATH", outputEvaluationMetricsDirectoryPath);
        Default modelManagement = new Default(config);

        // create ground truth evaluation metrics
        JSONObject groundTruth = new JSONObject();
        JSONArray evaluationMetricsGroundTruthJSONArray = new JSONArray();
        evaluationMetricsGroundTruthJSONArray.put(new JSONObject("{\"name\": \"f1-score\", \"value\": 2}"));
        evaluationMetricsGroundTruthJSONArray.put(new JSONObject("{\"name\": \"accuracy\", \"value\": 1}"));
        groundTruth.put("evaluation_metrics", evaluationMetricsGroundTruthJSONArray);

        // use adapter to store them
        modelManagement.saveEvaluationMetrics("data-partner-1", groundTruth.toString().getBytes(StandardCharsets.UTF_8));


        // assure files are ok
        List<String> directoryFiles = listDirectoryFiles(outputEvaluationMetricsDirectoryPath);
        Assertions.assertEquals(List.of("data-partner-1.json"), directoryFiles);

        // assure model files contents are ok
        byte[] output = Files.readAllBytes(Paths.get(String.format("%s/data-partner-1.json", outputEvaluationMetricsDirectoryPath)));
        JSONObject outputJSON = new JSONObject(new String(output, StandardCharsets.UTF_8));
        Assertions.assertEquals(groundTruth.toString(), outputJSON.toString());
    }

}
