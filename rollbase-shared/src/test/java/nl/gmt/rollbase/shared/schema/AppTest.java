package nl.gmt.rollbase.shared.schema;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() throws SAXException, IOException {
        InputStream stream = getClass().getResourceAsStream("Rollbase.xsd");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(stream));
        Validator validator = schema.newValidator();
        Source xmlFile = new StreamSource(new File("/home/gmt/Desktop/CRM_v2.xml"));
        try {
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (SAXException e) {
            System.err.println(xmlFile.getSystemId() + " is NOT valid");
            if (e instanceof SAXParseException) {
                SAXParseException pe = (SAXParseException)e;
                System.err.println("At: " + pe.getLineNumber() + ":" + pe.getColumnNumber());
            }
            System.err.println("Reason: " + e.getLocalizedMessage());
        }
    }
}
