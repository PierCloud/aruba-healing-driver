package it.aruba.qaa.healing.policy;

import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.config.HealingMode;
import it.aruba.qaa.healing.model.HealingCandidate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HealingPolicy {

    private final ArubaHealingConfig config;

    public HealingPolicy(ArubaHealingConfig config) {
        this.config = config;
    }

    public boolean canCallFallback() {
        return config.mode() != HealingMode.OFF;
    }

    public boolean canUseRuntimeHealing() {
        return config.mode() == HealingMode.ASSISTED_HEAL || config.mode() == HealingMode.STRICT_HEAL;
    }

    public Optional<HealingCandidate> selectCandidate(List<HealingCandidate> candidates) {
        return candidates.stream()
                .filter(this::isAllowed)
                .max(Comparator.comparingDouble(HealingCandidate::confidence));
    }

    public boolean isAllowed(HealingCandidate candidate) {
        String strategy = candidate.strategy().toLowerCase(Locale.ROOT);
        return candidate.confidence() >= config.minConfidence()
                && config.allowedStrategies().contains(strategy);
    }
}
