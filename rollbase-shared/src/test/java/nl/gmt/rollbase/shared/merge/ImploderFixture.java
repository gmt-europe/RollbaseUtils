package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.RbIdMode;
import nl.gmt.rollbase.shared.TestUtils;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.SchemaUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.transform.stream.StreamResult;
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

        try (InputStream is = TestUtils.openCrmXml()) {
            application = (Application)SchemaUtils.createUnmarshaller().unmarshal(new StreamSource(is));
        }

        // Re-serialize the application so we have a known starting point.

        String expected = serialize(application);

        // Explode the application.

        File target = new File("tmp");

        target.mkdirs();

        try (FileWriter fileWriter = new FileWriter(target)) {
            new Exploder().explode(application, fileWriter, RbIdMode.LONG);
        }

        // And implode it.

        application = new Imploder().implode(new File("tmp"));

        assertEquals(expected, serialize(application));
    }

    private String serialize(Application application) throws Exception {
        // Sort the application so we are comparing stable results.

        MergeUtils.sort(application);

        try (Writer writer = new StringWriter()) {
            JAXBUtils.marshalFormatted(
                SchemaUtils.createMarshaller(),
                application,
                new StreamResult(writer)
            );

            return writer.toString();
        }
    }
}
