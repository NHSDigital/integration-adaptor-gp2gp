package uk.nhs.adaptors.gp2gp;

public class TestContainerUtils {

    public static final String TRUE = "true";
    public static final String DISABLE_TEST_CONTAINERS = "DISABLE_TEST_CONTAINERS";

    public static boolean isTestContainersEnabled() {
        return !TRUE.equalsIgnoreCase(System.getenv(DISABLE_TEST_CONTAINERS));
    }
}
