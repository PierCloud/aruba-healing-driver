package it.aruba.qaa.healing.policy;

import it.aruba.qaa.healing.Support;
import it.aruba.qaa.healing.config.HealingMode;
import it.aruba.qaa.healing.model.HealingCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealingPolicyTest {

    @Test
    void offModeDoesNotCallFallback() {
        HealingPolicy policy = new HealingPolicy(Support.config(HealingMode.OFF, "cssSelector,xpath", 0.90));

        assertFalse(policy.canCallFallback());
        assertFalse(policy.canUseRuntimeHealing());
    }

    @Test
    void suggestOnlyCallsFallbackWithoutRuntimeHealing() {
        HealingPolicy policy = new HealingPolicy(Support.config(HealingMode.SUGGEST_ONLY, "cssSelector,xpath", 0.90));

        assertTrue(policy.canCallFallback());
        assertFalse(policy.canUseRuntimeHealing());
    }

    @Test
    void selectsHighestAllowedCandidate() {
        HealingPolicy policy = new HealingPolicy(Support.config(HealingMode.ASSISTED_HEAL, "cssSelector,xpath", 0.90));

        HealingCandidate selected = policy.selectCandidate(List.of(
                new HealingCandidate("xpath", "//button[1]", 0.91, "fallback"),
                new HealingCandidate("id", "confirm", 0.99, "not allowed"),
                new HealingCandidate("cssSelector", "button[data-testid='confirm']", 0.95, "stable")
        )).orElseThrow();

        assertEquals("cssSelector", selected.strategy());
        assertEquals("button[data-testid='confirm']", selected.value());
    }

    @Test
    void rejectsLowConfidenceCandidates() {
        HealingPolicy policy = new HealingPolicy(Support.config(HealingMode.STRICT_HEAL, "cssSelector", 0.95));

        assertTrue(policy.selectCandidate(List.of(
                new HealingCandidate("cssSelector", ".confirm", 0.94, "below threshold")
        )).isEmpty());
    }
}
