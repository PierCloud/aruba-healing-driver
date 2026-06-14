package it.aruba.qaa.healing.driver;

import it.aruba.qaa.healing.client.ArubaHealerClient;
import it.aruba.qaa.healing.client.HealerClient;
import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.context.TestContext;
import it.aruba.qaa.healing.context.TestContextProvider;
import it.aruba.qaa.healing.model.ElementBox;
import it.aruba.qaa.healing.model.HealingAudit;
import it.aruba.qaa.healing.model.HealingCandidate;
import it.aruba.qaa.healing.model.HealingRequest;
import it.aruba.qaa.healing.model.HealingResponse;
import it.aruba.qaa.healing.model.HealingResult;
import it.aruba.qaa.healing.model.LocatorPayload;
import it.aruba.qaa.healing.policy.HealingPolicy;
import it.aruba.qaa.healing.report.HealingReportWriter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ArubaHealingDriver implements WebDriver, JavascriptExecutor, TakesScreenshot, WrapsDriver {

    private final WebDriver delegate;
    private final ArubaHealingConfig config;
    private final HealerClient healerClient;
    private final HealingPolicy policy;
    private final HealingReportWriter reportWriter;
    private final TestContextProvider contextProvider;

    private ArubaHealingDriver(
            WebDriver delegate,
            ArubaHealingConfig config,
            HealerClient healerClient,
            HealingReportWriter reportWriter,
            TestContextProvider contextProvider
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.config = Objects.requireNonNull(config, "config");
        this.healerClient = Objects.requireNonNull(healerClient, "healerClient");
        this.policy = new HealingPolicy(config);
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider");
    }

    public static WebDriver wrap(WebDriver delegate, ArubaHealingConfig config) {
        return wrap(delegate, config, TestContextProvider.empty());
    }

    public static WebDriver wrap(WebDriver delegate, ArubaHealingConfig config, TestContextProvider contextProvider) {
        return new ArubaHealingDriver(
                delegate,
                config,
                new ArubaHealerClient(config),
                new HealingReportWriter(config.reportDir()),
                contextProvider
        );
    }

    static WebDriver wrap(
            WebDriver delegate,
            ArubaHealingConfig config,
            HealerClient healerClient,
            HealingReportWriter reportWriter,
            TestContextProvider contextProvider
    ) {
        return new ArubaHealingDriver(delegate, config, healerClient, reportWriter, contextProvider);
    }

    @Override
    public WebElement findElement(By by) {
        try {
            return delegate.findElement(by);
        } catch (RuntimeException originalException) {
            if (!policy.canCallFallback()) {
                writeAudit(by, List.of(), null, HealingResult.OFF, originalException);
                throw originalException;
            }
            return fallbackFindElement(by, originalException);
        }
    }

    private WebElement fallbackFindElement(By failedLocator, RuntimeException originalException) {
        HealingRequest request = buildRequest(failedLocator, originalException);
        HealingResponse response = healerClient.heal(request);
        List<HealingCandidate> candidates = response.candidates() == null ? List.of() : response.candidates();

        if (!policy.canUseRuntimeHealing()) {
            writeAudit(failedLocator, candidates, null, HealingResult.SUGGESTED, originalException);
            throw originalException;
        }

        HealingCandidate selected = policy.selectCandidate(candidates).orElse(null);
        if (selected == null) {
            writeAudit(failedLocator, candidates, null, HealingResult.REJECTED, originalException);
            throw originalException;
        }

        try {
            WebElement element = delegate.findElement(selected.locator().toBy());
            writeAudit(failedLocator, candidates, selected, elementBox(element), HealingResult.HEALED, originalException);
            return element;
        } catch (RuntimeException healingException) {
            writeAudit(failedLocator, candidates, selected, HealingResult.NOT_HEALED, originalException);
            throw originalException;
        }
    }

    private HealingRequest buildRequest(By failedLocator, RuntimeException exception) {
        TestContext context = contextProvider.current();
        return new HealingRequest(
                context.testName(),
                context.pageName(),
                safeCurrentUrl(),
                LocatorPayload.from(failedLocator),
                pageSource(),
                screenshotBase64(),
                exception.getClass().getSimpleName() + ": " + exception.getMessage()
        );
    }

    private void writeAudit(
            By failedLocator,
            List<HealingCandidate> candidates,
            HealingCandidate selected,
            HealingResult result,
            RuntimeException exception
    ) {
        writeAudit(failedLocator, candidates, selected, null, result, exception);
    }

    private void writeAudit(
            By failedLocator,
            List<HealingCandidate> candidates,
            HealingCandidate selected,
            ElementBox selectedElementBox,
            HealingResult result,
            RuntimeException exception
    ) {
        if (!config.customReportEnabled()) {
            return;
        }
        TestContext context = contextProvider.current();
        reportWriter.write(new HealingAudit(
                Instant.now(),
                context.testName(),
                context.pageName(),
                safeCurrentUrl(),
                config.mode(),
                LocatorPayload.from(failedLocator),
                candidates,
                selected,
                selectedElementBox,
                result,
                exception.getClass().getSimpleName() + ": " + exception.getMessage()
        ), screenshotBytes(), selectedElementBox);
    }

    private String safeCurrentUrl() {
        try {
            return delegate.getCurrentUrl();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String pageSource() {
        try {
            return delegate.getPageSource();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String screenshotBase64() {
        byte[] bytes = screenshotBytes();
        return bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] screenshotBytes() {
        if (!config.screenshotEnabled() || !(delegate instanceof TakesScreenshot screenshotDriver)) {
            return null;
        }
        try {
            return screenshotDriver.getScreenshotAs(OutputType.BYTES);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ElementBox elementBox(WebElement element) {
        try {
            Rectangle rect = element.getRect();
            return new ElementBox(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public List<WebElement> findElements(By by) {
        return delegate.findElements(by);
    }

    @Override
    public void get(String url) {
        delegate.get(url);
    }

    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getPageSource() {
        return delegate.getPageSource();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void quit() {
        delegate.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return delegate.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return delegate.switchTo();
    }

    @Override
    public Navigation navigate() {
        return delegate.navigate();
    }

    @Override
    public Options manage() {
        return delegate.manage();
    }

    @Override
    public Object executeScript(String script, Object... args) {
        return asJavascriptExecutor().executeScript(script, args);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return asJavascriptExecutor().executeAsyncScript(script, args);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) {
        if (!(delegate instanceof TakesScreenshot screenshotDriver)) {
            throw new UnsupportedOperationException("Wrapped driver does not support screenshots");
        }
        return screenshotDriver.getScreenshotAs(target);
    }

    @Override
    public WebDriver getWrappedDriver() {
        return delegate;
    }

    private JavascriptExecutor asJavascriptExecutor() {
        if (!(delegate instanceof JavascriptExecutor javascriptExecutor)) {
            throw new UnsupportedOperationException("Wrapped driver does not support JavaScript execution");
        }
        return javascriptExecutor;
    }
}
