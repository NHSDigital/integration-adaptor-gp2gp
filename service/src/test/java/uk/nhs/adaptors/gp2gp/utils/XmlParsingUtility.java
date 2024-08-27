package uk.nhs.adaptors.gp2gp.utils;

import org.w3c.dom.Document;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringReader;

public final class XmlParsingUtility {
    private XmlParsingUtility() { }

    public static Document getDocumentFromXmlString(String xmlString) throws
        IOException, SAXException, ParserConfigurationException {
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = builderFactory.newDocumentBuilder();
        final StringReader stringReader = new StringReader(xmlString);

        return builder.parse(new InputSource(stringReader));
    }

    public static NodeList getNodeListFromXpath(String xmlString, String xPathExpression) throws
        XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final Document document = getDocumentFromXmlString(xmlString);

        return (NodeList) xPath
            .compile(xPathExpression)
            .evaluate(document, XPathConstants.NODESET);
    }

    public static boolean xpathMatchFound(String xmlString, String xPathExpression) throws
        XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        final NodeList nodeList = getNodeListFromXpath(xmlString, xPathExpression);

        return nodeList.getLength() > 0;
    }
}