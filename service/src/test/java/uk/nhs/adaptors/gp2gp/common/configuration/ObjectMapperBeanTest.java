package uk.nhs.adaptors.gp2gp.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectMapperBeanTest {

    @Test
    public void createsObjectMapperWithUnlimitedStringLengthConstraint() {
        // The way we use JSON to store serialized XML and base64 encoded attachments means that the size of individual
        // strings can get very large. We've set the max string size below to be 2GB.
        // Ideal solution would be to avoid using JSON to store very large strings of data in the first place,
        // and if we must then not use the ObjectMapper to extract that information.
        ObjectMapperBean objectMapperBean = new ObjectMapperBean();
        ObjectMapper objectMapper = objectMapperBean.objectMapper(new Jackson2ObjectMapperBuilder());

        assertThat(objectMapper.getFactory().streamReadConstraints().getMaxStringLength()).isEqualTo(Integer.MAX_VALUE);
    }
}