package uk.nhs.adaptors.gp2gp.common.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FhirParseServiceTest {

    private ArgumentCaptor<StrictErrorHandler> captor = ArgumentCaptor.forClass(StrictErrorHandler.class);

    @Test
    void fhirParseServiceInitializedWithStrictErrorHandlerAndNewJsonParserTest() {

        FhirContext fhirContext = mock(FhirContext.class);
        IParser parser = mock(IParser.class);
        when(fhirContext.newJsonParser()).thenReturn(parser);

        FhirParseService service = new FhirParseService(fhirContext);

        verify(fhirContext).setParserErrorHandler(captor.capture());
        StrictErrorHandler strictErrorHandler = captor.getValue();
        assertNotNull(strictErrorHandler, "StrictErrorHandler should not be null");
        verify(fhirContext, times(2)).newJsonParser();
    }
}