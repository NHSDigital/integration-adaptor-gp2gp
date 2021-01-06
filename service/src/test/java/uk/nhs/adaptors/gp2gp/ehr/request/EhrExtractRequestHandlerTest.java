package uk.nhs.adaptors.gp2gp.ehr.request;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.nhs.adaptors.gp2gp.ResourceHelper;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.MissingValueException;
import uk.nhs.adaptors.gp2gp.ehr.SpineInteraction;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EhrExtractRequestHandlerTest {

    @Mock
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Mock
    private TimestampService timestampService;

    private EhrExtractRequestHandler ehrExtractRequestHandler;

    @BeforeEach
    public void before() {
        ehrExtractRequestHandler = new EhrExtractRequestHandler(ehrExtractStatusRepository, new XPathService(), timestampService);
    }

    @Test
    public void When_ValidEhrRequestReceived_Expect_EhrExtractStatusIsCreated() {
        Document soapHeader = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document soapBody = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");
        Instant now = Instant.now();
        when(timestampService.now()).thenReturn(now);

        ehrExtractRequestHandler.handle(soapHeader, soapBody);

        EhrExtractStatus expected = createEhrExtractStatusMatchXmlFixture(now);
        verify(ehrExtractStatusRepository).save(expected);
        // TODO: tasks created
    }

    @Test
    @SneakyThrows
    public void When_DuplicateEhrExtractRequestReceived_Expect_NoTasksAreCreated() {
        Document soapHeader = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document soapBody = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");
        Instant now = Instant.now();
        when(timestampService.now()).thenReturn(now);
        EhrExtractStatus expected = createEhrExtractStatusMatchXmlFixture(now);
        when(ehrExtractStatusRepository.save(expected)).thenThrow(mock(DuplicateKeyException.class));

        ehrExtractRequestHandler.handle(soapHeader, soapBody);

        verify(ehrExtractStatusRepository).save(expected);
        // TODO: no tasks created
    }

    private EhrExtractStatus createEhrExtractStatusMatchXmlFixture(Instant timestamp) {
        return new EhrExtractStatus(
            timestamp,
            timestamp,
            "DFF5321C-C6EA-468E-BBC2-B0E48000E071",
            new EhrExtractStatus.EhrRequest("041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC",
                "9692294935",
                "N82668-820670",
                "B86041-822103",
                "200000000205",
                "200000001161",
                "N82668",
                "B86041")
        );
    }

    private static List<String> pathsToBodyValues() {
        return List.of(
                "/RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/id/@root",
                "/RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/recordTarget/patient/id/@extension",
                "/RCMR_IN010000UK05/communicationFunctionSnd/device/id/@extension",
                "/RCMR_IN010000UK05/communicationFunctionRcv/device/id/@extension",
                "/RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/author/AgentOrgSDS/agentOrganizationSDS/id/@extension",
                "/RCMR_IN010000UK05/ControlActEvent/subject/EhrRequest/destination/AgentOrgSDS/agentOrganizationSDS/id/@extension"
        );
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("pathsToBodyValues")
    public void When_RequiredElementMissingFromBody_Expect_HandlerThrowsException(String xpath) {
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document body = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");

        removeAttributeElement(xpath, body);

        assertThatExceptionOfType(MissingValueException.class)
            .isThrownBy(() -> ehrExtractRequestHandler.handle(header, body))
            .withMessageContaining(xpath)
            .withMessageContaining(SpineInteraction.EHR_EXTRACT_REQUEST.getInteractionId());
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("pathsToBodyValues")
    public void When_RequiredValueIsBlankInBody_Expect_HandlerThrowsException(String xpath) {
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document body = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");

        clearAttribute(xpath, body);

        assertThatExceptionOfType(MissingValueException.class)
            .isThrownBy(() -> ehrExtractRequestHandler.handle(header, body))
            .withMessageContaining(xpath)
            .withMessageContaining(SpineInteraction.EHR_EXTRACT_REQUEST.getInteractionId());
    }

    private static List<String> pathsToHeaderValues() {
        return List.of(
            "/Envelope/Header/MessageHeader/From/PartyId",
            "/Envelope/Header/MessageHeader/To/PartyId"
        );
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("pathsToHeaderValues")
    public void When_RequiredElementMissingFromHeader_Expect_HandlerThrowsException(String xpath) {
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document body = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");

        removeElement(xpath, header);

        assertThatExceptionOfType(MissingValueException.class)
            .isThrownBy(() -> ehrExtractRequestHandler.handle(header, body))
            .withMessageContaining(xpath)
            .withMessageContaining(SpineInteraction.EHR_EXTRACT_REQUEST.getInteractionId());
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("pathsToHeaderValues")
    public void When_RequiredValueIsBlankInHeader_Expect_HandlerThrowsException(String xpath) {
        Document header = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document body = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");

        clearElement(xpath, header);

        assertThatExceptionOfType(MissingValueException.class)
            .isThrownBy(() -> ehrExtractRequestHandler.handle(header, body))
            .withMessageContaining(xpath)
            .withMessageContaining(SpineInteraction.EHR_EXTRACT_REQUEST.getInteractionId());
    }

    @SneakyThrows
    private static void clearAttribute(String xpath, Document xml) {
        XPathExpression xPathExpression = XPathFactory.newInstance()
            .newXPath()
            .compile(xpath);
        Attr attr = (Attr) xPathExpression.evaluate(xml, XPathConstants.NODE);
        Element owner = attr.getOwnerElement();
        owner.setAttribute(attr.getName(), "");
    }

    @SneakyThrows
    private static void removeAttributeElement(String xpath, Document xml) {
        XPathExpression xPathExpression = XPathFactory.newInstance()
            .newXPath()
            .compile(xpath);
        Attr attr = (Attr) xPathExpression.evaluate(xml, XPathConstants.NODE);
        Element owner = attr.getOwnerElement();
        owner.getParentNode().removeChild(owner);
    }

    @SneakyThrows
    private static void removeElement(String xpath, Document xml) {
        XPathExpression xPathExpression = XPathFactory.newInstance()
            .newXPath()
            .compile(xpath);
        Node node = (Node) xPathExpression.evaluate(xml, XPathConstants.NODE);
        node.getParentNode().removeChild(node);
    }

    @SneakyThrows
    private static void clearElement(String xpath, Document xml) {
        XPathExpression xPathExpression = XPathFactory.newInstance()
            .newXPath()
            .compile(xpath);
        Node node = (Node) xPathExpression.evaluate(xml, XPathConstants.NODE);
        node.setTextContent("");
    }

}
