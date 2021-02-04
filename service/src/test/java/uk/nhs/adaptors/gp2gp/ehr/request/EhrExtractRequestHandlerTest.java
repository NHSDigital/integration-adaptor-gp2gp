package uk.nhs.adaptors.gp2gp.ehr.request;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.nhs.adaptors.gp2gp.ResourceHelper;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.service.XPathService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusRepository;
import uk.nhs.adaptors.gp2gp.ehr.exception.MissingValueException;
import uk.nhs.adaptors.gp2gp.ehr.model.SpineInteraction;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcStructuredTaskDefinition;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EhrExtractRequestHandlerTest {
    private static final String REQUEST_ID = "041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC";
    private static final String CONVERSATION_ID = "DFF5321C-C6EA-468E-BBC2-B0E48000E071";
    private static final String NHS_NUMBER = "9692294935";
    private static final String TASK_ID = "3a93dfdd-5e72-4f23-8311-9f22772787af";
    private static final String FROM_ASID = "200000000205";
    private static final String TO_ASID = "200000001161";
    private static final String TO_ODS_CODE = "B86041";
    private static final String FROM_ODS_CODE = "N82668";

    @Mock
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @Spy
    private XPathService xPathService;

    @Mock
    private TimestampService timestampService;

    @Mock
    private TaskDispatcher taskDispatcher;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @InjectMocks
    private EhrExtractRequestHandler ehrExtractRequestHandler;

    @Test
    public void When_ValidEhrRequestReceived_Expect_EhrExtractStatusIsCreated() {
        Document soapHeader = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document soapBody = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");
        Instant now = Instant.now();
        when(timestampService.now()).thenReturn(now);
        when(randomIdGeneratorService.createNewId()).thenReturn(TASK_ID);

        ehrExtractRequestHandler.handle(soapHeader, soapBody);

        var expectedEhrExtractStatus = createEhrExtractStatusMatchingXmlFixture(now);
        verify(ehrExtractStatusRepository).save(expectedEhrExtractStatus);
        var expectedGetGpcStructuredTaskDefinition = createTaskMatchingXmlFixture();
        verify(taskDispatcher).createTask(expectedGetGpcStructuredTaskDefinition);
    }

    @Test
    @SneakyThrows
    public void When_DuplicateEhrExtractRequestReceived_Expect_NoTasksAreCreated() {
        Document soapHeader = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_header.xml");
        Document soapBody = ResourceHelper.loadClasspathResourceAsXml("/ehr/request/RCMR_IN010000UK05_body.xml");
        Instant now = Instant.now();
        when(timestampService.now()).thenReturn(now);
        EhrExtractStatus expected = createEhrExtractStatusMatchingXmlFixture(now);
        when(ehrExtractStatusRepository.save(expected)).thenThrow(mock(DuplicateKeyException.class));

        ehrExtractRequestHandler.handle(soapHeader, soapBody);

        verify(ehrExtractStatusRepository).save(expected);
        verifyNoInteractions(taskDispatcher);
    }

    private EhrExtractStatus createEhrExtractStatusMatchingXmlFixture(Instant timestamp) {
        return EhrExtractStatus.builder()
            .created(timestamp)
            .updatedAt(timestamp)
            .conversationId(CONVERSATION_ID)
            .ehrRequest(EhrExtractStatus.EhrRequest.builder()
                .requestId(REQUEST_ID)
                .nhsNumber(NHS_NUMBER)
                .fromPartyId("N82668-820670")
                .toPartyId("B86041-822103")
                .fromAsid(FROM_ASID)
                .toAsid(TO_ASID)
                .toOdsCode(TO_ODS_CODE)
                .fromOdsCode(FROM_ODS_CODE)
                .toOdsCode("B86041")
                .build()
            ).build();
    }

    private GetGpcStructuredTaskDefinition createTaskMatchingXmlFixture() {
        return GetGpcStructuredTaskDefinition.builder()
            .requestId(REQUEST_ID)
            .conversationId(CONVERSATION_ID)
            .nhsNumber(NHS_NUMBER)
            .taskId(TASK_ID)
            .fromAsid(FROM_ASID)
            .toAsid(TO_ASID)
            .toOdsCode(TO_ODS_CODE)
            .fromOdsCode(FROM_ODS_CODE)
            .build();
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
        owner.setAttribute(attr.getName(), StringUtils.EMPTY);
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
