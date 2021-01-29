package uk.nhs.adaptors.gp2gp.gpc;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrDocumentTemplateParameters;

import org.hl7.fhir.dstu3.model.Binary;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class GpcDocumentTranslator {
    private static final String PAYLOAD_KEY = "payload";
    private static final String ATTACHMENTS_KEY = "attachments";
    private static final String CONTENT_TYPE_KEY = "content_type";
    private static final String IS_BASE64_KEY = "is_base64";
    private static final String DESCRIPTION_KEY = "description";

    private final FhirParseService fhirParseService;
    private final EhrDocumentMapper ehrDocumentMapper;

    public String translateToMhsOutboundRequestPayload(GetGpcDocumentTaskDefinition taskDefinition, String response, String messageId) {
        Binary binary = fhirParseService.parseResource(response, Binary.class);

        EhrDocumentTemplateParameters ehrDocumentTemplateParameters =
            ehrDocumentMapper.mapToMhsPayloadTemplateParameters(taskDefinition, messageId);
        String xmlContent = ehrDocumentMapper.mapMhsPayloadTemplateToXml(ehrDocumentTemplateParameters);

        return prepareJsonPayload(taskDefinition, binary, xmlContent);
    }

    private String prepareJsonPayload(GetGpcDocumentTaskDefinition taskDefinition, Binary binary, String xmlContent) {
        JSONObject root = prepareJsonObject(PAYLOAD_KEY, xmlContent);

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(prepareJsonObject(CONTENT_TYPE_KEY, binary.getContentType()));
        jsonArray.put(prepareJsonObject(IS_BASE64_KEY, Boolean.TRUE.toString()));
        jsonArray.put(prepareJsonObject(DESCRIPTION_KEY, taskDefinition.getDocumentId()));
        jsonArray.put(prepareJsonObject(PAYLOAD_KEY, binary.getContentAsBase64()));

        root.put(ATTACHMENTS_KEY, jsonArray);

        return root.toString();
    }

    private JSONObject prepareJsonObject(String key, String value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, value);

        return jsonObject;
    }
}
