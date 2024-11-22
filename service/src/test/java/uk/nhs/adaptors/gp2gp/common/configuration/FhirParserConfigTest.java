package uk.nhs.adaptors.gp2gp.common.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FhirParserConfigTest {

    FhirParserConfig fhirParserConfig;

    @BeforeEach
    void setUp() {
        fhirParserConfig = new FhirParserConfig();
    }

    @Test
    void shouldCreateValidConfig() {
        assertNotNull(fhirParserConfig.fhirContext());
    }

}