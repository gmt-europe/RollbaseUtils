package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.RbNode;
import nl.gmt.rollbase.shared.schema.RbObject;
import nl.gmt.rollbase.shared.RbWalker;
import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.Map;

public class RbObjectMap {
    private final Application application;
    private final Map<String, RbObject> objects = new HashMap<>();

    public RbObjectMap(Application application) throws Exception {
        Validate.notNull(application, "application");

        this.application = application;

        new Walker().visit(application);
    }

    public Application getApplication() {
        return application;
    }

    public RbObject getObject(String id) {
        return objects.get(id);
    }

    private class Walker extends RbWalker {
        @Override
        public void visit(RbNode node) throws Exception {
            if (node instanceof RbObject) {
                RbObject object = (RbObject)node;

                objects.put(object.getId(), object);
            }

            super.visit(node);
        }
    }
}
