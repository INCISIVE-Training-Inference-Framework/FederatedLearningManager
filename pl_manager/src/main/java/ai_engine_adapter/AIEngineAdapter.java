package ai_engine_adapter;

import exceptions.AIEngineException;
import org.apache.commons.io.FileUtils;
import utils.ZipCompression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static utils.ZipCompression.zipFile;

public abstract class AIEngineAdapter {

    // only for testing
    public static void changeStaticPaths(String mergedModelPath, String unMergedModelsDirectoryPath) {
        MERGED_MODEL_PATH = mergedModelPath;
        UN_MERGED_MODELS_DIRECTORY_PATH = unMergedModelsDirectoryPath;
    }

    private static String MERGED_MODEL_PATH = "/usr/application/output/model";
    private static String UN_MERGED_MODELS_DIRECTORY_PATH = "/usr/application/input/models";

    public void initialize() throws AIEngineException {}

    public void saveUnMergedModel(String podId, byte[] bytes) throws AIEngineException {
        try {
            if (!Files.exists(Paths.get(UN_MERGED_MODELS_DIRECTORY_PATH))) {
                Files.createDirectory(Paths.get(UN_MERGED_MODELS_DIRECTORY_PATH));
            }

            // write compressed file to disk
            Path outputModel = Paths.get(UN_MERGED_MODELS_DIRECTORY_PATH + "/model_" + podId);
            ZipCompression.unZipFile(new ByteArrayInputStream(bytes), outputModel);
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while saving unmerged model from pod " + podId, e);
        }
    }

    public byte[] loadMergedModel() throws AIEngineException {
        byte[] mergedModelBytes;
        File modelDirectory = new File(MERGED_MODEL_PATH);

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

    public void cleanDirectories() throws AIEngineException {
        try {
            // delete unmerged models directories
            FileUtils.cleanDirectory(new File(UN_MERGED_MODELS_DIRECTORY_PATH));

            // delete merged model
            FileUtils.cleanDirectory(new File(MERGED_MODEL_PATH));
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while cleaning the AI Engine directories", e);
        }
    }

    public abstract void waitAIEngineToBeReady() throws AIEngineException;

    public abstract void mergeModels() throws AIEngineException;

    public abstract void endExecution() throws AIEngineException;

}
