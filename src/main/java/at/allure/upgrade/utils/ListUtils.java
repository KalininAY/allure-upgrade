package at.allure.upgrade.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class ListUtils {

    public static String diff(Set<String> strings1, String alias1, Set<String> strings2, String alias2) {
        Set<String> strings0 = new HashSet<>(strings1);
        strings1.removeAll(strings2);
        strings2.removeAll(strings0);
        return new HashMap<String, Set<String>>() {{
            if (!strings1.isEmpty())
                put(alias1, strings1);
            if (!strings2.isEmpty())
                put(alias2, strings2);
        }}.toString();
    }
}
