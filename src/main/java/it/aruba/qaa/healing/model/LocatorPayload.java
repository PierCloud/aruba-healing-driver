package it.aruba.qaa.healing.model;

import org.openqa.selenium.By;

public record LocatorPayload(String strategy, String value) {

    public static LocatorPayload from(By locator) {
        String raw = locator.toString();
        if (raw.startsWith("By.cssSelector: ")) {
            return new LocatorPayload("cssSelector", raw.substring("By.cssSelector: ".length()));
        }
        if (raw.startsWith("By.xpath: ")) {
            return new LocatorPayload("xpath", raw.substring("By.xpath: ".length()));
        }
        if (raw.startsWith("By.id: ")) {
            return new LocatorPayload("id", raw.substring("By.id: ".length()));
        }
        if (raw.startsWith("By.name: ")) {
            return new LocatorPayload("name", raw.substring("By.name: ".length()));
        }
        if (raw.startsWith("By.className: ")) {
            return new LocatorPayload("className", raw.substring("By.className: ".length()));
        }
        if (raw.startsWith("By.tagName: ")) {
            return new LocatorPayload("tagName", raw.substring("By.tagName: ".length()));
        }
        if (raw.startsWith("By.linkText: ")) {
            return new LocatorPayload("linkText", raw.substring("By.linkText: ".length()));
        }
        if (raw.startsWith("By.partialLinkText: ")) {
            return new LocatorPayload("partialLinkText", raw.substring("By.partialLinkText: ".length()));
        }
        return new LocatorPayload("unknown", raw);
    }

    public By toBy() {
        return switch (strategy) {
            case "cssSelector" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "className" -> By.className(value);
            case "tagName" -> By.tagName(value);
            case "linkText" -> By.linkText(value);
            case "partialLinkText" -> By.partialLinkText(value);
            default -> throw new IllegalArgumentException("Unsupported locator strategy: " + strategy);
        };
    }
}
