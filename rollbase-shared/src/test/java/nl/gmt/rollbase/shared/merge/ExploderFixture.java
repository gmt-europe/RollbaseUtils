package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.RbIdMode;
import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.TestUtils;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.SchemaUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RunWith(JUnit4.class)
public class ExploderFixture {
    @Test
    public void explode() throws RollbaseException, IOException, JAXBException {
        Application application;

        try (InputStream is = TestUtils.openCrmXml()) {
            application = (Application)SchemaUtils.createUnmarshaller().unmarshal(new StreamSource(is));
        }

        File target = new File("tmp");

        target.mkdirs();

        try (FileWriter fileWriter = new FileWriter(target)) {
            new Exploder().explode(application, fileWriter, RbIdMode.LONG);
        }
    }
}
