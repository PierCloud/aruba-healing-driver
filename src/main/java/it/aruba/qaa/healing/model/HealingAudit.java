package it.aruba.qaa.healing.model;

import it.aruba.qaa.healing.config.HealingMode;

import java.time.Instant;
import java.util.List;

public record HealingAudit(
        Instant timestamp,
        String testName,
        String pageName,
        String url,
        HealingMode mode,
        LocatorPayload failedLocator,
        List<HealingCandidate> candidates,
        HealingCandidate selectedCandidate,
        HealingResult result,
        String exception
) {
}
