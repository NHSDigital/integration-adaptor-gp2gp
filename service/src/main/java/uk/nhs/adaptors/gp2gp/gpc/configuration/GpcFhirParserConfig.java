package uk.nhs.adaptors.gp2gp.gpc.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Configuration
public class GpcFhirParserConfig {
    @Bean
    public IParser fhirJsonParser() {
        return FhirContext.forDstu3().newJsonParser();
    }
}