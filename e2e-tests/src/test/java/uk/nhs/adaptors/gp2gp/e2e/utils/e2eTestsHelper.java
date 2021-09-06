package uk.nhs.adaptors.gp2gp.e2e.utils;

import static uk.nhs.adaptors.gp2gp.e2e.AwaitHelper.waitFor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.platform.commons.util.StringUtils;

import uk.nhs.adaptors.gp2gp.Mongo;

public class e2eTestsHelper {

    private static final String EHR_EXTRACT_REQUEST_TEST_FILE = "/ehrExtractRequest.json";
    private static final String CONVERSATION_ID_PLACEHOLDER = "%%ConversationId%%";
    private static final String FROM_ODS_CODE_PLACEHOLDER = "%%From_ODS_Code%%";
    private static final String NHS_NUMBER_PLACEHOLDER = "%%NHSNumber%%";

    // Paths in EhrExtractStatus

    public static Document fetchObjectFromEhrExtract(String conversationId, EhrExtractStatusPaths... pathToObject) {
        if (pathToObject.length > 0) {
            Document documentObject = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
            for (int i = 0; i < pathToObject.length; i++) {
                documentObject = (Document) documentObject.get(pathToObject[i].toString());
                if (documentObject == null) {
                    i = 0;
                    documentObject = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
                }
            }
            return documentObject;
        }
        return waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
    }

    public static Document fetchObjectFromEhrExtractWithValidation(Function<Document, Boolean> validator, String conversationId,
        EhrExtractStatusPaths... pathToObject) {
        var output = fetchObjectFromEhrExtract(conversationId, pathToObject);
        Boolean valid = validator.apply(output);
        if (valid)
            return output;

        return null;
    }

    public static Document fetchFirstObjectFromList(String conversationId, EhrExtractStatusPaths... pathToObject) {
        if (pathToObject.length > 0) {
            Document documentObject = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
            for (int i = 0; i < pathToObject.length - 1; i++) {
                documentObject = (Document) documentObject.get(pathToObject[i].toString());
                if (documentObject == null) {
                    i = 0;
                    documentObject = waitFor(() -> Mongo.findEhrExtractStatus(conversationId));
                }
            }
            var docList = documentObject.get(pathToObject[pathToObject.length-1].toString(), Collections.emptyList());
            return docList.isEmpty() ? null :(Document) docList.get(0);
        }
        return null;
    }

    public static String getEnvVar(String name, String defaultValue) {
        var value = System.getenv(name);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    public static String buildEhrExtractRequest(String conversationId, String notExistingPatientNhsNumber, String fromODSCode) throws IOException {
        return IOUtils.toString(e2eTestsHelper.class
            .getResourceAsStream(EHR_EXTRACT_REQUEST_TEST_FILE), StandardCharsets.UTF_8)
            .replace(CONVERSATION_ID_PLACEHOLDER, conversationId)
            .replace(NHS_NUMBER_PLACEHOLDER, notExistingPatientNhsNumber)
            .replace(FROM_ODS_CODE_PLACEHOLDER, fromODSCode);
    }
}
