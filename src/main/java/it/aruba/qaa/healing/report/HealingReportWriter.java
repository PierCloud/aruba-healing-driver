package it.aruba.qaa.healing.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.aruba.qaa.healing.model.HealingAudit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public final class HealingReportWriter {

    private final Path reportDir;
    private final ObjectMapper objectMapper;

    public HealingReportWriter(Path reportDir) {
        this.reportDir = reportDir;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void write(HealingAudit audit) {
        try {
            Files.createDirectories(reportDir);
            Path report = reportDir.resolve(fileName(audit));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), audit);
        } catch (IOException ignored) {
        }
    }

    private static String fileName(HealingAudit audit) {
        String test = audit.testName() == null || audit.testName().isBlank() ? "unknown-test" : audit.testName();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(audit.timestamp())
                .replace(":", "")
                .replace(".", "-");
        String safeTest = test.replaceAll("[^a-zA-Z0-9._-]", "-");
        return safeTest + "-" + timestamp + ".json";
    }
}
