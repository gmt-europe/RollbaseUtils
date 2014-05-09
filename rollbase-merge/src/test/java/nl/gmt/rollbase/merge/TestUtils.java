package nl.gmt.rollbase.merge;

import java.io.InputStream;

public class TestUtils {
    private TestUtils() {
    }

    public static InputStream loadXml() {
        return TestUtils.class.getResourceAsStream("CRM_v2.xml");
    }
}
