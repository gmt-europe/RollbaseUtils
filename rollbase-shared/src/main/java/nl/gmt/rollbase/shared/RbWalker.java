package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.DataObject;
import nl.gmt.rollbase.shared.schema.RbNode;

import java.lang.reflect.Method;
import java.util.*;

public class RbWalker implements RbVisitor {
    @Override
    public void visit(RbNode node) throws Exception {
        visitChildren(node);
    }

    protected void visitChildren(RbNode node) throws Exception {
        RbMetaModel metaModel = RbMetaModel.getMetaModel(node.getClass());

        List<String> propertyNames = new ArrayList<>(metaModel.getProperties());
        Collections.sort(propertyNames);

        for (String propertyName : propertyNames) {
            RbAccessor accessor = metaModel.getAccessor(propertyName);
            Class<?> type = accessor.getType();

            if (RbNode.class.isAssignableFrom(type)) {
                RbNode childNode = (RbNode)accessor.getValue(node);

                if (childNode != null) {
                    visitChild(node, childNode, accessor);
                }
            } else if (List.class.isAssignableFrom(type)) {
                List list = (List)accessor.getValue(node);

                if (list != null) {
                    visitList(node, list, accessor);
                }
            } else if (!isIgnorableType(type)) {
                throw new RbWalkerException(String.format(
                    "Invalid return type '%s' of method '%s' of class '%s'",
                    type.getName(),
                    accessor.getName(),
                    node.getClass().getName()
                ));
            }
        }
    }

    protected void visitList(RbNode parent, List list, RbAccessor accessor) throws Exception {
        for (Object object : list) {
            if (object instanceof RbNode) {
                visitChild(parent, (RbNode)object, accessor);
            } else if (!isIgnorableType(object.getClass())) {
                throw new RbWalkerException(String.format(
                    "List item of method '%s' of class '%s' has invalid type '%s'",
                    accessor.getName(),
                    parent.getClass().getName(),
                    object.getClass().getName()
                ));
            }
        }
    }

    protected void visitChild(RbNode parent, RbNode child, RbAccessor accessor) throws Exception {
        visit(child);
    }

    private Method[] getMethodsSorted(Class<?> type) {
        Method[] methods = type.getMethods();

        Arrays.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method a, Method b) {
                return a.getName().compareTo(b.getName());
            }
        });

        return methods;
    }

    private boolean isIgnorableType(Class<?> type) {
        return
            type == Integer.class ||
            type == Long.class ||
            type == Boolean.class ||
            type == byte[].class ||
            type == String.class ||
            // Property.getAny() returns a list of XML elements.
            org.w3c.dom.Element.class.isAssignableFrom(type) ||
            // Field doesn't inherit from RbNode because of a limitation in XSD.
            type == DataObject.Field.class;
    }
}
