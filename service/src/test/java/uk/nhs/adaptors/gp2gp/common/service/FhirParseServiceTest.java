package uk.nhs.adaptors.gp2gp.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.nhs.adaptors.gp2gp.common.configuration.ObjectMapperBean;
import uk.nhs.adaptors.gp2gp.ehr.EhrResendController;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
class FhirParseServiceTest {

    public static final String INVALID_IDENTIFIER_VALUE = "INVALID_IDENTIFIER_VALUE";
    private static final String OPERATION_OUTCOME_URL = "https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-OperationOutcome-1";
    private OperationOutcome operationOutcome;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ObjectMapperBean objectMapperBean = new ObjectMapperBean();
        objectMapper = objectMapperBean.objectMapper(new Jackson2ObjectMapperBuilder());

        var details = getCodeableConcept(INVALID_IDENTIFIER_VALUE);
        var diagnostics = "Provide a conversationId that exists and retry the operation";
        operationOutcome = EhrResendController.createOperationOutcome(OperationOutcome.IssueType.VALUE,
                                                      OperationOutcome.IssueSeverity.ERROR,
                                                      details,
                                                      diagnostics);
    }

    @Test
    void parseOperationOutcomeTest() throws JsonProcessingException {
        FhirParseService fhirParseService = new FhirParseService();

        String convertedToJsonOperationOutcome = fhirParseService.encodeToJson(operationOutcome);

        JsonNode rootNode = objectMapper.readTree(convertedToJsonOperationOutcome);
        String code =
            rootNode.path("issue").get(0).path("details").path("coding").get(0).path("code").asText();
        String operationOutcomeUrl = rootNode.path("meta").path("profile").get(0).asText();

        assertEquals(INVALID_IDENTIFIER_VALUE, code);
        assertEquals(OPERATION_OUTCOME_URL, operationOutcomeUrl);
    }

    private static CodeableConcept getCodeableConcept(String codeableConceptCode) {
        var details = new CodeableConcept();
        var codeableConceptCoding = new Coding();
        codeableConceptCoding.setSystem("http://fhir.nhs.net/ValueSet/gpconnect-error-or-warning-code-1");
        codeableConceptCoding.setCode(codeableConceptCode);
        details.setCoding(List.of(codeableConceptCoding));
        return details;
    }
}