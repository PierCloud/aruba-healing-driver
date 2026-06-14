package it.aruba.qaa.healing;

import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.config.HealingMode;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class Support {

    private Support() {
    }

    public static ArubaHealingConfig config(HealingMode mode, String allowedStrategies, double minConfidence) {
        return new ArubaHealingConfig(
                mode,
                URI.create("http://localhost:8025"),
                minConfidence,
                Arrays.stream(allowedStrategies.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet()),
                Path.of("target/test-healing-report"),
                true,
                true,
                true,
                Duration.ofMillis(500)
        );
    }
}
