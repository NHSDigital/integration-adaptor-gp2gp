package uk.nhs.adaptors.gp2gp.utils;

import lombok.SneakyThrows;
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class XmlAssertion extends AbstractAssert<XmlAssertion, String> {

    protected XmlAssertion(String xml) {
        super(xml, XmlAssertion.class);
    }

    /**
     * Create an assertion for a {@link String} XML value.
     * @param actual the actual XML value.
     * @return the created assertion object.
     */
    @Contract("_ -> new")
    public static @NotNull XmlAssertion assertThatXml(String actual) {
        return new XmlAssertion(actual);
    }

    /**
     * Verifies that the actual {@link String} XML contains the given XPath value.
     * @param expectedXPath the {@link String} XPath value expected to be found within the provided XML.
     */
    @SneakyThrows
    public void containsXPath(String expectedXPath) {
        isNotNull();
        if (!XmlParsingUtility.xpathMatchFound(actual, expectedXPath)) {
            failWithMessage(
                """
                    Expected to find expectedXPath in XML, but it was not present.

                    <XPath>: %s
                    <XML>: %s""",
                expectedXPath,
                actual
            );
        }
    }

    /**
     * Verifies that the actual {@link String} XML contains all the given xPath values.
     * @param xPaths A Collection of Strings (xPaths) expected to be within the provided XML.
     */
    public void containsAllXPaths(Collection<String> xPaths) {
        xPaths.forEach(this::containsXPath);
    }

    /**
     * Verifies that the actual {@link String} XML does not contain the given XPath value.
     * @param xPath the {@link String} XPath value expected to not be found within the provided XML.
     */
    @SneakyThrows
    public void doesNotContainXPath(String xPath) {
        isNotNull();
        if (XmlParsingUtility.xpathMatchFound(actual, xPath)) {
            failWithMessage(
                """
                Expected not to find xPath in XML, but it was present.

                <XPath>: %s
                <XML>: %s""",
                xPath,
                actual
            );
        }
    }
}
