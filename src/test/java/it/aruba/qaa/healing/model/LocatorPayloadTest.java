package it.aruba.qaa.healing.model;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocatorPayloadTest {

    @Test
    void convertsCssSelector() {
        LocatorPayload payload = LocatorPayload.from(By.cssSelector("button[data-testid='pay']"));

        assertEquals("cssSelector", payload.strategy());
        assertEquals("button[data-testid='pay']", payload.value());
        assertEquals(By.cssSelector("button[data-testid='pay']"), payload.toBy());
    }

    @Test
    void convertsXpath() {
        LocatorPayload payload = LocatorPayload.from(By.xpath("//button[@type='submit']"));

        assertEquals("xpath", payload.strategy());
        assertEquals("//button[@type='submit']", payload.value());
        assertEquals(By.xpath("//button[@type='submit']"), payload.toBy());
    }

    @Test
    void convertsIdAndName() {
        assertEquals(By.id("email"), new LocatorPayload("id", "email").toBy());
        assertEquals(By.name("password"), new LocatorPayload("name", "password").toBy());
    }
}
