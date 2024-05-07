package uk.nhs.adaptors.gp2gp.gpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.common.service.TimestampService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskDispatcher;
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
    @Mock private RandomIdGeneratorService randomIdGeneratorService;

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
                    .contentType("image/jpeg")
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
                        .contentType("image/jpeg")
                        .originalDescription("NHS Docs")
                        .build()
                )
            )
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    public void When_BundleContainsAttachmentMissingAUrl_Expect_DocumentAddedToEhrExtractStatusServiceAsAbsent(String url) {
        // Arrange
        when(this.structuredRecordMappingService.getAbsentAttachments(any())).thenReturn(List.of());
        when(this.structuredRecordMappingService.getExternalAttachments(any())).thenReturn(
            List.of(
                OutboundMessage.ExternalAttachment.builder()
                    .documentId("Very important document")
                    .url(url)
                    .identifier(List.of(Identifier.builder().value("Mewow").build()))
                    .originalDescription("Pictures of cats")
                    .filename("cats.jpg")
                    .contentType("image/jpeg")
                    .build()
            )
        );

        // Act
        getGpcStructuredTaskExecutor.execute(
                GetGpcStructuredTaskDefinition.builder()
                        .conversationId("1984")
                        .taskId("HIHO")
                        .build()
        );

        // Assert
        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocumentDocumentReferences(
            any(),
            eq(
                List.of(
                    EhrExtractStatus.GpcDocument.builder()
                        .documentId("Very important document")
                        .fileName("AbsentAttachmentVery important document.txt")
                        .accessDocumentUrl(null)
                        .objectName(null)
                        .accessedAt(stubbedTime)
                        .taskId("HIHO")
                        .messageId("1984")
                        .contentType("text/plain")
                        .isSkeleton(false)
                        .identifier(List.of(Identifier.builder().value("Mewow").build()))
                        .originalDescription("Pictures of cats")
                        .build()
                )
            )
        );
    }

    @Test
    public void When_BundleContainsAbsentAttachment_Expect_DocumentAddedToEhrExtractStatusService() {
        when(this.structuredRecordMappingService.getAbsentAttachments(any())).thenReturn(
            List.of(
                OutboundMessage.ExternalAttachment.builder()
                    .documentId("NopeDocument")
                    .url("https://assets.nhs.uk/nope.jpg")
                    .identifier(List.of(Identifier.builder().value("Nooooope").build()))
                    .originalDescription("Nope Nope Nope")
                    .filename("ignored.jpg")
                    .contentType("image/jpeg")
                    .build()
            )
        );

        getGpcStructuredTaskExecutor.execute(
            GetGpcStructuredTaskDefinition.builder()
                .conversationId("1471")
                .taskId("vincent")
                .build()
        );

        verify(ehrExtractStatusService).updateEhrExtractStatusAccessDocumentDocumentReferences(
            any(),
            eq(
                List.of(
                    EhrExtractStatus.GpcDocument.builder()
                        .documentId("NopeDocument")
                        .fileName("AbsentAttachmentNopeDocument.txt")
                        .contentType("text/plain")
                        .accessDocumentUrl(null)
                        .objectName(null)
                        .accessedAt(stubbedTime)
                        .taskId("vincent")
                        .messageId("1471")
                        .isSkeleton(false)
                        .identifier(List.of(Identifier.builder().value("Nooooope").build()))
                        .originalDescription("Nope Nope Nope")
                        .build()
                )
            )
        );
    }

    @BeforeEach public void setup() {
        stubTimestampService();
        stubEhrExtractXml();
        setupIt();
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
            this.randomIdGeneratorService
        );
    }

    private void stubEhrExtractXml() {
        when(this.structuredRecordMappingService.mapStructuredRecordToEhrExtractXml(any(), any())).thenReturn(
            ""
        );
    }
}