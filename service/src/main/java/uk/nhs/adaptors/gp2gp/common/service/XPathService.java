package uk.nhs.adaptors.gp2gp.common.service;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;

import static javax.xml.xpath.XPathConstants.NODESET;

@Component
public class XPathService {
    public Document parseDocumentFromXml(String xml) throws SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        InputSource inputSource;
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            inputSource = new InputSource(new StringReader(xml));
            return documentBuilder.parse(inputSource);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to configure XML parser", e);
        } catch (IOException e) {
            throw new RuntimeException("IO error while reading XML", e);
        }
    }

    public String getNodeValue(Document xmlDoc, String expression) {
        try {
            XPathExpression xPathExpression = XPathFactory.newInstance()
                    .newXPath()
                    .compile(expression);
            return (String) xPathExpression.evaluate(xmlDoc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid xpath expression " + expression, e);
        }
    }

    @SneakyThrows
    public NodeList getNodes(Document document, String xPath) {
        XPathExpression xPathExpression = XPathFactory.newInstance()
                .newXPath()
                .compile(xPath);

        return ((NodeList) xPathExpression.evaluate(document, NODESET));
    }
}
