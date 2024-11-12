package uk.nhs.adaptors.gp2gp.ehr;

import lombok.AllArgsConstructor;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;

import java.util.Optional;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/ehr-resend")
public class EhrResendController {
    private EhrExtractStatusRepository ehrExtractStatusRepository;

    @PostMapping("/{conversationId}")
    public ResponseEntity<OperationOutcome> scheduleEhrExtractResend(@PathVariable String conversationId) {
        Optional<EhrExtractStatus> ehrExtractStatus = ehrExtractStatusRepository.findByConversationId(conversationId);

        return ehrExtractStatus.map(e -> new ResponseEntity<OperationOutcome>(HttpStatus.ACCEPTED))
                .orElse(new ResponseEntity<>(new OperationOutcome(), HttpStatus.NOT_FOUND));
    }

}
