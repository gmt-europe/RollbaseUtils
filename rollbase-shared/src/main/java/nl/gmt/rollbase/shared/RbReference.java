package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.RbElement;
import org.apache.commons.lang.Validate;

public class RbReference {
    private final String id;
    private final RbElement target;
    private final RbAccessor accessor;
    private final String propertyKey;

    public RbReference(String id, RbElement target, RbAccessor accessor, String propertyKey) {
        Validate.notNull(id, "id");
        Validate.notNull(target, "target");
        Validate.notNull(accessor, "accessor");

        this.id = id;
        this.target = target;
        this.accessor = accessor;
        this.propertyKey = propertyKey;
    }

    public String getId() {
        return id;
    }

    public RbElement getTarget() {
        return target;
    }

    public RbAccessor getAccessor() {
        return accessor;
    }

    public String getPropertyKey() {
        return propertyKey;
    }
}
