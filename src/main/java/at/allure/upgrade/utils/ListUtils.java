package at.allure.upgrade.utils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public abstract class ListUtils {

    /**
     * Возвращает строку с различиями (уникальные пути в каждом из сетов)
     */
    public static String diff(Set<Path> paths1, String alias1, Set<Path> paths2, String alias2) {
        Set<Path> strings0 = new HashSet<>(paths1);
        paths1.removeAll(paths2);
        paths2.removeAll(strings0);
        return new LinkedHashMap<String, Set<Path>>() {{
            if (!paths1.isEmpty())
                put(alias1, paths1);
            if (!paths2.isEmpty())
                put(alias2, paths2);
        }}.toString();
    }
}
