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

    /**
     * Wraps a given XML string within a root element.
     * <p>
     * This method is useful when working with XML fragments or segments that do not have a single root element.
     * Such fragments are not well-formed XML documents, making them incompatible with certain XML parsers
     * or operations, such as XPath queries, which require a single root element to function correctly.
     * </p>
     * <p>
     * By wrapping the provided XML string within a Root element, this method ensures that the resulting
     * XML is well-formed and can be safely processed with XML tools.
     * </p>
     *
     * @param xmlString the XML string that may lack a root element
     * @return a well-formed XML string with the provided content wrapped inside a Root element
     */
    public static String wrapXmlInRootElement(String xmlString) {
        return "<Root>%s</Root>".formatted(xmlString);
    }

    public static boolean xpathMatchFound(String xmlString, String xPathExpression) throws
        XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        final NodeList nodeList = getNodeListFromXpath(xmlString, xPathExpression);

        return nodeList.getLength() > 0;
    }

    public static String getXmlStringFromFile(String directory, String filename) {
        return ResourceTestFileUtils.getFileContent(
            directory + filename
        );
    }
}