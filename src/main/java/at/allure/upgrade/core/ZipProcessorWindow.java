package at.allure.upgrade.core;

import at.allure.upgrade.utils.AllureUtils;
import at.allure.upgrade.utils.ListUtils;
import at.allure.upgrade.utils.PluginFileUtils;
import at.allure.upgrade.utils.ZipUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ZipProcessorWindow extends JFrame {
    private static final Path ALLURE_CONFIG = Paths.get("config", "allure.yml");
    private static final String PLUGIN_ROW = "  - resultiks-plugin";

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton selectFileButton;
    private JLabel successIcon;
    private File selectedFile;
    private JTextArea consoleArea;
    private JScrollPane consoleScroll;
    private Path pluginDirPath;
    private JLabel pluginDirLabel;
    private JButton selectPluginDirButton;

    public ZipProcessorWindow() {
        init();
    }

    /**
     * Инициализация компонентов и интерфейса
     */
    public void init() {
        // Удаляем все компоненты, если они уже были добавлены
        getContentPane().removeAll();

        initializeComponents();
        setupLayout();
        setupEventHandlers();

        revalidate();
        repaint();
    }

    private void initializeComponents() {
        setTitle("Let's upgrade your allure");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(500, 300));
        setSize(500, 300);
        setLocationRelativeTo(null);
        setResizable(true);

        selectFileButton = new JButton("Выбрать ZIP архив с Allure");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusLabel = new JLabel("Выберите ZIP архив для обработки", JLabel.CENTER);

        // Создаем простую иконку галочки
        successIcon = new JLabel(createCheckIcon());
        successIcon.setVisible(false);

        // "Консоль" для вывода сообщений
        consoleArea = new JTextArea(10, 40);
        consoleArea.setEditable(false);
        consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        consoleScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Label и кнопка для выбора директории плагина
        pluginDirLabel = new JLabel("<html>Папка плагина:<br>" +
            (pluginDirPath != null ? pluginDirPath : "Встроенные ресурсы (jar)") +
            "</html>");
        selectPluginDirButton = new JButton("Выбрать папку плагина");
    }

    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(selectFileButton);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(statusLabel, BorderLayout.NORTH);
        centerPanel.add(progressBar, BorderLayout.CENTER);
        centerPanel.add(successIcon, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Нижняя панель: либо консоль, либо выбор папки плагина
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        if (selectedFile == null) {
            bottomPanel.add(pluginDirLabel, BorderLayout.WEST);
            bottomPanel.add(selectPluginDirButton, BorderLayout.SOUTH);
        } else {
            bottomPanel.add(consoleScroll, BorderLayout.CENTER);
        }
        add(bottomPanel, BorderLayout.SOUTH);

        // Добавляем отступы
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));
    }

    private void setupEventHandlers() {
        selectFileButton.addActionListener(e -> selectZipFile());
        selectPluginDirButton.addActionListener(e -> selectPluginDirectory());
    }

    private void selectZipFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP архивы", "zip"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            selectFileButton.setVisible(false);
            // После выбора архива пересобираем layout, чтобы показать консоль
            init();
            processZipFile();
        }
    }

    private void selectPluginDirectory() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setAcceptAllFileFilterUsed(false);
        int result = dirChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            pluginDirPath = dirChooser.getSelectedFile().toPath();
            pluginDirLabel.setText("<html>Папка плагина:<br>" + pluginDirPath + "</html>");
        }
    }

    /**
     * Валидация папки плагина: должен быть allure-plugin.yml с id: resultiks
     */
    private List<PluginFile> getAndValidatePluginDir() {
        List<PluginFile> files;
        try {
            files = pluginDirPath != null 
                    ? PluginFileUtils.fromDirectory(pluginDirPath)
                    : PluginFileUtils.fromResources();
        } catch (Exception e) {
            handleError("Ошибка при получении файлов плагина: " + e.getMessage());
            return Collections.emptyList();
        }

        if (!validatePluginFiles(files)) {
            return Collections.emptyList();
        }

        selectFileButton.setEnabled(true);
        statusLabel.setText("Выберите ZIP архив для обработки");
        return files;
    }

    /**
     * Обработка ошибки: вывод в статус и консоль, блокировка кнопки
     */
    private void handleError(String error) {
        selectFileButton.setEnabled(false);
        setStatusError(error);
        printError(error);
    }

    /**
     * Валидация файлов плагина
     */
    private boolean validatePluginFiles(List<PluginFile> files) {
        PluginFile pluginYml = files.stream()
                .filter(PluginFile::isPluginYml)
                .findFirst()
                .orElse(null);
        if (pluginYml == null) {
            handleError("В папке плагина не найден allure-plugin.yml");
            return false;
        }

        if (!pluginYml.hasPluginId()) {
            handleError("В allure-plugin.yml не найдено 'id: resultiks'");
            return false;
        }

        return true;
    }

    /**
     * Устанавливает ошибку в statusLabel с выделением красным цветом
     */
    private void setStatusError(String message) {
        statusLabel.setText("<html><span style='color:red;'>Ошибка: " + message + "</span></html>");
    }

    /**
     * Выводит сообщение в "консоль"
     */
    private void print(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            consoleArea.append(time + "   " + message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private void printError(String message) {
        print("Ошибка: " + message);
    }

    private void processZipFile() {
        // Сброс состояния
        successIcon.setVisible(false);
        selectFileButton.setVisible(false);

        // Показываем прогресс-бар
        progressBar.setVisible(true);
        progressBar.setValue(0);
        statusLabel.setText("Проверка файла...");

        // Используем SwingWorker для выполнения в фоновом потоке
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            private Zip zip;
            private String errorMessage = null;

            @Override
            protected Void doInBackground() {
                List<PluginFile> pluginFiles = getPluginFiles();
                if (pluginFiles.isEmpty()) return null;

                if (!initializeZipFile()) return null;
                if (!validateAllureZip()) return null;
                if (!addPluginFiles(pluginFiles)) return null;
                if (!updateConfigFile()) return null;
                if (!saveZipFile()) return null;
                if (!verifyChanges(pluginFiles)) return null;

                publish(100);
                return null;
            }

            private List<PluginFile> getPluginFiles() {
                print("Проверка файлов плагина в " + (pluginDirPath == null ? "составе jar" : pluginDirPath) + "...");
                List<PluginFile> pluginFiles = getAndValidatePluginDir();
                if (pluginFiles.isEmpty()) {
                    publish(-1);
                    return pluginFiles;
                }
                print("Файлы плагина проверены");
                publish(5);
                return pluginFiles;
            }

            private boolean initializeZipFile() {
                print("Начало обработки файла " + selectedFile.getAbsolutePath() + "...");
                try {
                    zip = new Zip(selectedFile.toPath());
                    print("Файл успешно считан и распознан как архив");
                    publish(10);
                    return true;
                } catch (Exception e) {
                    errorMessage = "Не удалось открыть архив, " + e.getMessage();
                    publish(-1);
                    return false;
                }
            }

            private boolean validateAllureZip() {
                if (AllureUtils.isAllureZip(zip)) {
                    publish(20);
                    String allureVersion = AllureUtils.parseAllureVersion(zip);
                    setTitle(getTitle() + "  " + allureVersion);
                    print("В архиве найден Allure " + allureVersion + ", начинается модификация...");
                    publish(30);
                    return true;
                } else {
                    errorMessage = "Выбранный архив не содержит Allure";
                    publish(-1);
                    return false;
                }
            }

            private boolean addPluginFiles(List<PluginFile> pluginFiles) {
                try {
                    publish(35);
                    int totalFiles = pluginFiles.size();
                    if (totalFiles == 0) {
                        throw new RuntimeException("В папке плагина нет файлов для добавления.");
                    }
                    int startProgress = 35;
                    int endProgress = 75;
                    int progressRange = endProgress - startProgress;
                    int fileIndex = 0;
                    for (PluginFile pf : pluginFiles) {
                        print("Добавление файла: " + pf.inZipName);
                        zip.add(pf.inZipName, pf.getBytes());
                        int progress = startProgress + (int)(((double)++fileIndex / totalFiles) * progressRange);
                        publish(progress);
                    }
                    print("Плагин успешно добавлен (файлов: " + totalFiles + ")");
                    return true;
                } catch (Exception e) {
                    errorMessage = "Ошибка при добавлении плагина: " + e.getMessage();
                    publish(-1);
                    return false;
                }
            }

            private boolean updateConfigFile() {
                print("Обновление конфигурационного файла " + ALLURE_CONFIG + "...");
                try {
                    zip.update(
                        ALLURE_CONFIG,
                        content -> {
                            if (content.contains(PLUGIN_ROW)) {
                                return content;
                            }
                            int firstLineEnd = content.indexOf('\n');
                            if (firstLineEnd == -1) {
                                return content + "\n" + PLUGIN_ROW;
                            }
                            return content.substring(0, firstLineEnd + 1) + PLUGIN_ROW + "\n" + content.substring(firstLineEnd + 1);
                        }
                    );
                    print("Конфиг успешно обновлён");
                    publish(85);
                    return true;
                } catch (Exception e) {
                    errorMessage = "Ошибка при обновлении конфига: " + e.getMessage();
                    publish(-1);
                    return false;
                }
            }

            private boolean saveZipFile() {
                print("Сохранение архива...");
                try {
                    zip.save();
                    print("Архив успешно сохранён!");
                    publish(95);
                    return true;
                } catch (Exception e) {
                    errorMessage = "Ошибка при сохранении архива: " + e.getMessage();
                    publish(-1);
                    return false;
                }
            }

            private boolean verifyChanges(List<PluginFile> pluginFiles) {
                print("Проверка изменений в архиве...");
                try {
                    Zip zipBefore = new Zip(zip.path);
                    Set<String> initialFiles = zipBefore.files.keySet();
                    Set<String> pluginFileNames = pluginFiles.stream().map(pf -> pf.inZipName).collect(Collectors.toSet());
                    Zip zipAfter = new Zip(ZipUtils.update(zip.path));
                    Set<String> resultFiles = zipAfter.files.keySet();

                    boolean allPluginFilesPresent = pluginFileNames.stream().allMatch(resultFiles::remove);
                    boolean onlyPluginFilesAdded = initialFiles.equals(resultFiles);

                    if (allPluginFilesPresent && onlyPluginFilesAdded) {
                        print("Проверка успешна: файлы плагина добавлены в архив.");
                        return true;
                    }

                    print("Внимание: не все файлы плагина найдены в архиве или список файлов не изменился!");
                    if (!allPluginFilesPresent) {
                        errorMessage = "Отсутствуют файлы плагина: " +
                                pluginFileNames.stream().filter(f -> !resultFiles.contains(f)).collect(Collectors.joining(", "));
                        publish(-1);
                    }
                    if (!onlyPluginFilesAdded) {
                        errorMessage = ListUtils.diff(initialFiles, "before", resultFiles, "after");
                        publish(-1);
                    }
                    return false;
                } catch (Exception e) {
                    errorMessage = "Ошибка при проверке изменений архива: " + e.getMessage();
                    publish(-1);
                    return false;
                }
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                for (Integer progress : chunks) {
                    if (progress == -1) {
                        // Ошибка валидации
                        progressBar.setVisible(false);
                        setStatusError(errorMessage != null ? errorMessage : "Выбран неподходящий файл");
                        selectFileButton.setEnabled(true);
                        selectFileButton.setVisible(true);
                        printError(errorMessage != null ? errorMessage : "Выбран неподходящий файл");
                        return;
                    } else if (progress == 10) {
                        statusLabel.setText("Файл получен");
                        progressBar.setValue(progress);
                    } else if (progress <= 90) {
                        statusLabel.setText("Обработка файла...");
                        progressBar.setValue(progress);
                    } else {
                        statusLabel.setText("Создание архива...");
                        progressBar.setValue(progress);
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // выбросит исключение, если оно было в doInBackground
                    if (progressBar.getValue() == 100) {
                        // Успешное завершение
                        progressBar.setVisible(false);
                        statusLabel.setText("Создан файл: " + ZipUtils.update(zip.path).getFileName());
                        successIcon.setVisible(true);
                        print("Создан файл: " + ZipUtils.update(zip.path));
                        print("Обработка завершена успешно!");
                    }
                } catch (Exception ex) {
                    setStatusError("при обработке: " + ex.getMessage());
                    printError("Ошибка при обработке: " + ex.getMessage());
                    selectFileButton.setEnabled(true);
                    selectFileButton.setVisible(true);
                    return;
                }
                selectFileButton.setEnabled(true);
            }
        };

        worker.execute();
    }

    // Создание простой иконки галочки
    private ImageIcon createCheckIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем зеленый круг
        g2d.setColor(new Color(76, 175, 80));
        g2d.fillOval(2, 2, size-4, size-4);

        // Рисуем белую галочку
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Координаты галочки
        int[] xPoints = {size/4, size*2/5, size*3/4};
        int[] yPoints = {size/2, size*2/3, size/3};

        g2d.drawPolyline(xPoints, yPoints, 3);

        g2d.dispose();
        return new ImageIcon(image);
    }

}
