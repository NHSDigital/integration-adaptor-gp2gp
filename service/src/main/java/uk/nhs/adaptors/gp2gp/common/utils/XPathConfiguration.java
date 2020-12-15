package uk.nhs.adaptors.gp2gp.common.utils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XPathConfiguration {
    @Bean
    public XPath configureXpath() {
        return XPathFactory.newInstance()
            .newXPath();
    }
}
