package it.aruba.qaa.healing.model;

import java.util.List;

public record HealingResponse(
        String status,
        List<HealingCandidate> candidates
) {
    public static HealingResponse empty() {
        return new HealingResponse("empty", List.of());
    }
}
