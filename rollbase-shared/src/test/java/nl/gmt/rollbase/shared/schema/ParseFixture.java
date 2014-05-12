package nl.gmt.rollbase.shared.schema;

import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.TestUtils;
import nl.gmt.rollbase.shared.merge.JAXBUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(JUnit4.class)
public class ParseFixture {
    @Test
    public void parse() throws JAXBException, RollbaseException {
        Application application = (Application)SchemaUtils.createUnmarshaller().unmarshal(TestUtils.openCrmXml());

        System.out.println(application.toString());
    }

    @Test
    public void save() throws JAXBException, IOException, RollbaseException, TransformerException {
        Application application = (Application)SchemaUtils.createUnmarshaller().unmarshal(TestUtils.openCrmXml());

        new File("tmp").mkdirs();

        try (OutputStream os = new FileOutputStream("tmp/CRM_v2 (copy).xml")) {
            JAXBUtils.marshalFormatted(
                SchemaUtils.createMarshaller(),
                application,
                new StreamResult(os)
            );
        }
    }
}
