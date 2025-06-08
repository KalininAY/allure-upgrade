package at.allure.upgrade.core;


import at.allure.upgrade.utils.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Вспомогательный класс для хранения информации о файле плагина
 */
public class PluginFile {
    public static final String PLUGIN_ID = "id: resultiks";
    public static final String PLUGIN_YML = "allure-plugin.yml";
    public static final String PLUGIN_PREFIX = "plugins/resultiks-plugin/";
    public static final String STATIC_DIR = "static/";

    final String inZipName;
    final Path filePath; // если файл на диске
    final String resourcePath; // если файл в jar

    public PluginFile(String inZipName, Path filePath) {
        this.inZipName = inZipName;
        this.filePath = filePath;
        this.resourcePath = null;
    }

    public PluginFile(String inZipName, String resourcePath) {
        this.inZipName = inZipName;
        this.filePath = null;
        this.resourcePath = resourcePath;
    }

    /**
     * Проверяет, является ли файл конфигурацией плагина
     */
    public boolean isPluginYml() {
        return inZipName.endsWith("/" + PLUGIN_YML);
    }

    /**
     * Читает содержимое файла (с диска или из ресурсов)
     */
    public List<String> readLines() throws IOException {
        if (filePath != null) {
            return Files.readAllLines(filePath);
        } else if (resourcePath != null) {
            try (InputStream is = PluginFile.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) throw new IOException("Не найден ресурс: " + resourcePath);
                byte[] bytes = ZipUtils.readStreamToByteArray(is);
                return Arrays.asList(new String(bytes).split("\\s*\n"));
            }
        }
        throw new IOException("Неизвестный тип файла");
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

    public byte[] getBytes() throws IOException {
        if (filePath != null) {
            return Files.readAllBytes(filePath);

        } else if (resourcePath != null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null)
                    throw new IOException("Не найден ресурс: " + resourcePath);
                return ZipUtils.readStreamToByteArray(is);
            }
        }
        throw new IllegalStateException("Файл не в директории и не в ресурсах " + this);
    }

    @Override
    public String toString() {
        return "PluginFile{" +
                "inZipName='" + inZipName + '\'' +
                ", filePath=" + filePath +
                ", resourcePath='" + resourcePath + '\'' +
                '}';
    }
}
