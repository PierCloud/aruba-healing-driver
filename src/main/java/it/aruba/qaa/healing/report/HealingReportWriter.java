package it.aruba.qaa.healing.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.aruba.qaa.healing.model.ElementBox;
import it.aruba.qaa.healing.model.HealingAudit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

public final class HealingReportWriter {

    private final Path reportDir;
    private final ObjectMapper objectMapper;

    public HealingReportWriter(Path reportDir) {
        this.reportDir = reportDir;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void write(HealingAudit audit) {
        write(audit, null, null);
    }

    public void write(HealingAudit audit, byte[] screenshot, ElementBox elementBox) {
        try {
            Files.createDirectories(reportDir);
            String baseName = baseName(audit);
            Path jsonReport = reportDir.resolve(baseName + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonReport.toFile(), audit);

            if (screenshot != null && elementBox != null) {
                String screenshotFile = baseName + ".png";
                Files.write(reportDir.resolve(screenshotFile), screenshot, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(reportDir.resolve(baseName + ".html"), html(audit, screenshotFile, elementBox), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    private static String baseName(HealingAudit audit) {
        String test = audit.testName() == null || audit.testName().isBlank() ? "unknown-test" : audit.testName();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(audit.timestamp())
                .replace(":", "")
                .replace(".", "-");
        String safeTest = test.replaceAll("[^a-zA-Z0-9._-]", "-");
        return safeTest + "-" + timestamp;
    }

    private static String html(HealingAudit audit, String screenshotFile, ElementBox box) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <title>Healing report</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 24px; color: #1f2937; }
                        .stage { position: relative; display: inline-block; border: 1px solid #d1d5db; }
                        .stage img { display: block; max-width: 100%%; }
                        .box { position: absolute; border: 3px solid #dc2626; box-sizing: border-box; pointer-events: none; }
                        dl { display: grid; grid-template-columns: 180px 1fr; gap: 8px 16px; }
                        dt { font-weight: 700; }
                        dd { margin: 0; font-family: Consolas, monospace; }
                    </style>
                </head>
                <body>
                    <h1>Healing report</h1>
                    <dl>
                        <dt>Result</dt><dd>%s</dd>
                        <dt>Test</dt><dd>%s</dd>
                        <dt>Page</dt><dd>%s</dd>
                        <dt>Failed locator</dt><dd>%s=%s</dd>
                        <dt>Selected locator</dt><dd>%s</dd>
                    </dl>
                    <div class="stage">
                        <img src="%s" alt="Healing screenshot">
                        <div class="box" style="left:%dpx;top:%dpx;width:%dpx;height:%dpx"></div>
                    </div>
                </body>
                </html>
                """.formatted(
                audit.result(),
                escape(audit.testName()),
                escape(audit.pageName()),
                escape(audit.failedLocator().strategy()),
                escape(audit.failedLocator().value()),
                audit.selectedCandidate() == null ? "" : escape(audit.selectedCandidate().strategy() + "=" + audit.selectedCandidate().value()),
                escape(screenshotFile),
                box.x(),
                box.y(),
                box.width(),
                box.height()
        );
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
