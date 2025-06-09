package at.allure.upgrade.core;

import at.allure.upgrade.utils.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("UnusedReturnValue")
public class Zip {
    public final Path path;
    public final Map<Path, byte[]> files;
    public final String rootDir;


    public Zip(Path path) {
        this.path = path;
        this.files = new LinkedHashMap<>();
        String tempRootDir = null;
        try (InputStream inputStream = Files.newInputStream(path);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                Path inZipPath = Paths.get(entry.getName());
                if (inZipPath.getNameCount() > 1) { // если путь может содержать rootDir
                    if (tempRootDir == null) {
                        tempRootDir = inZipPath.getName(0).toString();
                    }
                    // если путь содержит rootDir, вычитаем его
                    if (inZipPath.getName(0).toString().equals(tempRootDir)) {
                        inZipPath = inZipPath.subpath(1, inZipPath.getNameCount());
                    }
                }
                // кладём без rootDir
                files.put(inZipPath, ZipUtils.readStreamToByteArray(zipInputStream));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        rootDir = tempRootDir == null ? "" : tempRootDir;
    }


    public Zip add(Path inZipPath, byte[] bytes) {
        files.put(inZipPath, bytes);
        return this;
    }

    public Zip updateContent(Path inZipPath, UnaryOperator<String> updateContent) {
        String content = new String(files.get(inZipPath));
        String updatedContent = updateContent.apply(content);
        files.put(inZipPath, updatedContent.getBytes());
        return this;
    }

    public void save() {
        Path newPath = ZipUtils.update(path);
        try {
            ZipUtils.save(newPath, files, rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл " + newPath, e);
        }
    }
}
