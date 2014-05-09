package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.shared.RbAccessor;
import nl.gmt.rollbase.shared.RbMetaModel;
import nl.gmt.rollbase.shared.RbWalker;
import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.schema.*;
import nl.gmt.rollbase.shared.schema.Process;
import org.apache.commons.lang.Validate;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

class Utils {
    private Utils() {
    }

    private static final Map<Class<? extends RbNode>, RbNodeHelper> HELPERS = new HashMap<>();
    private static <T extends RbNode> void addHelper(Class<T> klass, RbNodeHelper<T> helper) {
        HELPERS.put(klass, helper);
    }

    public static final Set<Class<?>> GENERATE_IN_FOLDER = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
        WebSite.class,
        DataObjectDef.class
    )));

    static {
        try {
            addHelper(Button.class, new DefaultNodeHelper<>(Button.class, "DisplayName"));
            addHelper(Chart.class, new DefaultNodeHelper<>(Chart.class, "ChartName"));
            addHelper(ConversionMap.class, new DefaultNodeHelper<>(ConversionMap.class, "MapName"));
            addHelper(DataObjectDef.class, new DefaultNodeHelper<>(DataObjectDef.class, "SingularName"));
            addHelper(DependentDataObjectDef.class, new DefaultNodeHelper<>(DependentDataObjectDef.class, "SingularName"));
            addHelper(Event.class, new DefaultNodeHelper<>(Event.class, "Name"));
            addHelper(Gauge.class, new DefaultNodeHelper<>(Gauge.class, "GaugeName"));
            addHelper(HostedFile.class, new DefaultNodeHelper<>(HostedFile.class, "DisplayName"));
            addHelper(ListView.class, new DefaultNodeHelper<>(ListView.class, "ViewName"));
            addHelper(Process.class, new DefaultNodeHelper<>(Process.class, "DisplayName"));
            addHelper(Question.class, new DefaultNodeHelper<>(Question.class, "ShortLabel"));
            addHelper(Report.class, new DefaultNodeHelper<>(Report.class, "Name"));
            addHelper(Role.class, new DefaultNodeHelper<>(Role.class, "Name"));
            addHelper(Template.class, new DefaultNodeHelper<>(Template.class, "Name"));
            addHelper(WebPageDef.class, new DefaultNodeHelper<>(WebPageDef.class, "PageDefName"));
            addHelper(WebSite.class, new DefaultNodeHelper<>(WebSite.class, "SiteName"));

            addHelper(Application.class, new SimpleNodeHelper<>(Application.class));
            addHelper(Menus.class, new SimpleNodeHelper<>(Menus.class));
            addHelper(Permissions.class, new SimpleNodeHelper<>(Permissions.class));
            addHelper(SeedRecords.class, new SimpleNodeHelper<>(SeedRecords.class));
            addHelper(Tokens.class, new SimpleNodeHelper<>(Tokens.class));
        } catch (RollbaseException e) {
            throw new RuntimeException("Cannot load helpers", e);
        }
    }

    public static RbNodeHelper getHelper(Class<?> type) {
        return HELPERS.get(type);
    }

    @SuppressWarnings("unchecked")
    public static String getNaturalName(RbNode node) {
        Validate.notNull(node, "node");

        RbNodeHelper helper = HELPERS.get(node.getClass());

        if (helper == null) {
            throw new IllegalArgumentException(String.format("Cannot find helper for type '%s'", node.getClass().getName()));
        }

        return helper.getNaturalName(node);
    }

    public static boolean isRootClass(Class<?> type) {
        return type.isAnnotationPresent(XmlRootElement.class);
    }

    private static interface RbNodeHelper<T extends RbNode> {
        String getNaturalName(T node);
    }

    private static class DefaultNodeHelper<T extends RbNode> implements RbNodeHelper<T> {
        private final RbAccessor accessor;

        private DefaultNodeHelper(Class<T> klass, String accessorName) throws RollbaseException {
            accessor = RbMetaModel.getMetaModel(klass).getAccessor(accessorName);
        }

        @Override
        public String getNaturalName(T node) {
            return (String)accessor.getValue(node);
        }
    }

    private static class SimpleNodeHelper<T extends RbNode> implements RbNodeHelper<T> {
        private final Class<T> klass;

        public SimpleNodeHelper(Class<T> klass) {
            this.klass = klass;
        }

        @Override
        public String getNaturalName(T node) {
            return klass.getSimpleName();
        }
    }

    public static void copy(RbMetaModel metaModel, Object source, Object destination) {
        for (RbAccessor accessor : metaModel.getAccessors()) {
            accessor.setValue(destination, accessor.getValue(source));
        }
    }

    public static boolean isIgnorableType(Class<?> type) {
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

    public static void sort(RbNode node) throws Exception {
        Validate.notNull(node, "node");

        new SortWalker().visit(node);
    }

    @SuppressWarnings("unchecked")
    private static class SortWalker extends RbWalker {
        @Override
        protected void visitList(RbNode parent, List list, RbAccessor accessor) throws Exception {
            if (list.size() > 0) {
                final RbNodeHelper helper = Utils.getHelper(list.get(0).getClass());

                if (helper != null) {
                    Collections.sort(list, new Comparator() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public int compare(Object a, Object b) {
                            int result = helper.getNaturalName((RbNode)a).compareToIgnoreCase(helper.getNaturalName((RbNode)b));

                            if (result == 0 && a instanceof RbObject) {
                                result = Integer.compare(
                                    Integer.parseInt(((RbObject)a).getId()),
                                    Integer.parseInt(((RbObject)b).getId())
                                );
                            }

                            return result;
                        }
                    });
                }
            }

            super.visitList(parent, list, accessor);
        }
    }
}
