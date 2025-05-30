package at.allure.upgrade.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class ZipUtils {

    public static byte[] readAllBytes(Path p) {
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, byte[]> readFiles(Path zip) {
        Map<String, byte[]> map = new LinkedHashMap<>();
        try (InputStream inputStream = new ZipInputStream(Files.newInputStream(zip));
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                map.put(entry.getName(), readStreamToByteArray(zipInputStream));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static void save(Path path, Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            files.forEach((pathName, bytes) -> {
                try {
                    ZipEntry newEntry = new ZipEntry(pathName);
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
