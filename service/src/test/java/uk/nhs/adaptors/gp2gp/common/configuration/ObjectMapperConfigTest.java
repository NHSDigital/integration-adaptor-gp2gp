package uk.nhs.adaptors.gp2gp.common.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void javaTimeModuleIsRegisteredTest() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ObjectMapperConfig.class);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        boolean javaTimeModuleRegistered = objectMapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310");
        assertTrue(javaTimeModuleRegistered);
    }

    @Test
    void objectMapperConfigIgnoresUnknownPropertiesTest() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ObjectMapperConfig.class);
        ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

        String jsonWithUnknownProperty = "{\"knownProperty\":\"testValue\", \"unknownProperty\":\"extraValue\"}";

        assertDoesNotThrow(() -> {
            TestClass result = objectMapper.readValue(jsonWithUnknownProperty, TestClass.class);
            assertEquals("testValue", result.knownProperty);
        });

        context.close();
    }

    public static class TestClass {
        @JsonProperty("knownProperty")
        private String knownProperty;
    }

}