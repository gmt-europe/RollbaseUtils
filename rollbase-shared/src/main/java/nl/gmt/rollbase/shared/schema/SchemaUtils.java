package nl.gmt.rollbase.shared.schema;

import nl.gmt.rollbase.shared.RbAccessor;
import nl.gmt.rollbase.shared.RbIdMode;
import nl.gmt.rollbase.shared.RollbaseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SchemaUtils {
    private SchemaUtils() {
    }

    private static String buildListRe(String itemRe) {
        return "^\\s*" + itemRe + "(\\s*,\\s*" + itemRe + ")*\\s*(,*\\s*)*$";
    }

    private static final Pattern ID_LIST_PATTERN = Pattern.compile(buildListRe("(?:\\d+)"));
    private static final Pattern UUID_LIST_PATTERN = Pattern.compile(buildListRe("(?:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"), Pattern.CASE_INSENSITIVE);
    private final static JAXBContext CONTEXT = createContext();

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(Application.class);
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

    public static void validate(Source source) throws SAXException, IOException {
        Validate.notNull(source, "source");

        try (InputStream stream = SchemaUtils.class.getResourceAsStream("Rollbase.xsd")) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(stream));

            schema.newValidator().validate(source);
        }
    }

    public static List<String> getPropertyKeys(Properties properties) {
        Validate.notNull(properties, "properties");

        List<String> keys = new ArrayList<>();

        for (Object object : properties.getAny()) {
            Element element = (Element)object;

            keys.add(element.getTagName());
        }

        return keys;
    }

    public static String getProperty(Properties properties, String key) {
        Validate.notNull(properties, "properties");
        Validate.notNull(key, "key");

        for (Object object : properties.getAny()) {
            Element element = (Element)object;

            if (key.equals(element.getTagName())) {
                return element.getTextContent().trim();
            }
        }

        return null;
    }

    public static void setProperty(Properties properties, String key, String value) throws RollbaseException {
        Validate.notNull(properties, "properties");
        Validate.notNull(key, "key");
        Validate.notNull(value, "value");

        for (Object object : properties.getAny()) {
            Element element = (Element)object;

            if (key.equals(element.getTagName())) {
                element.setTextContent(value);
                return;
            }
        }
        Element property;
        try {
            property = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument()
                .createElement(key);
        } catch (ParserConfigurationException e) {
            throw new RollbaseException("Cannot create property", e);
        }

        property.setTextContent(value);

        properties.getAny().add(property);
    }

    public static boolean removeProperty(Properties properties, String key) {
        Validate.notNull(properties, "properties");
        Validate.notNull(key, "key");

        List<Object> any = properties.getAny();

        for (int i = 0; i < any.size(); i++) {
            Element element = (Element)any.get(i);

            if (key.equals(element.getTagName())) {
                any.remove(i);
                return true;
            }
        }

        return false;
    }

    public static boolean looksLikeIdList(String value, RbIdMode idMode) {
        Validate.notNull(value, "value");
        Validate.notNull(idMode, "idMode");

        return getIdListPattern(idMode).matcher(value).matches();
    }

    public static List<String> parseIdList(RbNode node, RbAccessor accessor, RbIdMode idMode) throws RollbaseException {
        Validate.notNull(node, "node");
        Validate.notNull(accessor, "accessor");

        String value = (String)accessor.getValue(node);

        // objDef1 and objDef2 of RelationshipDef may be references to actual column names.

        if (!getIdListPattern(idMode).matcher(value).matches()) {
            if (!(
                (node instanceof RelationshipDef) &&
                (accessor.getName().equals("ObjDef1") || accessor.getName().equals("ObjDef2"))
            )) {
                throw new RollbaseException(String.format(
                    "Cannot parse id '%s' of node '%s'",
                    value,
                    node.getClass().getName()
                ));
            }

            return null;
        } else {
            return getIds(value);
        }
    }

    private static Pattern getIdListPattern(RbIdMode idMode) {
        return idMode == RbIdMode.LONG ? ID_LIST_PATTERN : UUID_LIST_PATTERN;
    }

    public static List<String> getIds(String value) {
        List<String> result = new ArrayList<>();

        for (String id : StringUtils.split(value, ',')) {
            id = id.trim();

            if (id.length() > 0) {
                result.add(id);
            }
        }

        return result;
    }
}
