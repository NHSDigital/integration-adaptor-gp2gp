package uk.nhs.adaptors.gp2gp.ehr;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.gpc.DetectTranslationCompleteService;
import uk.nhs.adaptors.gp2gp.gpc.DocumentToMHSTranslator;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskDefinition;
import uk.nhs.adaptors.gp2gp.gpc.GetGpcDocumentTaskExecutor;
import uk.nhs.adaptors.gp2gp.gpc.GpcClient;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetGpcDocumentTaskExecutorTest {

    @Mock private StorageConnectorService storageConnectorService;
    @Mock private EhrExtractStatusService ehrExtractStatusService;
    @Mock private GpcClient gpcClient;
    @Mock private DocumentToMHSTranslator documentToMHSTranslator;
    @Mock private DetectTranslationCompleteService detectTranslationCompleteService;
    @Mock private FhirParseService fhirParseService;
    @Mock private GetAbsentAttachmentTaskExecutor getAbsentAttachmentTaskExecutor;

    @InjectMocks
    private GetGpcDocumentTaskExecutor getGpcDocumentTaskExecutor;

    private final GetGpcDocumentTaskDefinition getGpcDocumentTaskDefinition =
        GetGpcDocumentTaskDefinition.builder().build();

    @Test
    public void When_ExecuteWithGpcClientExceptionNoOperationOutcome_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition)).thenThrow(new GpConnectException(""));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithGpcClientExceptionHasNoOperationOutcomeIssue_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome();

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithGpcClientExceptionHasEmptyOperationOutcomeIssue_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of()
            );

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithGpcClientExceptionHasOperationOutcomeIssueHasNoDetail_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of(
                    new OperationOutcome.OperationOutcomeIssueComponent()
                )
            );

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithOperationOutcomeIssueDetailHasNoCoding_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of(new OperationOutcome.OperationOutcomeIssueComponent()
                    .setDetails(
                        new CodeableConcept()
                    )
                )
            );


        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithOperationOutcomeIssueDetailHasEmptyCoding_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of(
                    new OperationOutcome.OperationOutcomeIssueComponent().setDetails(
                        new CodeableConcept().setCoding(List.of())
                    )
                )
            );

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithOperationOutcomeIssueDetailCodingDisplay_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of(
                    new OperationOutcome.OperationOutcomeIssueComponent().setDetails(
                        new CodeableConcept().setCoding(List.of(
                            new Coding()
                        ))
                    )
                )
            );

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithOperationOutcomeIssueDetailCodingDisplayIsEmptyString_Expect_HandleAbsentAttachmentToUseEmptyOptional() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of(
                    new OperationOutcome.OperationOutcomeIssueComponent().setDetails(
                        new CodeableConcept().setCoding(List.of(
                            new Coding("", "", "")
                        ))
                    )
                )
            );

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.empty()
        );
    }

    @Test
    public void When_ExecuteWithValidOperationOutcomeIssueDetailCodingDisplay_Expect_HandleAbsentAttachmentToUseOptionalOfThis() {
        var operationOutcome = new OperationOutcome()
            .setIssue(
                List.of(
                    new OperationOutcome.OperationOutcomeIssueComponent().setDetails(
                        new CodeableConcept().setCoding(List.of(
                            new Coding("", "", "display-message")
                        ))
                    )
                )
            );

        when(gpcClient.getDocumentRecord(getGpcDocumentTaskDefinition))
            .thenThrow(new GpConnectException("", operationOutcome));

        getGpcDocumentTaskExecutor.execute(getGpcDocumentTaskDefinition);

        verify(getAbsentAttachmentTaskExecutor).handleAbsentAttachment(
            getGpcDocumentTaskDefinition,
            Optional.of("display-message")
        );
    }
}
