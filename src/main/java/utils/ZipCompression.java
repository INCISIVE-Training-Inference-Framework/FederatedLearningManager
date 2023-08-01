package utils;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipCompression {

    public static void unZipFile(InputStream inputStream, Path destDirectory) throws IOException {
        File destDir = destDirectory.toFile();
        String parentDirectory = "/";
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry, parentDirectory);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    public static void zipFile(String pathToZip, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            if (pathToZip.endsWith("*")) {
                // compress only contents
                pathToZip = pathToZip.replace("*", "");
                File fileToZip = new File(pathToZip);
                zipFileRecursive(fileToZip, null, zipOut);
            } else {
                // compress contents including external folder
                File fileToZip = new File(pathToZip);
                String folderName = fileToZip.getName();
                zipFileRecursive(fileToZip, folderName, zipOut);
            }
        }
    }

    public static void zipFile(List<File> filesToZip, String fileName, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (File fileToZip: filesToZip) zipFileRecursive(fileToZip, fileName, zipOut);
        }
    }

    private static void zipFileRecursive(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName != null) {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    zipOut.closeEntry();
                } else {
                    zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                    zipOut.closeEntry();
                }
            }
            final File[] children = fileToZip.listFiles();
            for (final File childFile : children) {
                if (fileName != null) {
                    zipFileRecursive(childFile, fileName + "/" + childFile.getName(), zipOut);
                } else {
                    zipFileRecursive(childFile, childFile.getName(), zipOut);
                }
            }
            return;
        }
        try(FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry, String parentDirectory) throws IOException {
        File destFile;
        if (parentDirectory != null) {
            String destinationPath = destinationDir.getCanonicalPath() + File.separator + zipEntry.getName();
            destFile = new File(destinationPath);
        } else {
            destFile = new File(destinationDir, zipEntry.getName());
        }

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
