package ai_engine.model_management;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<String> listDirectoryFiles(String dir) throws IOException {
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
