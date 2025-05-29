package at;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.function.UnaryOperator;

public class Zip {
    public final Path path;
    public final Map<String, byte[]> files;


    public Zip(Path path) {
        this.path = path;
        this.files = ZipUtils.readFiles(path);
    }

    public Zip add(Path file) {
        files.put(file.toString(), ZipUtils.readAllBytes(file));
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
