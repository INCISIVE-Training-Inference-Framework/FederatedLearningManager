package ai_engine.model_management;

import ai_engine_adapter.model_management.client.types.Default;
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

public class TestClientDefault {

    @AfterEach
    public void afterEach() throws Exception {
        FileUtils.cleanDirectory(new File("src/test/resources"));
    }

    @Test
    void saveAndLoadModelsSuccess() throws Exception {
        String inputMergedModelPath = "src/test/resources/input/model";
        String OutputUnMergedModelPath = "src/test/resources/output/model";

        // create directories
        Files.createDirectories(Paths.get(inputMergedModelPath));
        Files.createDirectories(Paths.get(OutputUnMergedModelPath));

        // load adapter
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_INPUT_MERGED_MODEL_PATH", inputMergedModelPath);
        config.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_UNMERGED_MODEL_PATH", OutputUnMergedModelPath);
        Default modelManagement = new Default(config);

        // create model files and write something into them
        Path modelFile = Paths.get(OutputUnMergedModelPath + "/model.pt");
        Path modelConfigFile = Paths.get(OutputUnMergedModelPath + "/model_config.json");
        Files.createFile(modelFile);
        Files.createFile(modelConfigFile);
        List<String> lines = Arrays.asList("test model content 1", "test model content 2");
        Files.write(modelFile, lines, StandardCharsets.UTF_8);
        lines = List.of("{\"test\": \"dummy_json\"}");
        Files.write(modelConfigFile, lines, StandardCharsets.UTF_8);

        // use adapter to transform it to a byte array
        byte[] bytes = modelManagement.loadUnMergedModel();

        // use adapter to save it again
        modelManagement.saveMergedModel(bytes);

        // assure files are ok
        List<String> directoryFiles = listDirectoryFiles(inputMergedModelPath);
        Assertions.assertEquals(Arrays.asList("model.pt", "model_config.json"), directoryFiles);
        directoryFiles = listDirectoryFiles(OutputUnMergedModelPath);
        Assertions.assertEquals(Arrays.asList("model.pt", "model_config.json"), directoryFiles);

        // assure model files contents are ok
        lines = Files.lines(Paths.get(inputMergedModelPath + "/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
        lines = Files.lines(Paths.get(inputMergedModelPath + "/model_config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);
        lines = Files.lines(Paths.get(OutputUnMergedModelPath + "/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
        lines = Files.lines(Paths.get(OutputUnMergedModelPath + "/model_config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
        Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);

        // use adapter to clean files
        modelManagement.cleanDirectories();

        // assure files are deleted
        directoryFiles = listDirectoryFiles(inputMergedModelPath);
        Assertions.assertEquals(new ArrayList<>(), directoryFiles);
        directoryFiles = listDirectoryFiles(OutputUnMergedModelPath);
        Assertions.assertEquals(new ArrayList<>(), directoryFiles);
    }

    @Test
    void loadEvaluationMetricsSuccess() throws Exception {
        String outputEvaluationMetricsPath = "src/test/resources/evaluation_metrics.json";

        // load adapter
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_MODEL_MANAGEMENT_ADAPTER_OUTPUT_EVALUATION_METRICS_PATH", outputEvaluationMetricsPath);
        Default modelManagement = new Default(config);

        // create ground truth evaluation metrics
        JSONObject groundTruth = new JSONObject();
        JSONArray evaluationMetricsGroundTruthJSONArray = new JSONArray();
        evaluationMetricsGroundTruthJSONArray.put(new JSONObject("{\"name\": \"f1-score\", \"value\": 2}"));
        evaluationMetricsGroundTruthJSONArray.put(new JSONObject("{\"name\": \"accuracy\", \"value\": 1}"));
        groundTruth.put("evaluation_metrics", evaluationMetricsGroundTruthJSONArray);

        // write evaluation metrics to file
        Files.createFile(Paths.get(outputEvaluationMetricsPath));
        Files.writeString(Paths.get(outputEvaluationMetricsPath), groundTruth.toString());

        // use adapter to load them
        byte[] output = modelManagement.loadEvaluationMetrics();
        JSONObject outputJSON = new JSONObject(new String(output, StandardCharsets.UTF_8));

        // assure contents are ok
        Assertions.assertEquals(outputJSON.toString(), groundTruth.toString());
    }

}
