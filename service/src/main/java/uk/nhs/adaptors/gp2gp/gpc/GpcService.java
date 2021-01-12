package uk.nhs.adaptors.gp2gp.gpc;

import java.io.IOException;
import java.util.Base64;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorFactory;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;

@Service
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcService {

    private final GpcClient gpcClient;
    private final GpcRequestBuilder gpcRequestBuilder;
    private final StorageConnectorService storageConnectorService;

    private String temp = "<?xml version=\"1.0\" " +
        "encoding=\"UTF-8\"?><RCMR_IN010000UK05 xmlns=\"urn:hl7-org:v3\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" type=\"Message\"><id root=\"DFF5321C-C6EA-468E-BBC2-B0E48000E071\" " +
        "/><creationTime value=\"20201116171338\" /><versionCode code=\"V3NPfIT3.1.10\" /><interactionId root=\"2.16.840.1.113883.2.1" +
        ".3.2.4.12\" extension=\"RCMR_IN010000UK05\" /><processingCode code=\"P\" /><processingModeCode code=\"T\" /><acceptAckCode " +
        "code=\"NE\" /><communicationFunctionRcv type=\"CommunicationFunction\" typeCode=\"RCV\"><device type=\"Device\" " +
        "classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"200000001161\" " +
        "/></device></communicationFunctionRcv><communicationFunctionSnd type=\"CommunicationFunction\" typeCode=\"SND\"><device " +
        "type=\"Device\" classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.2.0.107\" " +
        "extension=\"200000000205\" /></device></communicationFunctionSnd><ControlActEvent type=\"ControlAct\" classCode=\"CACT\" " +
        "moodCode=\"EVN\"><author1 type=\"Participation\" typeCode=\"AUT\"><AgentSystemSDS type=\"RoleHeir\" " +
        "classCode=\"AGNT\"><agentSystemSDS type=\"Device\" classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0" +
        ".2.0.107\" extension=\"200000000205\" /></agentSystemSDS></AgentSystemSDS></author1><subject type=\"ActRelationship\" " +
        "typeCode=\"SUBJ\" contextConductionInd=\"false\"><EhrRequest type=\"ActHeir\" classCode=\"EXTRACT\" moodCode=\"RQO\"><id " +
        "root=\"041CA2AE-3EC6-4AC9-942F-0F6621CC0BFC\" /><recordTarget type=\"Participation\" typeCode=\"RCT\"><patient " +
        "type=\"Patient\" classCode=\"PAT\"><id root=\"2.16.840.1.113883.2.1.4.1\" extension=\"9690937286\" " +
        "/></patient></recordTarget><author type=\"Participation\" typeCode=\"AUT\"><AgentOrgSDS type=\"RoleHeir\" " +
        "classCode=\"AGNT\"><agentOrganizationSDS type=\"Organization\" classCode=\"ORG\" determinerCode=\"INSTANCE\"><id root=\"1.2" +
        ".826.0.1285.0.1.10\" extension=\"N82668\" /></agentOrganizationSDS></AgentOrgSDS></author><destination " +
        "type=\"Participation\" typeCode=\"DST\"><AgentOrgSDS type=\"RoleHeir\" classCode=\"AGNT\"><agentOrganizationSDS " +
        "type=\"Organization\" classCode=\"ORG\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.1.10\" extension=\"B86041\" " +
        "/></agentOrganizationSDS></AgentOrgSDS></destination></EhrRequest></subject></ControlActEvent></RCMR_IN010000UK05>";

    public void handleStructureTask(GetGpcStructuredTaskDefinition structuredTaskDefinition) throws ParserConfigurationException, SAXException,
        XPathExpressionException, IOException {

        var requestBodyParameters = GpcRequestBuilder.buildGetStructuredRecordRequestBody(temp);
        var request = gpcRequestBuilder.buildGetStructuredRecordRequest(requestBodyParameters);
        var response = gpcClient.getStructuredRecord(request);
        storageConnectorService.handleStructuredRecord(response, structuredTaskDefinition);
    }
}
