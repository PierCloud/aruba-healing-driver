package it.aruba.qaa.healing.context;

@FunctionalInterface
public interface TestContextProvider {
    TestContext current();

    static TestContextProvider empty() {
        return TestContext::empty;
    }
}
