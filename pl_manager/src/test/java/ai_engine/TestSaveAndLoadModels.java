package ai_engine;

import ai_engine_adapter.AIEngineAdapter;
import ai_engine_adapter.types.DummyTestFiles;
import exceptions.AIEngineException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestSaveAndLoadModels {

    @Test
    void saveAndLoadModelsSuccess() throws Exception {
        try {
            String mergedModelPath = "src/test/resources/output/model";
            String unMergedModelsDirectoryPath = "src/test/resources/input/models";

            // load adapter
            AIEngineAdapter.changeStaticPaths(mergedModelPath, unMergedModelsDirectoryPath);
            AIEngineAdapter aiEngineAdapter = new DummyTestFiles();

            // create model files and write something into them
            Files.createDirectories(Paths.get(mergedModelPath));
            Files.createDirectories(Paths.get(unMergedModelsDirectoryPath));
            Path modelFile = Paths.get(mergedModelPath + "/model.pt");
            Path configFile = Paths.get(mergedModelPath + "/config.json");
            Files.createFile(modelFile);
            Files.createFile(configFile);
            List<String> lines = Arrays.asList("test model content 1", "test model content 2");
            Files.write(modelFile, lines, StandardCharsets.UTF_8);
            lines = List.of("{\"test\": \"dummy_json\"}");
            Files.write(configFile, lines, StandardCharsets.UTF_8);

            // use adapter to transform them to a byte array
            byte[] bytes = aiEngineAdapter.loadMergedModel();

            // use adapter to save them again
            aiEngineAdapter.saveUnMergedModel("pod_id", bytes);

            // assure files are ok
            List<String> directoryFiles = listDirectoryFiles(mergedModelPath);
            Assertions.assertEquals(Arrays.asList("model.pt", "config.json"), directoryFiles);
            directoryFiles = listDirectoryFiles(unMergedModelsDirectoryPath);
            Assertions.assertEquals(Arrays.asList("model_pod_id", "model.pt", "config.json"), directoryFiles);

            // assure model files contents are ok
            lines = Files.lines(Paths.get(mergedModelPath + "/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
            lines = Files.lines(Paths.get(mergedModelPath + "/config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);
            lines = Files.lines(Paths.get(unMergedModelsDirectoryPath + "/model_pod_id/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
            lines = Files.lines(Paths.get(unMergedModelsDirectoryPath + "/model_pod_id/config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);

            // clean directories
            aiEngineAdapter.cleanDirectories();

            // assure they are cleaned
            directoryFiles = listDirectoryFiles(mergedModelPath);
            Assertions.assertEquals(new ArrayList<>(), directoryFiles);
            directoryFiles = listDirectoryFiles(unMergedModelsDirectoryPath);
            Assertions.assertEquals(new ArrayList<>(), directoryFiles);

        } catch (AIEngineException e) {
            e.getException().printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // clean test environment
            FileUtils.cleanDirectory(new File("src/test/resources"));
        }
    }

    private static List<String> listDirectoryFiles(String dir) throws IOException {
        List<String> directoryFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    directoryFiles.add(path.getFileName().toString());
                } else {
                    directoryFiles.add(path.getFileName().toString());
                    directoryFiles.addAll(listDirectoryFiles(path.toString()));
                }
            }
        }
        return directoryFiles;
    }

}
