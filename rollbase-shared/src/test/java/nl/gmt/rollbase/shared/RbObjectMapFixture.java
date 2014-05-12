package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.SchemaUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RbObjectMapFixture {
    @Test
    public void createObjectMap() throws Exception {
        Application application = (Application)SchemaUtils.createUnmarshaller().unmarshal(TestUtils.openCrmXml());

        RbObjectMap objectMap = new RbObjectMap(application, RbIdMode.LONG);
    }
}
