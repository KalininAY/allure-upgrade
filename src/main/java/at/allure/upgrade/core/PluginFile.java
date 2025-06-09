package at.allure.upgrade.core;


import at.allure.upgrade.utils.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Вспомогательный класс для хранения информации о файле плагина
 */
public abstract class PluginFile {
    public static final String PLUGIN_ID = "id: resultiks";
    public static final String PLUGIN_YML = "allure-plugin.yml";
    public static final Path PLUGIN_DIR = Paths.get("plugins", "resultiks-plugin");
    public static final Path STATIC_DIR = Paths.get("static");

    public final Path inZipPath;

    protected PluginFile(Path inZipPath) {
        this.inZipPath = inZipPath;
    }

    /**
     * Читает содержимое файла
     */
    public abstract List<String> readLines() throws IOException;

    public abstract byte[] getBytes() throws IOException;

    /**
     * Проверяет, является ли файл конфигурацией плагина
     */
    public boolean isPluginYml() {
        return inZipPath.getFileName().toString().equals(PLUGIN_YML);
    }

    /**
     * Проверяет наличие ID плагина в конфигурации
     */
    public boolean hasPluginId() {
        try {
            return readLines().stream().anyMatch(s -> s.trim().equals(PLUGIN_ID));
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * если файл на диске
     */
    public static class FromPath extends PluginFile {
        final Path filePath;


        public FromPath(Path inZipPath, Path filePath) {
            super(inZipPath);
            assert filePath != null;
            this.filePath = filePath;
        }

        public List<String> readLines() throws IOException {
            return Files.readAllLines(filePath);
        }

        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(filePath);
        }

        @Override
        public String toString() {
            return "PluginFile.FromPath{" +
                    "inZipPath='" + inZipPath + '\'' +
                    ", filePath=" + filePath +
                    '}';
        }
    }


    /**
     * если файл в jar
     */
    public static class FromResources extends PluginFile {
        final String resourcePath;


        public FromResources(Path resourcePath) {
            super(resourcePath);
            assert resourcePath != null;
            this.resourcePath = resourcePath.toString().replace("\\", "/"); // независимо от ОС
        }

        public List<String> readLines() throws IOException {
            try (InputStream is = PluginFile.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null)
                    throw new IOException("Не найден ресурс: " + resourcePath);
                byte[] bytes = ZipUtils.readStreamToByteArray(is);
                return Arrays.asList(new String(bytes).split("\\s*\n"));
            }
        }

        public byte[] getBytes() throws IOException {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null)
                    throw new IOException("Не найден ресурс: " + resourcePath);
                return ZipUtils.readStreamToByteArray(is);
            }
        }

        @Override
        public String toString() {
            return "PluginFile.FromResources{" +
                    "inZipPath='" + inZipPath + '\'' +
                    ", resourcePath=" + resourcePath +
                    '}';
        }
    }

}
