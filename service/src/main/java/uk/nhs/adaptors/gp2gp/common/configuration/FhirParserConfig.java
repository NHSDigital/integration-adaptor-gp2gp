package uk.nhs.adaptors.gp2gp.common.configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirParserConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forDstu3();
    }

    @Bean
    public IParser fhirJsonParser() {
        return FhirContext.forDstu3().newJsonParser();
    }

}
