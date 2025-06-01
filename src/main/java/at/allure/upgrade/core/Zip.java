package at.allure.upgrade.core;

import at.allure.upgrade.utils.ZipUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class Zip {
    public final Path path;
    public final Map<String, byte[]> files;


    public Zip(Path path) {
        this.path = path;
        this.files = ZipUtils.readFiles(path);
    }

    public Zip addDirectory(Path dir) {
        if (!Files.isDirectory(dir))
            return this;
        try (Stream<Path> pluginFiles = Files.list(pluginPath)) {
            pluginFiles.filter(Files::isRegularFile).forEach(path -> {
                String inZipName = path.relativize(pluginPath).toString();
                zip.add(inZipName, path);
            });
        }
        files.put(inZipName, ZipUtils.readAllBytes(file));
        return this;
    }

    public Zip add(String inZipName, Path file) {
        files.put(inZipName, ZipUtils.readAllBytes(file));
        return this;
    }

    public Zip add(Path path, String content) {
        files.put(path.toString(), content.getBytes());
        return this;
    }

    public Zip update(Path path, UnaryOperator<String> updateContent) {
        String content = new String(files.get(path.toString()));
        String updatedContent = updateContent.apply(content);
        files.put(path.toString(), updatedContent.getBytes());
        return this;
    }

    public void save() {
        String oldName = "old_" + path.getFileName();
        Path oldPath = path.getParent().resolve(oldName);
        try {
            Files.move(path, oldPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось переместить файл " + path + " -> " + oldPath, e);
        }
        try {
            ZipUtils.save(path, files);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл " + path, e);
        }
    }
}
