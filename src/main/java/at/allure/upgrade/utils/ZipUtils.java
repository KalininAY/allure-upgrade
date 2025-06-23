package at.allure.upgrade.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class ZipUtils {

    public static void save(Path path, Map<Path, byte[]> files, String rootDir) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            files.forEach((inZipPath, bytes) -> {
                try {
                    inZipPath = rootDir.isEmpty() ? inZipPath : Paths.get(rootDir).resolve(inZipPath);
                    String entryName = inZipPath.toString();
                    ZipEntry newEntry = new ZipEntry(entryName);
                    zipOutputStream.putNextEntry(newEntry);
                    zipOutputStream.write(bytes);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Files.write(path, byteArrayOutputStream.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Считывает весь поток в массив байтов.
     *
     * @param inputStream Входной поток.
     * @return Массив байтов с содержимым потока.
     */
    public static byte[] readStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return byteArrayOutputStream.toByteArray();
    }

}
