package uk.nhs.adaptors.gp2gp;

public class TestContainerUtils {
    public static boolean isTestContainersEnabled() {
        return !"true".equalsIgnoreCase(System.getenv("DISABLE_TEST_CONTAINERS"));
    }
}
