package uk.nhs.adaptors.mockmhsservice.common;

public class MockMHSException extends RuntimeException {
    public MockMHSException(String message) {
        super(message);
    }

    public MockMHSException(String message, Throwable cause) {
        super(message, cause);
    }
}
