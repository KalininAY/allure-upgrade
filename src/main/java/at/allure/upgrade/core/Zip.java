package at.allure.upgrade.core;

import at.allure.upgrade.utils.ZipUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;

public class Zip {
    public final Path path;
    public final Map<String, byte[]> files;
    public final String rootDir;


    public Zip(Path path) {
        this.path = path;
        ZipUtils.ZipContent content = ZipUtils.readFiles(path);
        this.files = content.files;
        this.rootDir = content.rootDir;
    }


    public Zip add(String inZipName, byte[] bytes) {
        files.put(inZipName, bytes);
        return this;
    }

    public Zip update(Path path, UnaryOperator<String> updateContent) {
        String content = new String(files.get(path.toString()));
        String updatedContent = updateContent.apply(content);
        files.put(path.toString(), updatedContent.getBytes());
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
