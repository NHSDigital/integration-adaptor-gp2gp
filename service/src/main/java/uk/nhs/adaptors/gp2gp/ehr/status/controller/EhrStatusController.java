package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusService;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/ehr-status")
public class EhrStatusController {

    private EhrStatusService ehrStatusService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<EhrStatus> getEhrStatus(@PathVariable String conversationId) {
        Optional<EhrStatus> ehrStatusOptional = ehrStatusService.getEhrStatus(conversationId);

        return ehrStatusOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

}
