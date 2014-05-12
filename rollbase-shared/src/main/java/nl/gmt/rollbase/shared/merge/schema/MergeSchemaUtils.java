package nl.gmt.rollbase.shared.merge.schema;

import org.apache.commons.lang.Validate;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;

public class MergeSchemaUtils {
    private MergeSchemaUtils() {
    }

    public final static String FILE_NAME = "IdMaps.xml";
    private final static JAXBContext CONTEXT = createContext();

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(ApplicationVersions.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Marshaller createMarshaller() throws JAXBException {
        return CONTEXT.createMarshaller();
    }

    public static Unmarshaller createUnmarshaller() throws JAXBException {
        return CONTEXT.createUnmarshaller();
    }

    public static void validate(Source source) throws SAXException {
        Validate.notNull(source, "source");

        try (InputStream stream = MergeSchemaUtils.class.getResourceAsStream("Merge.xsd")) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(stream));

            schema.newValidator().validate(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
