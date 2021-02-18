package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class MapperTest {
    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @AfterAll
    public static void deinitialize() {
        TimeZone.setDefault(null);
    }
}
