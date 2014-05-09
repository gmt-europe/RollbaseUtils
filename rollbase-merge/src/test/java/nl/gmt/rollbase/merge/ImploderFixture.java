package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.XmlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ImploderFixture {
    @Test
    public void implode() throws Exception {
        // Explode a fresh version with prefixed index number so we get a stable ordering.

        Application application;

        try (InputStream is = TestUtils.loadXml()) {
            application = (Application)XmlUtils.createUnmarshaller().unmarshal(new StreamSource(is));
        }

        // Re-serialize the application so we have a known starting point.

        Marshaller marshaller = XmlUtils.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        String expected = serialize(application, marshaller);

        // Explode the application.

        File target = new File("tmp");

        target.mkdirs();

        try (FileWriter fileWriter = new FileWriter(target)) {
            new Exploder().explode(application, fileWriter);
        }

        // And implode it.

        application = new Imploder().implode(new File("tmp"));

        assertEquals(expected, serialize(application, marshaller));
    }

    private String serialize(Application application, Marshaller marshaller) throws Exception {
        // Sort the application so we are comparing stable results.

        Utils.sort(application);

        try (Writer writer = new StringWriter()) {
            marshaller.marshal(application, writer);

            return writer.toString();
        }
    }
}
