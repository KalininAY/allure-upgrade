package at.allure.upgrade.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
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

    public static class ZipContent {
        public final Map<String, byte[]> files;
        public final String rootDir;
        public ZipContent(Map<String, byte[]> files, String rootDir) {
            this.files = files;
            this.rootDir = rootDir;
        }
    }

    public static ZipContent readFiles(Path zip) {
        Map<String, byte[]> map = new LinkedHashMap<>();
        String rootDir = null;
        try (InputStream inputStream = Files.newInputStream(zip);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                Path inZipPath = Paths.get(entry.getName());
                if (rootDir == null && inZipPath.getNameCount() > 1) {
                    rootDir = inZipPath.getName(0).toString();
                }
                if (inZipPath.getNameCount() > 1 && rootDir != null && inZipPath.getName(0).toString().equals(rootDir)) {
                    String nameInZip = inZipPath.subpath(1, inZipPath.getNameCount()).toString();
                    map.put(nameInZip, readStreamToByteArray(zipInputStream));
                } else {
                    // если нет rootDir, кладём как есть
                    map.put(inZipPath.toString(), readStreamToByteArray(zipInputStream));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (rootDir == null) rootDir = "";
        return new ZipContent(map, rootDir);
    }

    public static void save(Path path, Map<String, byte[]> files, String rootDir) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            files.forEach((pathName, bytes) -> {
                try {
                    String entryName = rootDir.isEmpty() ? pathName : rootDir + "/" + pathName;
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

    public static Path update(Path path) {
        String fileName = path.getFileName().toString();
        int dotIdx = fileName.lastIndexOf('.');
        String newName;
        if (dotIdx > 0) {
            newName = fileName.substring(0, dotIdx) + "-with-resultiks" + fileName.substring(dotIdx);
        } else {
            newName = fileName + "-with-resultiks";
        }
        return path.getParent().resolve(newName);
    }

}
