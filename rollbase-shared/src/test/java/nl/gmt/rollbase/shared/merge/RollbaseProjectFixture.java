package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.RollbaseProject;
import nl.gmt.rollbase.shared.TestUtils;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.XmlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class RollbaseProjectFixture {
    private static final Pattern SPACE_RE = Pattern.compile("\\s+");
    private static final Pattern TRAILING_COMMA_RE = Pattern.compile(",<");

    @Test
    public void save() throws JAXBException, IOException, RollbaseException {
        Application application;

        try (InputStream is = TestUtils.openCrmXml()) {
            application = (Application)XmlUtils.createUnmarshaller().unmarshal(is);
        }

        new RollbaseProject(new File("tmp")).save(application);
    }

    @Test
    public void saveAndLoad() throws Exception {
        Application application;

        try (InputStream is = TestUtils.openCrmXml()) {
            application = (Application)XmlUtils.createUnmarshaller().unmarshal(is);
        }

        int expectedVersion = application.getVersion();
        String expected = serialize(application);

        // Save the project.

        RollbaseProject project = new RollbaseProject(new File("tmp"));

        project.save(application);

        // Re-load the project.

        Application loaded = project.load();

        // And compare. We need to remove the appId property from loaded and reset the application version
        // to be able to do a correct textual compare.

        XmlUtils.removeProperty(loaded.getProps(), UuidRewriter.APP_ID_KEY);
        loaded.setVersion(expectedVersion);

        String actual = serialize(loaded);

        if (!normalize(expected).equals(normalize(actual))) {
            assertEquals(expected, actual);
        }
    }

    private String normalize(String value) {
        value = SPACE_RE.matcher(value).replaceAll("");
        value = TRAILING_COMMA_RE.matcher(value).replaceAll("<");

        return value;
    }

    private String serialize(Application application) throws Exception {
        // Sort the application so we are comparing stable results.

        MergeUtils.sort(application);

        try (Writer writer = new StringWriter()) {
            JAXBUtils.marshalFormatted(
                XmlUtils.createMarshaller(),
                application,
                new StreamResult(writer)
            );

            return writer.toString();
        }
    }
}
