package uk.nhs.adaptors.gp2gp.common.utils;

import lombok.SneakyThrows;

import jakarta.jms.Message;
import java.time.Instant;

public class Jms {
    @SneakyThrows
    public static Instant getJmsMessageTimestamp(Message message) {
        return Instant.ofEpochMilli(message.getJMSTimestamp());
    }
}
