package nl.gmt.rollbase.shared.schema;

import nl.gmt.rollbase.support.schema.XmlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;

@RunWith(JUnit4.class)
public class XsdValidateFixture {
    @Test
    public void validate() throws SAXException, IOException {
        String fileName = "/home/gmt/Desktop/CRM_v2.xml";

        try {
            XmlUtils.validate(new StreamSource(new File(fileName)));
            System.out.println(fileName + " is valid");
        } catch (SAXException e) {
            System.err.println(fileName + " is NOT valid");

            if (e instanceof SAXParseException) {
                SAXParseException pe = (SAXParseException)e;
                System.err.println("At: " + pe.getLineNumber() + ":" + pe.getColumnNumber());
            }

            System.err.println("Reason: " + e.getLocalizedMessage());
        }
    }
}
