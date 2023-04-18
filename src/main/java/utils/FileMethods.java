package utils;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMethods {

    public static JSONObject readJson(InputStream inputStream) throws IOException {
        try (BufferedReader bufferReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;

            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            return new JSONObject(stringBuilder.toString());
        }
    }

    public static byte[] readFile(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public static void saveFile(byte[] bytes, Path path) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(String.valueOf(path))) {
            outputStream.write(bytes);
        }
    }

}
