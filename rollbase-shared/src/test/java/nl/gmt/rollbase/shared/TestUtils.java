package nl.gmt.rollbase.shared;

import java.io.InputStream;

public class TestUtils {
    private TestUtils() {
    }

    public static InputStream openCrmXml() {
        return TestUtils.class.getResourceAsStream("CRM_v2.xml");
    }
}
