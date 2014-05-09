package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.shared.RbAccessor;
import nl.gmt.rollbase.shared.RbMetaModel;
import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.schema.*;
import nl.gmt.rollbase.shared.schema.Process;
import org.apache.commons.lang.Validate;

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

    @SuppressWarnings("unchecked")
    public static String getNaturalName(RbNode node) {
        Validate.notNull(node, "node");

        RbNodeHelper helper = HELPERS.get(node.getClass());

        if (helper == null) {
            throw new IllegalArgumentException(String.format("Cannot find helper for type '%s'", node.getClass().getName()));
        }

        return helper.getNaturalName(node);
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
}
