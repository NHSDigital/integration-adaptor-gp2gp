package uk.nhs.adaptors.gp2gp.common.utils;

import lombok.SneakyThrows;

import javax.jms.Message;
import java.text.SimpleDateFormat;

public class Jms {
    @SneakyThrows
    public static String getJmsMessageTimestamp(Message message) {
        var timestampAsDate = new java.util.Date(message.getJMSTimestamp());
        var dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return dateFormatter.format(timestampAsDate);
    }
}
