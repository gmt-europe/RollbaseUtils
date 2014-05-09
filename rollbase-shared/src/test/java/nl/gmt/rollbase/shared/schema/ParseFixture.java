package nl.gmt.rollbase.shared.schema;

import nl.gmt.rollbase.shared.RollbaseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

@RunWith(JUnit4.class)
public class ParseFixture {
    public static InputStream openCrmXml() {
        return ParseFixture.class.getResourceAsStream("CRM_v2.xml");
    }

    @Test
    public void parse() throws JAXBException, RollbaseException {
        Application application = (Application)XmlUtils.createUnmarshaller().unmarshal(openCrmXml());

        System.out.println(application.toString());
    }

    @Test
    public void save() throws JAXBException, IOException, RollbaseException {
        Application application = (Application)XmlUtils.createUnmarshaller().unmarshal(openCrmXml());

        new File("tmp").mkdirs();

        try (OutputStream os = new FileOutputStream("tmp/CRM_v2 (copy).xml")) {
            Marshaller marshaller = XmlUtils.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(application, os);
        }
    }
}
