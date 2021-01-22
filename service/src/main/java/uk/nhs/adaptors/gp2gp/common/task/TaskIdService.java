package uk.nhs.adaptors.gp2gp.common.task;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskIdService {
    public String createNewTaskId() {
        return UUID.randomUUID().toString();
    }
}
