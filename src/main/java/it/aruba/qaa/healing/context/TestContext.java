package it.aruba.qaa.healing.context;

public record TestContext(String testName, String pageName) {
    public static TestContext empty() {
        return new TestContext(null, null);
    }
}
