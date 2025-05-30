package at.allure.upgrade.utils;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MathUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\D+([\\d.]+)\\.\\D+");

    public static Optional<String> versionFrom(String rawString) {
        Matcher version = VERSION_PATTERN.matcher(rawString);
        if (version.find())
            return Optional.of(version.group());
        return Optional.empty();
    }

    public static Comparator<String> compareVersions() {
        return (version1, version2) -> {
            // Разбиваем версии на части
            String[] parts1 = version1.split("\\.");
            String[] parts2 = version2.split("\\.");

            // Находим максимальную длину массива
            int maxLength = Math.max(parts1.length, parts2.length);

            for (int i = 0; i < maxLength; i++) {
                // Получаем часть версии, если она есть, иначе считаем её равной 0
                int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

                // Сравниваем части
                if (part1 > part2) {
                    return 1; // version1 больше
                } else if (part1 < part2) {
                    return -1; // version1 меньше
                }
            }

            // Если все части равны
            return 0;
        };
    }
}
