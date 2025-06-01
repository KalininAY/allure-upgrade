package at.allure.upgrade.core;

import at.allure.upgrade.utils.AllureUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ZipProcessorWindow extends JFrame {
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton selectFileButton;
    private JLabel successIcon;
    private File selectedFile;

    {
        init();
    }

    public void init() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setTitle("Let's upgrade your allure");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setResizable(false);

        selectFileButton = new JButton("Выбрать ZIP архив с Allure");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        statusLabel = new JLabel("Выберите ZIP архив для обработки", JLabel.CENTER);

        // Создаем простую иконку галочки
        successIcon = new JLabel(createCheckIcon());
        successIcon.setVisible(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(selectFileButton);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(statusLabel, BorderLayout.NORTH);
        centerPanel.add(progressBar, BorderLayout.CENTER);
        centerPanel.add(successIcon, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Добавляем отступы
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void setupEventHandlers() {
        selectFileButton.addActionListener(e -> selectZipFile());
    }

    private void selectZipFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("ZIP архивы", "zip"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            processZipFile();
        }
    }

    private void print(String message) {
        // TODO: вывести в "консоль"
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
            @Override
            protected Void doInBackground() throws Exception {
                print("Начало обработки файла " + selectedFile.getAbsolutePath() + "...");

                Zip zip;
                try {
                    zip = new Zip(selectedFile.toPath());
                } catch (Exception e) { // например, выбран не архив
                    showError("Не удалось открыть архив, " + e.getMessage());
                    init();
                    return null;
                }

                // Файл получен (5%)
                publish(5);
                print("Файл распознан как архив");

                if (AllureUtils.isAllureZip(zip)) {
                    String allureVersion = AllureUtils.parseAllureVersion(zip);
                    print("В архиве найден Allure " + allureVersion + ", начинается модификация...");

                } else {
                    showError("Выбранный архив не содержит аллюр");
                    init();
                    return null;
                }

                // Исходный Allure получен (10%)
                publish(10);

                // TODO: add jar
                // TODO: add static
                Path pluginDir = Paths.get("src", "main", "resources", "resultiks-plugin");
                zip.addDirectory(pluginDir);

                // TODO: edit config
                String pluginRow = "  - resultiks-plugin";
                zip.update(
                        Paths.get("config", "allure.yml"),
                        content -> content.contains(pluginRow) ? content : content + "\n" + pluginRow
                );

                // TODO: edit other plugins



                Thread.sleep(500);

                // Process (10% - 90%)
                for (int i = 10; i <= 90; i += 5) {
                    Thread.sleep(100); // Имитация работы
                    publish(i);
                }

                // Zip (90% - 100%)
                for (int i = 90; i <= 100; i += 2) {
                    Thread.sleep(50); // Имитация работы
                    publish(i);
                }

                return null;
            }

            private void showError(String error) {
                print(error);
                JOptionPane.showMessageDialog(ZipProcessorWindow.this, error, "", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                for (Integer progress : chunks) {
                    if (progress == -1) {
                        // Ошибка валидации
                        progressBar.setVisible(false);
                        statusLabel.setText("Выбран неподходящий файл");
                        selectFileButton.setEnabled(true);
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
                if (progressBar.getValue() == 100) {
                    // Успешное завершение
                    progressBar.setVisible(false);
                    statusLabel.setText("Обработка завершена успешно!");
                    successIcon.setVisible(true);
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
