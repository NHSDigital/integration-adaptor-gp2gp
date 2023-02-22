package uk.nhs.adaptors.gp2gp.ehr.status.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequest;
import uk.nhs.adaptors.gp2gp.ehr.status.model.EhrStatusRequestQuery;
import uk.nhs.adaptors.gp2gp.ehr.status.service.EhrStatusRequestsService;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping(path = "/requests")
public class EhrStatusRequestsController {

    private EhrStatusRequestsService ehrRequestsService;

    @PostMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EhrStatusRequest>> getEhrRequestsEncodedForm(EhrStatusRequestQuery request) {

        Optional<List<EhrStatusRequest>> ehrRequestOptional = ehrRequestsService.getEhrStatusRequests(request);
        return ehrRequestOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<EhrStatusRequest>> getEhrRequests(@RequestBody EhrStatusRequestQuery request) {

        Optional<List<EhrStatusRequest>> ehrRequestOptional = ehrRequestsService.getEhrStatusRequests(request);
        return ehrRequestOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

}