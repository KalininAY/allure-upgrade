package at.allure.upgrade.utils;

import at.allure.upgrade.core.PluginFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static at.allure.upgrade.core.PluginFile.*;

public abstract class PluginFileUtils {


    /**
     * Создает список файлов плагина из директории
     */
    public static List<PluginFile> fromDirectory(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) {
            throw new RuntimeException("Переданная директория плагина не существует");
        }

        Map<String, Path> files = getAllFilesFromDirectory(dir);
        if (files.isEmpty()) {
            throw new RuntimeException("В папке плагина нет файлов");
        }

        Path pluginYml = files.get(PLUGIN_YML);
        if (pluginYml == null) {
            throw new RuntimeException("В папке плагина не найден " + PLUGIN_YML);
        }

        return processPluginFiles(files, pluginYml);
    }

    /**
     * Создает список файлов плагина из ресурсов
     */
    public static List<PluginFile> fromResources() throws Exception {
        ClassLoader cl = PluginFile.class.getClassLoader();
        URL dirURL = cl.getResource("plugins");
        if (dirURL == null) {
            throw new RuntimeException("Папка плагина не найдена в ресурсах: plugins");
        }

        if (dirURL.getProtocol().equals("file")) {
            return fromFileSystem(dirURL);
        } else if (dirURL.getProtocol().equals("jar")) {
            return fromJar(dirURL);
        } else {
            throw new UnsupportedOperationException("Неизвестный протокол ресурсов: " + dirURL.getProtocol());
        }
    }

    private static Map<String, Path> getAllFilesFromDirectory(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .collect(Collectors.toMap(p -> p.getFileName().toString(), p -> p));
        }
    }

    private static List<PluginFile> processPluginFiles(Map<String, Path> files, Path pluginYml) throws Exception {
        List<String> lines = Files.readAllLines(pluginYml);
        String jarName = findJarName(files, lines);
        List<String> staticFiles = findStaticFiles(lines);

        List<PluginFile> result = new ArrayList<>();
        result.add(new PluginFile.FromPath(PLUGIN_DIR.resolve(PLUGIN_YML), pluginYml));

        if (jarName != null) {
            Path jarPath = getValidPath(jarName, files);
            result.add(new PluginFile.FromPath(PLUGIN_DIR.resolve(jarName), jarPath));
        }

        for (String jsOrCss : staticFiles) {
            Path path = getValidPath(jsOrCss, files);
            result.add(new PluginFile.FromPath(PLUGIN_DIR.resolve(STATIC_DIR).resolve(jsOrCss), path));
        }

        return result;
    }

    private static Path getValidPath(String fileName, Map<String, Path> files) {
        Path p = files.get(fileName);
        if (p == null || !Files.exists(p)) {
            throw new RuntimeException("Не найден файл, указанный в " + PLUGIN_YML + ": " + fileName);
        }
        return p;
    }

    private static String findJarName(Map<String, Path> files, List<String> lines) {
        String section = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("extensions:")) {
                section = "extensions";
            } else if (trimmed.startsWith("-") && "extensions".equals(section)) {
                String value = trimmed.substring(1).trim();
                return files.keySet().stream()
                        .filter(name -> name.endsWith(".jar"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Не найден .jar файл с классом " + value));
            }
        }
        return null;
    }

    private static List<String> findStaticFiles(List<String> lines) {
        List<String> staticFiles = new ArrayList<>();
        String section = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("jsFiles:") || trimmed.startsWith("cssFiles:")) {
                section = trimmed.substring(0, trimmed.length() - 1);
            } else if (trimmed.startsWith("-") && section != null) {
                staticFiles.add(trimmed.substring(1).trim());
            }
        }
        return staticFiles;
    }

    private static List<PluginFile> fromFileSystem(URL dirURL) throws Exception {
        List<PluginFile> result = new ArrayList<>();
        Path resPath = Paths.get(dirURL.toURI());
        try (Stream<Path> stream = Files.walk(resPath)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path inZipPath = resPath.getParent().relativize(path);
                result.add(new PluginFile.FromPath(inZipPath, path));
            });
        }
        return result;
    }

    private static List<PluginFile> fromJar(URL dirURL) throws Exception {
        List<PluginFile> result = new ArrayList<>();
        String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("plugins/") && !entry.isDirectory()) {
                    result.add(new PluginFile.FromResources(Paths.get(entry.getName())));
                }
            }
        }
        return result;
    }
}
