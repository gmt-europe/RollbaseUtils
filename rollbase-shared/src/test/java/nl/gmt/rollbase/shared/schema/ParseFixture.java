package nl.gmt.rollbase.shared.schema;

import nl.gmt.rollbase.support.schema.XmlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

@RunWith(JUnit4.class)
public class ParseFixture {
    @Test
    public void parse() throws JAXBException {
        Application application = XmlUtils.parse(
            new StreamSource(new File("/home/gmt/Desktop/CRM_v2.xml")),
            Application.class
        );

        System.out.println(application.toString());
    }

    @Test
    public void save() throws JAXBException, IOException {
        Application application = XmlUtils.parse(
            new StreamSource(new File("/home/gmt/Desktop/CRM_v2.xml")),
            Application.class
        );

        try (OutputStream os = new FileOutputStream("/home/gmt/Desktop/CRM_v2 (copy).xml")) {
            XmlUtils.save(os, application, true);
        }
    }
}
