package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.configuration.Gp2gpConfiguration;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
import uk.nhs.adaptors.gp2gp.ehr.EhrDocumentMapper;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.mhs.model.Identifier;
import uk.nhs.adaptors.gp2gp.mhs.model.OutboundMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetGpcStructuredTaskExecutorTest {
    @Mock private TimestampService timestampService;
    @Mock private GpcClient gpcClient;
    @Mock private StorageConnectorService storageConnectorService;
    @Mock private EhrExtractStatusService ehrExtractStatusService;
    @Mock private DetectTranslationCompleteService detectTranslationCompleteService;
    @Mock private MessageContext messageContext;
    @Mock private FhirParseService fhirParseService;
    @Mock private ObjectMapper objectMapper;
    @Mock private StructuredRecordMappingService structuredRecordMappingService;
    @Mock private TaskDispatcher taskDispatcher;
    @Mock private Gp2gpConfiguration gp2gpConfiguration;
    @Mock private RandomIdGeneratorService randomIdGeneratorService;
    @Mock private EhrDocumentMapper ehrDocumentMapper;

    @Test
    public void When_BundleContainsExternalDocumentReference_Expect_DocumentAddedToEhrExtractStatusService() {
        when(this.structuredRecordMappingService.getExternalAttachments(any())).thenReturn(
            List.of(
                OutboundMessage.ExternalAttachment.builder()
                    .documentId("Docktor")
                    .url("https://assets.nhs.uk/nhsuk-cms/images/IS_0818_homepage_hero_3_913783962.width-1000.jpg")
                    .identifier(List.of(Identifier.builder().value("Medicine in action").build()))
                    .originalDescription("NHS Docs")
                    .filename("homepage_hero.jpg")
                    .build()
            )
        );

        getGpcStructuredTaskExecutor.execute(
            GetGpcStructuredTaskDefinition.builder()
                .conversationId("118 118")
                .taskId("tasky boi")
                .build()
        );

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocumentDocumentReferences(
            any(),
            eq(
                List.of(
                    EhrExtractStatus.GpcDocument.builder()
                        .documentId("Docktor")
                        .accessDocumentUrl("https://assets.nhs.uk/nhsuk-cms/images/IS_0818_homepage_hero_3_913783962.width-1000.jpg")
                        .objectName(null)
                        .accessedAt(stubbedTime)
                        .taskId("tasky boi")
                        .messageId("118 118")
                        .isSkeleton(false)
                        .identifier(List.of(Identifier.builder().value("Medicine in action").build()))
                        .fileName("homepage_hero.jpg")
                        .originalDescription("NHS Docs")
                        .build()
                )
            )
        );
    }

    @Test
    public void When_EhrSizeExceedsThreshold_Expect_SkeletonExtractAddedToEhrExtractStatusService() {
        when(this.structuredRecordMappingService.mapStructuredRecordToEhrExtractXml(any(), any())).thenReturn(
            "<BIG EHR />"
        );

        getGpcStructuredTaskExecutor.execute(
            GetGpcStructuredTaskDefinition.builder().conversationId("118 118").build()
        );

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocumentDocumentReferences(
            any(),
            eq(
                List.of(
                    EhrExtractStatus.GpcDocument.builder()
                        .messageId("1st randomly generated ID")
                        .documentId("2nd randomly generated ID")
                        .objectName("2nd randomly generated ID.gzip")
                        .fileName("2nd randomly generated ID.gzip")
                        .taskId("3rd randomly generated ID")
                        .accessDocumentUrl(null)
                        .accessedAt(stubbedTime)
                        .isSkeleton(true)
                        .identifier(null)
                        .originalDescription(null)
                        .build()
                )
            )
        );
    }

    @BeforeEach public void setup() {
        stubRandomIdGeneratorService();
        stubTimestampService();
        stubEhrExtractXml();
        setupIt();
    }

    private void stubRandomIdGeneratorService() {
        when(this.randomIdGeneratorService.createNewId()).thenReturn(
            "1st randomly generated ID",
            "2nd randomly generated ID",
            "3rd randomly generated ID",
            "4th randomly generated ID"
        );
    }

    private Instant stubbedTime;
    private void stubTimestampService() {
        this.stubbedTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(timestampService.now()).thenReturn(stubbedTime);
    }

    private GetGpcStructuredTaskExecutor getGpcStructuredTaskExecutor;
    private void setupIt() {
        this.getGpcStructuredTaskExecutor = new GetGpcStructuredTaskExecutor(
            this.timestampService,
            this.gpcClient,
            this.storageConnectorService,
            this.ehrExtractStatusService,
            this.detectTranslationCompleteService,
            this.messageContext,
            this.fhirParseService,
            this.objectMapper,
            this.structuredRecordMappingService,
            this.taskDispatcher,
            this.gp2gpConfiguration,
            this.randomIdGeneratorService,
            this.ehrDocumentMapper
        );
    }

    private void stubEhrExtractXml() {
        when(this.structuredRecordMappingService.mapStructuredRecordToEhrExtractXml(any(), any())).thenReturn(
            ""
        );
    }
}