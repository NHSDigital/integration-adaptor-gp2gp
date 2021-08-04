package uk.nhs.adaptors.gp2gp.e2e;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AwaitHelper {

    protected static final int WAIT_FOR_IN_SECONDS = 300;
    protected static final int POLL_INTERVAL_MS = 300;
    protected static final int POLL_DELAY_MS = 30;

    public static <T> T waitFor(Supplier<T> supplier) {
        var dataToReturn = new AtomicReference<T>();
        await()
                .atMost(WAIT_FOR_IN_SECONDS, SECONDS)
                .pollInterval(POLL_INTERVAL_MS, MILLISECONDS)
                .pollDelay(POLL_DELAY_MS, MILLISECONDS)
                .until(() -> {
                    var data = supplier.get();
                    if (data != null) {
                        dataToReturn.set(data);
                        return true;
                    }
                    return false;
                });

        return dataToReturn.get();
    }
}
