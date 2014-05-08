package nl.gmt.rollbase.support;

import nl.gmt.rollbase.shared.schema.DataObject;
import nl.gmt.rollbase.shared.schema.RbNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class RbWalker implements RbVisitor {
    @Override
    public void visit(RbNode node) {
        visitChildren(node);
    }

    public void visitChildren(RbNode node) {
        for (Method method : getMethodsSorted(node.getClass())) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            Class<?> returnType = method.getReturnType();

            if (returnType.equals(Void.TYPE)) {
                continue;
            }

            if (RbNode.class.isAssignableFrom(returnType)) {
                RbNode childNode;

                try {
                    childNode = (RbNode)method.invoke(node);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RbWalkerException("Exception while invoking getter", e);
                }

                if (childNode != null) {
                    visit(childNode);
                }
            } else if (Iterable.class.isAssignableFrom(returnType)) {
                Iterable iterable;

                try {
                    iterable = (Iterable)method.invoke(node);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RbWalkerException("Exception while invoking getter", e);
                }

                if (iterable != null) {
                    for (Object object : iterable) {
                        if (object instanceof RbNode) {
                            visit((RbNode)object);
                        } else if (!isIgnorableType(object.getClass())) {
                            throw new RbWalkerException(String.format(
                                "List item of method '%s' of class '%s' has invalid type '%s'",
                                method.getName(),
                                node.getClass().getName(),
                                object.getClass().getName()
                            ));
                        }
                    }
                }
            } else if (!isIgnorableType(returnType)) {
                throw new RbWalkerException(String.format(
                    "Invalid return type '%s' of method '%s' of class '%s'",
                    returnType.getName(),
                    method.getName(),
                    node.getClass().getName()
                ));
            }
        }
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
            type == int.class ||
            type == long.class ||
            type == boolean.class ||
            type == byte[].class ||
            type == String.class ||
            // Property.getAny() returns a list of XML elements.
            org.w3c.dom.Element.class.isAssignableFrom(type) ||
            // Field doesn't inherit from RbNode because of a limitation in XSD.
            type == DataObject.Field.class;
    }
}
