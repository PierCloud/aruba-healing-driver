package it.aruba.qaa.healing.model;

public record HealingCandidate(
        String strategy,
        String value,
        double confidence,
        String reason
) {
    public LocatorPayload locator() {
        return new LocatorPayload(strategy, value);
    }
}
