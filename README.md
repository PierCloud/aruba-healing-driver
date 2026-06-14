# aruba-healing-driver

Maven library for policy-based locator healing in Selenium frameworks.

## Scope

The library wraps an existing `WebDriver`. The wrapped driver can be a native Selenium driver or a Healenium `SelfHealingDriver`.

Runtime flow:

```text
findElement
  -> delegate driver
  -> fallback healer service
  -> candidate policy
  -> audit artifact
```

Default mode is `SUGGEST_ONLY`. In this mode the fallback service is called and the audit file is written, but the original Selenium exception is preserved.

The test framework remains responsible for driver lifecycle, TestNG/JUnit hooks, browser options, grid configuration and page objects.

Selenium and Jackson are declared with `provided` scope. The consuming e2e framework must provide the approved runtime versions through its own dependency management.

Healenium remains the first healing layer when `ARUBA_HEALENIUM_ENABLED=true`. Native Healenium reports, screenshots, highlighted elements and frontend views are produced by the Healenium runtime stack when its backend/report components are configured in the consuming test environment.

The custom fallback report is controlled by `ARUBA_HEALING_REPORT_ENABLED`. It writes JSON audit files and, when a fallback candidate is applied with a screenshot available, an HTML report with the selected element highlighted.

## Maven

```xml
<dependency>
    <groupId>it.aruba.qaa</groupId>
    <artifactId>aruba-healing-driver</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Driver Setup

```java
import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.driver.ArubaHealeniumRuntime;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

WebDriver baseDriver = new ChromeDriver();

WebDriver driver = ArubaHealeniumRuntime.create(
        baseDriver,
        ArubaHealingConfig.fromEnv()
);
```

Explicit setup:

```java
import com.epam.healenium.SelfHealingDriver;
import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.driver.ArubaHealingDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

WebDriver baseDriver = new ChromeDriver();
WebDriver healeniumDriver = SelfHealingDriver.create(baseDriver);
WebDriver driver = ArubaHealingDriver.wrap(healeniumDriver, ArubaHealingConfig.fromEnv());
```

## Environment

```text
ARUBA_HEALING_MODE=SUGGEST_ONLY
ARUBA_HEALER_URL=http://healer:8025
ARUBA_HEALING_MIN_CONFIDENCE=0.90
ARUBA_HEALING_ALLOWED_STRATEGIES=cssSelector,xpath,id,name
ARUBA_HEALING_REPORT_DIR=target/healing-report
ARUBA_HEALING_SCREENSHOT=true
ARUBA_HEALENIUM_ENABLED=true
ARUBA_HEALING_REPORT_ENABLED=true
ARUBA_HEALING_TIMEOUT_MS=3000
```

Modes:

```text
OFF            no fallback call, original Selenium exception is thrown
SUGGEST_ONLY   fallback call and audit only, original Selenium exception is thrown
ASSISTED_HEAL  applies an accepted fallback candidate at runtime
STRICT_HEAL    applies an accepted fallback candidate at runtime
```

## Healer API

```text
POST /heal/locator
```

Request:

```json
{
  "testName": "CheckoutTest.shouldCompleteCheckout",
  "pageName": "CheckoutPage",
  "url": "https://app.test/checkout",
  "failedLocator": {
    "strategy": "cssSelector",
    "value": "#confirm-button"
  },
  "domSnapshot": "<html>...</html>",
  "screenshotBase64": "...",
  "exception": "NoSuchElementException"
}
```

Response:

```json
{
  "status": "candidate_found",
  "candidates": [
    {
      "strategy": "cssSelector",
      "value": "button[data-testid='confirm-order']",
      "confidence": 0.94,
      "reason": "stable test id"
    }
  ]
}
```

## Audit

Custom fallback reports are written under:

```text
target/healing-report
```

Artifacts:

```text
*.json   audit data
*.html   visual fallback report when a candidate is applied
*.png    screenshot used by the HTML report
```

Each JSON file records the failed locator, runtime mode, fallback candidates, selected candidate, selected element rectangle and final result.

The HTML report is written only when all these conditions are true:

```text
ARUBA_HEALING_REPORT_ENABLED=true
ARUBA_HEALING_SCREENSHOT=true
runtime result is HEALED
the healed WebElement exposes a valid rectangle
the wrapped driver implements TakesScreenshot
```

## Report Switches

```text
ARUBA_HEALENIUM_ENABLED=true
```

Enables the Healenium `SelfHealingDriver` layer. When disabled, the wrapper delegates directly to the provided Selenium driver and only the custom fallback layer can run.

```text
ARUBA_HEALING_REPORT_ENABLED=true
```

Enables the custom fallback report. When disabled, no custom JSON, HTML or screenshot artifact is written.

## Release

Configure Nexus endpoints through Maven properties:

```text
nexus.releases.url=https://nexus.example/repository/maven-releases/
nexus.snapshots.url=https://nexus.example/repository/maven-snapshots/
```

Publish with:

```bash
mvn clean deploy
```
