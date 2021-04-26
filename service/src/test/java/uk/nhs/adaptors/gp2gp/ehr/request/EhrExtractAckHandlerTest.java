package uk.nhs.adaptors.gp2gp.ehr.request;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.SAXException;

import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.mhs.InvalidInboundMessageException;

@ExtendWith(MockitoExtension.class)
public class EhrExtractAckHandlerTest {

    @Spy
    private XPathService xPathService;

    @InjectMocks
    private EhrExtractAckHandler ehrExtractAckHandler;


    @Test
    public void When_HandleUnsupportedAckTypeCode_Expect_ExceptionThrown() throws SAXException {
        var document = new XPathService().parseDocumentFromXml("<MCCI_IN010000UK13><acknowledgement typeCode=\"CE\"/></MCCI_IN010000UK13>");

        assertThatExceptionOfType(InvalidInboundMessageException.class)
            .isThrownBy(() -> ehrExtractAckHandler.handle("123", document))
            .withMessage("Unsupported //MCCI_IN010000UK13/acknowledgement/@typeCode: CE");
    }
}
