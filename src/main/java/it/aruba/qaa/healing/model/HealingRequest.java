package it.aruba.qaa.healing.model;

public record HealingRequest(
        String testName,
        String pageName,
        String url,
        LocatorPayload failedLocator,
        String domSnapshot,
        String screenshotBase64,
        String exception
) {
}
