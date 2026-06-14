package it.aruba.qaa.healing.driver;

import com.epam.healenium.SelfHealingDriver;
import it.aruba.qaa.healing.config.ArubaHealingConfig;
import it.aruba.qaa.healing.context.TestContextProvider;
import org.openqa.selenium.WebDriver;

public final class ArubaHealeniumRuntime {

    private ArubaHealeniumRuntime() {
    }

    public static WebDriver create(WebDriver baseDriver) {
        return create(baseDriver, ArubaHealingConfig.fromEnv(), TestContextProvider.empty());
    }

    public static WebDriver create(WebDriver baseDriver, ArubaHealingConfig config) {
        return create(baseDriver, config, TestContextProvider.empty());
    }

    public static WebDriver create(
            WebDriver baseDriver,
            ArubaHealingConfig config,
            TestContextProvider contextProvider
    ) {
        WebDriver healeniumDriver = SelfHealingDriver.create(baseDriver);
        return ArubaHealingDriver.wrap(healeniumDriver, config, contextProvider);
    }
}
