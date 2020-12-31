package uk.nhs.adaptors.gp2gp.common.service;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XPathService {
    public static Document prepareDocumentFromXml(String xml) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document document;
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xml));
            document = documentBuilder.parse(inputSource);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException(e);
        }

        return document;
    }

    public static String getNodeValue(Document xmlDoc, String path) {
        try {
            XPathExpression xPathExpression = XPathFactory.newInstance()
                .newXPath()
                .compile(path);
            return (String) xPathExpression.evaluate(xmlDoc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
