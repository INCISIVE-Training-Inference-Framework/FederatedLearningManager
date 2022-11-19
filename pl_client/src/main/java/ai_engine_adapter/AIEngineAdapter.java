package ai_engine_adapter;

import exceptions.AIEngineException;
import org.apache.commons.io.FileUtils;
import utils.ZipCompression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static utils.ZipCompression.zipFile;

public abstract class AIEngineAdapter {

    // only for testing
    public static void changeStaticPaths(String mergedModelPath, String unMergedModelPath) {
        MERGED_MODEL_PATH = mergedModelPath;
        UN_MERGED_MODEL_PATH = unMergedModelPath;
    }

    private static String MERGED_MODEL_PATH = "/usr/application/input/model";
    private static String UN_MERGED_MODEL_PATH = "/usr/application/output/model";

    public void initialize() throws AIEngineException {}

    public void saveMergedModel(byte[] bytes) throws AIEngineException {
        try {
            // write compressed file to disk
            ZipCompression.unZipFile(new ByteArrayInputStream(bytes), Paths.get(MERGED_MODEL_PATH));  // automatically creates directory if it does not exist
        } catch (IOException| IllegalArgumentException e) {
            throw new AIEngineException("Error while saving merged model", e);
        }
    }

    public byte[] loadUnMergedModel() throws AIEngineException {
        byte[] mergedModelBytes;
        File modelDirectory = new File(UN_MERGED_MODEL_PATH);

        try {
            // compress directory
            try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                zipFile(modelDirectory, modelDirectory.getName(), outputStream);
                mergedModelBytes = outputStream.toByteArray();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new AIEngineException("Error while loading unmerged model", e);
        }

        return mergedModelBytes;
    }

    public void cleanDirectories() throws AIEngineException {
        try {
            // delete merged model
            if (Files.exists(Paths.get(MERGED_MODEL_PATH))) {  // for the first iteration of training from scratch
                FileUtils.cleanDirectory(new File(MERGED_MODEL_PATH));
            }

            // delete unmerged model
            FileUtils.cleanDirectory(new File(UN_MERGED_MODEL_PATH));
        } catch (IOException | IllegalArgumentException e) {
            throw new AIEngineException("Error while cleaning the AI Engine directories", e);
        }
    }

    public abstract void waitAIEngineToBeReady() throws AIEngineException;

    public abstract void run(String useCase) throws AIEngineException;

    public abstract void endExecution() throws AIEngineException;

}
