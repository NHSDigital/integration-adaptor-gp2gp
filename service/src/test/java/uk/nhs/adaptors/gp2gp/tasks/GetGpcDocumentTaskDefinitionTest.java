package uk.nhs.adaptors.gp2gp.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetGpcDocumentTaskDefinitionTest {

    @Test
    public void testest() {
        GetGpcDocumentTaskDefinition gpcDocumentTaskDefinition = new GetGpcDocumentTaskDefinition(
            "requestid", "conversationid", "documentid");

    }
}
