package uk.nhs.adaptors.gp2gp.common.configuration;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperPostProcessorConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ObjectMapper objectMapper) {
            objectMapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build());
        }
        return bean;
    }
}