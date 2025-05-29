package at;

import java.util.Optional;

public abstract class AllureUtils {

    public static boolean isAllureZip(Zip zip) {
        return zip.files.containsKey("bin/allure");
    }

    /**
     * Максимальная версия из lib/allure-*, иначе "Unrecognized version"
     */
    public static String parseAllureVersion(Zip zip) {
        return zip.files.keySet().stream()
                .filter(name -> name.startsWith("lib/allure-"))
                .map(MathUtils::versionFrom)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(MathUtils.compareVersions())
                .orElse("Unrecognized version");
    }

}
