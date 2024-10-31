package uk.nhs.adaptors.gp2gp.common.configuration;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectMapperConfigTest {

    @Test
    void objectMapperConfigSetToWorkWithUnlimitedDataSizeTest() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ObjectMapperConfig.class);

        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        StreamReadConstraints constraints = objectMapper.getFactory().streamReadConstraints();

        assertEquals(Integer.MAX_VALUE, constraints.getMaxStringLength(),
                     "Expected objectMapper read constraint to be practically unlimited");

        context.close();
    }

}