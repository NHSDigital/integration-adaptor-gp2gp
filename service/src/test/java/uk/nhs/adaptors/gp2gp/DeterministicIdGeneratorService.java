package uk.nhs.adaptors.gp2gp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;

public class DeterministicIdGeneratorService extends RandomIdGeneratorService {

    Queue<String> ids;

    @SneakyThrows
    public void reset() {
        ids = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/uuids.txt")))) {
            reader.lines().forEach(ids::add);
        }
    }

    @Override
    public String createNewId() {
        if (ids == null) {
            reset();
        }
        return ids.remove();
    }
}
