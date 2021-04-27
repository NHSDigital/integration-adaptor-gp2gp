package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;

@ExtendWith(MockitoExtension.class)
public class EhrExtractAckHandlerTest {

    private static final String ACK_TYPE_CODE_XPATH = "//MCCI_IN010000UK13/acknowledgement/@typeCode";

    @Mock
    private XPathService xPathService;

    @InjectMocks
    private EhrExtractAckHandler ehrExtractAckHandler;

    @Test
    public void When_HandleUnsupportedAckTypeCode_Expect_ExceptionThrown() {
        var document = mock(Document.class);
        when(xPathService.getNodeValue(document, ACK_TYPE_CODE_XPATH)).thenReturn("CE");

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> ehrExtractAckHandler.handle("123", document))
            .withMessage("Unsupported //MCCI_IN010000UK13/acknowledgement/@typeCode: CE");
    }
}
