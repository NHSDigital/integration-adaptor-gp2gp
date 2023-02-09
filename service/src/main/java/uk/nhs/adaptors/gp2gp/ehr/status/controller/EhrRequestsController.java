package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrRequestStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrRequestsRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatus;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrRequestsService;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/requests")
public class EhrRequestsController {

    private EhrRequestsService ehrRequestsService;

    @PostMapping()
    public ResponseEntity<EhrRequestStatus> getEhrStatus(@RequestBody EhrRequestsRequest request) {

        Optional<List<EhrRequestStatus>> ehrRequestOptional = ehrRequestsService.getEhrRequests(request);
        return ehrRequestOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

}