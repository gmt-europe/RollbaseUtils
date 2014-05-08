package nl.gmt.rollbase.support.schema;

import org.apache.commons.lang.Validate;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XmlUtils {
    private XmlUtils() {
    }

    public static void validate(Source source) throws SAXException {
        Validate.notNull(source, "source");

        try (InputStream stream = XmlUtils.class.getResourceAsStream("Rollbase.xsd")) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(stream));

            schema.newValidator().validate(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(Source source, Class<T> klass) throws JAXBException {
        Validate.notNull(source, "source");
        Validate.notNull(klass, "klass");

        return (T)JAXBContext.newInstance(klass).createUnmarshaller().unmarshal(source);
    }

    public static <T> void save(final OutputStream os, T object) throws JAXBException {
        save(os, object, false);
    }

    public static <T> void save(final OutputStream os, T object, boolean prettyPrint) throws JAXBException {
        Validate.notNull(os, "os");
        Validate.notNull(object, "object");

        Marshaller marshaller = JAXBContext.newInstance(object.getClass()).createMarshaller();

        if (prettyPrint) {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        }

        marshaller.marshal(object, os);
    }
}
