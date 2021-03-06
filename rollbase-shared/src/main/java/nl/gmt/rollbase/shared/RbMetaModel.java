package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.RbKnownProperties;
import nl.gmt.rollbase.shared.schema.RbNode;
import org.apache.commons.lang.Validate;

import java.lang.reflect.Method;
import java.util.*;

public class RbMetaModel {
    private static final Object syncRoot = new Object();
    private static final Map<Class<?>, RbMetaModel> MODELS = new HashMap<>();

    private final Class<?> type;
    private final Map<String, RbAccessor> accessors = new HashMap<>();
    private final Set<String> knownProperties;

    public static RbMetaModel getMetaModel(Class<?> type) throws RollbaseException {
        Validate.notNull(type, "type");

        synchronized (syncRoot) {
            RbMetaModel metaModel = MODELS.get(type);

            if (metaModel == null) {
                metaModel = new RbMetaModel(type);
                MODELS.put(type, metaModel);
            }

            return metaModel;
        }
    }

    private RbMetaModel(Class<?> type) throws RollbaseException {
        this.type = type;
        this.knownProperties = getKnownProperties(type);

        createAccessors();
    }

    private Set<String> getKnownProperties(Class<?> type) {
        RbKnownProperties annotation = type.getAnnotation(RbKnownProperties.class);

        if (annotation == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(annotation.value())));
    }

    private void createAccessors() throws RollbaseException {
        for (Method getter : type.getMethods()) {
            if (getter.getDeclaringClass() == Object.class || getter.getReturnType().equals(Void.TYPE)) {
                continue;
            }

            if (!getter.getName().startsWith("get") && !getter.getName().startsWith("is")) {
                throw new RollbaseException(String.format(
                    "Expected method name '%s' of '%s' to start with 'get' or 'is'", type, getter.getName()
                ));
            }

            String methodName = getter.getName().startsWith("get")
                ? getter.getName().substring(3)
                : getter.getName().substring(2);

            Method setter;

            try {
                setter = type.getMethod("set" + methodName, getter.getReturnType());
            } catch (NoSuchMethodException e) {
                // Lists do not have setters.

                if (List.class.isAssignableFrom(getter.getReturnType())) {
                    setter = null;
                } else {
                    throw new RollbaseException(String.format(
                        "Cannot find setter for '%s'", getter.getName()
                    ));
                }
            }

            accessors.put(methodName, RbAccessor.createAccessor(getter, setter));
        }
    }

    public Class<?> getType() {
        return type;
    }

    public Set<String> getProperties() {
        return accessors.keySet();
    }

    public Collection<RbAccessor> getAccessors() {
        return accessors.values();
    }

    public RbAccessor getAccessor(String property) {
        Validate.notNull(property, "property");

        return accessors.get(property);
    }

    public Set<String> getKnownProperties() {
        return knownProperties;
    }

    public Object getValue(String property, RbNode parent) {
        return getAccessor(property).getValue(parent);
    }

    public void setValue(String property, RbNode parent, Object value) {
        getAccessor(property).setValue(parent, value);
    }
}
