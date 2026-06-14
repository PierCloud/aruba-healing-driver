package it.aruba.qaa.healing.driver;

import it.aruba.qaa.healing.client.HealerClient;
import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.config.HealingMode;
import it.aruba.qaa.healing.context.TestContext;
import it.aruba.qaa.healing.context.TestContextProvider;
import it.aruba.qaa.healing.model.HealingCandidate;
import it.aruba.qaa.healing.model.HealingResponse;
import it.aruba.qaa.healing.report.HealingReportWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArubaHealingDriverTest {

    @TempDir
    Path reportDir;

    @Test
    void assistedModeUsesAcceptedFallbackCandidate() throws Exception {
        WebElement healedElement = element();
        FakeWebDriver delegate = new FakeWebDriver(By.cssSelector("button[data-testid='confirm']"), healedElement);
        HealerClient healerClient = request -> new HealingResponse(
                "candidate_found",
                List.of(new HealingCandidate("cssSelector", "button[data-testid='confirm']", 0.96, "stable test id"))
        );
        WebDriver driver = ArubaHealingDriver.wrap(
                delegate,
                config(HealingMode.ASSISTED_HEAL),
                healerClient,
                new HealingReportWriter(reportDir),
                context()
        );

        WebElement found = driver.findElement(By.cssSelector("#confirm"));

        assertSame(healedElement, found);
        assertEquals(2, delegate.findCalls);
        assertEquals(3, Files.list(reportDir).count());
    }

    @Test
    void suggestOnlyKeepsOriginalException() {
        WebElement healedElement = element();
        FakeWebDriver delegate = new FakeWebDriver(By.cssSelector("button[data-testid='confirm']"), healedElement);
        HealerClient healerClient = request -> new HealingResponse(
                "candidate_found",
                List.of(new HealingCandidate("cssSelector", "button[data-testid='confirm']", 0.96, "stable test id"))
        );
        WebDriver driver = ArubaHealingDriver.wrap(
                delegate,
                config(HealingMode.SUGGEST_ONLY),
                healerClient,
                new HealingReportWriter(reportDir),
                context()
        );

        assertThrows(NoSuchElementException.class, () -> driver.findElement(By.cssSelector("#confirm")));
        assertEquals(1, delegate.findCalls);
    }

    @Test
    void disabledCustomReportDoesNotWriteArtifacts() throws Exception {
        WebElement healedElement = element();
        FakeWebDriver delegate = new FakeWebDriver(By.cssSelector("button[data-testid='confirm']"), healedElement);
        HealerClient healerClient = request -> new HealingResponse(
                "candidate_found",
                List.of(new HealingCandidate("cssSelector", "button[data-testid='confirm']", 0.96, "stable test id"))
        );
        WebDriver driver = ArubaHealingDriver.wrap(
                delegate,
                config(HealingMode.ASSISTED_HEAL, false, true),
                healerClient,
                new HealingReportWriter(reportDir),
                context()
        );

        driver.findElement(By.cssSelector("#confirm"));

        assertEquals(0, Files.list(reportDir).count());
    }

    @Test
    void disabledHealeniumUsesBaseDriverAsDelegate() {
        FakeWebDriver baseDriver = new FakeWebDriver(By.cssSelector("button[data-testid='confirm']"), element());

        WebDriver driver = ArubaHealeniumRuntime.create(
                baseDriver,
                config(HealingMode.SUGGEST_ONLY, true, false),
                context()
        );

        assertSame(baseDriver, ((WrapsDriver) driver).getWrappedDriver());
    }

    private ArubaHealingConfig config(HealingMode mode) {
        return config(mode, true, true);
    }

    private ArubaHealingConfig config(HealingMode mode, boolean customReportEnabled, boolean healeniumEnabled) {
        return new ArubaHealingConfig(
                mode,
                URI.create("http://localhost:8025"),
                0.90,
                Set.of("cssselector", "xpath"),
                reportDir,
                true,
                healeniumEnabled,
                customReportEnabled,
                Duration.ofMillis(500)
        );
    }

    private static TestContextProvider context() {
        return () -> new TestContext("CheckoutTest.shouldConfirm", "CheckoutPage");
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(
                ArubaHealingDriverTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isDisplayed", "isEnabled", "isSelected" -> true;
                    case "getText" -> "Confirm";
                    case "getTagName" -> "button";
                    case "getAttribute", "getDomAttribute", "getDomProperty", "getCssValue" -> null;
                    case "getRect" -> new Rectangle(10, 20, 120, 32);
                    case "getLocation", "getSize", "getScreenshotAs" -> null;
                    default -> null;
                }
        );
    }

    private static final class FakeWebDriver implements WebDriver, TakesScreenshot, JavascriptExecutor {

        private final By healedLocator;
        private final WebElement healedElement;
        private int findCalls;

        private FakeWebDriver(By healedLocator, WebElement healedElement) {
            this.healedLocator = healedLocator;
            this.healedElement = healedElement;
        }

        @Override
        public WebElement findElement(By by) {
            findCalls++;
            if (healedLocator.equals(by)) {
                return healedElement;
            }
            throw new NoSuchElementException("missing locator: " + by);
        }

        @Override
        public List<WebElement> findElements(By by) {
            return healedLocator.equals(by) ? List.of(healedElement) : List.of();
        }

        @Override
        public void get(String url) {
        }

        @Override
        public String getCurrentUrl() {
            return "https://app.test/checkout";
        }

        @Override
        public String getTitle() {
            return "Checkout";
        }

        @Override
        public String getPageSource() {
            return "<html><button data-testid='confirm'>Confirm</button></html>";
        }

        @Override
        public void close() {
        }

        @Override
        public void quit() {
        }

        @Override
        public Set<String> getWindowHandles() {
            return Set.of("window");
        }

        @Override
        public String getWindowHandle() {
            return "window";
        }

        @Override
        public TargetLocator switchTo() {
            return null;
        }

        @Override
        public Navigation navigate() {
            return null;
        }

        @Override
        public Options manage() {
            return null;
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) {
            return target.convertFromPngBytes(new byte[]{1, 2, 3});
        }

        @Override
        public Object executeScript(String script, Object... args) {
            return null;
        }

        @Override
        public Object executeAsyncScript(String script, Object... args) {
            return null;
        }
    }
}
