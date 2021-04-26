package uk.nhs.adaptors.gp2gp.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.SneakyThrows;

public class XPathServiceTest {

    @Test
    @SneakyThrows
    public void When_ValidXmlIsParsed_Expect_DocumentIsReturned() {
        var document = new XPathService().parseDocumentFromXml("<root/>");
        assertThat(document.getChildNodes().item(0).getNodeName()).isEqualTo("root");
    }

    @Test
    @SneakyThrows
    public void When_InvalidXmlIsParsed_Expect_SAXExceptionIsThrown() {
        assertThatExceptionOfType(SAXException.class)
            .isThrownBy(() -> new XPathService().parseDocumentFromXml("NOT XML"));
    }

    @Test
    @SneakyThrows
    public void When_GetNodeValueWithValidXPath_Expect_TheValueIsReturned() {
        var document = new XPathService().parseDocumentFromXml("<element>value</element>");
        assertThat(new XPathService().getNodeValue(document, "/element"))
            .isEqualTo("value");
    }

    @Test
    @SneakyThrows
    public void When_GetNodeValueWithInvalidXPath_Expect_ThrowsRuntimeException() {
        var document = new XPathService().parseDocumentFromXml("<element>value</element>");
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new XPathService().getNodeValue(document, "!!!NOT XPATH"))
            .withMessageContaining("!!!NOT XPATH");
    }

    @Test
    @SneakyThrows
    public void When_GetNodesWithValidXPath_Expect_NodeListReturned() {
        var document = new XPathService().parseDocumentFromXml("<elements><element>value</element><element>value2</element></elements>");
        NodeList nodes = new XPathService().getNodes(document, "//element");
        assertThat(nodes.getLength()).isEqualTo(2);
        assertAll(
            () -> assertThat(nodes.getLength()).isEqualTo(2),
            () -> assertThat(nodes.item(0).getTextContent()).isEqualTo("value"),
            () -> assertThat(nodes.item(1).getTextContent()).isEqualTo("value2")
        );
    }
}
