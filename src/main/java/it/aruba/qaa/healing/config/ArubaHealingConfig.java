package it.aruba.qaa.healing.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record ArubaHealingConfig(
        HealingMode mode,
        URI healerUrl,
        double minConfidence,
        Set<String> allowedStrategies,
        Path reportDir,
        boolean screenshotEnabled,
        Duration timeout
) {

    public static ArubaHealingConfig fromEnv() {
        return new ArubaHealingConfig(
                enumValue(env("ARUBA_HEALING_MODE", "SUGGEST_ONLY"), HealingMode.class),
                URI.create(env("ARUBA_HEALER_URL", "http://healer:8025")),
                Double.parseDouble(env("ARUBA_HEALING_MIN_CONFIDENCE", "0.90")),
                strategies(env("ARUBA_HEALING_ALLOWED_STRATEGIES", "cssSelector,xpath,id,name")),
                Path.of(env("ARUBA_HEALING_REPORT_DIR", "target/healing-report")),
                Boolean.parseBoolean(env("ARUBA_HEALING_SCREENSHOT", "true")),
                Duration.ofMillis(Long.parseLong(env("ARUBA_HEALING_TIMEOUT_MS", "3000")))
        );
    }

    public URI locatorEndpoint() {
        return healerUrl.resolve("/heal/locator");
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Set<String> strategies(String rawValue) {
        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static <E extends Enum<E>> E enumValue(String value, Class<E> enumType) {
        return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
    }
}
