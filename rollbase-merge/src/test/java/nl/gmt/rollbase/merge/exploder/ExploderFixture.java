package nl.gmt.rollbase.merge.exploder;

import nl.gmt.rollbase.merge.Exploder;
import nl.gmt.rollbase.merge.FileWriter;
import nl.gmt.rollbase.merge.MergeException;
import nl.gmt.rollbase.merge.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RunWith(JUnit4.class)
public class ExploderFixture {
    @Test
    public void explode() throws MergeException, IOException {
        try (InputStream is = TestUtils.loadXml()) {
            File target = new File("tmp");

            target.mkdirs();

            try (FileWriter fileWriter = new FileWriter(target)) {
                new Exploder().explode(new StreamSource(is), fileWriter);
            }
        }
    }
}
