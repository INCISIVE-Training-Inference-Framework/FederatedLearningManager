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
            String mergedModelPath = "src/test/resources/input/model";
            String unMergedModelPath = "src/test/resources/output/model";

            // create directories
            Files.createDirectories(Paths.get(mergedModelPath));
            Files.createDirectories(Paths.get(unMergedModelPath));

            // load adapter
            AIEngineAdapter.changeStaticPaths(mergedModelPath, unMergedModelPath);
            AIEngineAdapter aiEngineAdapter = new DummyTestFiles();

            // create model files and write something into them
            Path modelFile = Paths.get(unMergedModelPath + "/model.pt");
            Path modelConfigFile = Paths.get(unMergedModelPath + "/model_config.json");
            Files.createFile(modelFile);
            Files.createFile(modelConfigFile);
            List<String> lines = Arrays.asList("test model content 1", "test model content 2");
            Files.write(modelFile, lines, StandardCharsets.UTF_8);
            lines = List.of("{\"test\": \"dummy_json\"}");
            Files.write(modelConfigFile, lines, StandardCharsets.UTF_8);

            // use adapter to transform it to a byte array
            byte[] bytes = aiEngineAdapter.loadUnMergedModel();

            // use adapter to save it again
            aiEngineAdapter.saveMergedModel(bytes);

            // assure files are ok
            List<String> directoryFiles = listDirectoryFiles(mergedModelPath);
            Assertions.assertEquals(Arrays.asList("model.pt", "model_config.json"), directoryFiles);
            directoryFiles = listDirectoryFiles(unMergedModelPath);
            Assertions.assertEquals(Arrays.asList("model.pt", "model_config.json"), directoryFiles);

            // assure model files contents are ok
            lines = Files.lines(Paths.get(mergedModelPath + "/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
            lines = Files.lines(Paths.get(mergedModelPath + "/model_config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);
            lines = Files.lines(Paths.get(unMergedModelPath + "/model.pt"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(Arrays.asList("test model content 1", "test model content 2"), lines);
            lines = Files.lines(Paths.get(unMergedModelPath + "/model_config.json"), StandardCharsets.UTF_8).collect(Collectors.toList());
            Assertions.assertEquals(List.of("{\"test\": \"dummy_json\"}"), lines);

            // use adapter to clean files
            aiEngineAdapter.cleanDirectories();

            // assure files are deleted
            directoryFiles = listDirectoryFiles(mergedModelPath);
            Assertions.assertEquals(new ArrayList<>(), directoryFiles);
            directoryFiles = listDirectoryFiles(unMergedModelPath);
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
