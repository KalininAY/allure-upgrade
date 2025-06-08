package at.allure.upgrade;

import at.allure.upgrade.core.ZipProcessorWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new ZipProcessorWindow().setVisible(true);
        });
    }
}