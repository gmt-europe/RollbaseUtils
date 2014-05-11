package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.merge.UuidRewriter;
import nl.gmt.rollbase.shared.schema.*;
import nl.gmt.rollbase.shared.schema.Properties;
import org.apache.commons.lang.Validate;
import org.jboss.logging.Logger;

import java.util.*;

public class RbObjectMap {
    private static final Logger LOG = Logger.getLogger(RbObjectMap.class);
    private static final List<RbReference> EMPTY_REFERENCES = Collections.emptyList();
    private static final Set<String> IGNORE_ID_DETECTION = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "Id", "OrigId", "OrderNo", "PageType"
    )));

    private final Application application;
    private final Map<String, RbObject> objects = new HashMap<>();
    private final RbIdMode idMode;
    //    private final Map<Long, Long> origIds = new HashMap<>();
    private long maxId = -1;
    private final Map<String, List<RbReference>> references = new HashMap<>();

    public RbObjectMap(Application application, RbIdMode idMode) throws RollbaseException {
        Validate.notNull(application, "application");
        Validate.notNull(idMode, "idMode");

        this.application = application;
        this.idMode = idMode;

        try {
            new ObjectWalker().visit(application);
            new ReferenceWalker(objects.keySet()).visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot create object map", e);
        }

        // Change the values of the references map into immutable references.

        for (String id : references.keySet()) {
            references.put(id, Collections.unmodifiableList(references.get(id)));
        }
    }

    public long getMaxId() {
        return maxId;
    }

    public Application getApplication() {
        return application;
    }

    public RbObject getObject(String id) {
        return objects.get(id);
    }

    private class ObjectWalker extends RbWalker {
        @Override
        public void visit(RbNode node) throws Exception {
            if (node instanceof RbObject) {
                RbObject object = (RbObject)node;

                // This is a sanity check; it should not be hit. The reason we exclude relationship def's is
                // because these are defined on both the source and target data object def.

                String id = object.getId();

                if (!(object instanceof RelationshipDef) && objects.containsKey(id)) {
                    throw new RollbaseException(String.format("Duplicate object ID '%s'", id));
                }

                objects.put(id, object);

                if (idMode == RbIdMode.LONG) {
                    long parsedId = Long.parseLong(id);
                    if (parsedId > maxId) {
                        maxId = parsedId;
                    }
                }
            }

            super.visit(node);
        }
    }

    private class ReferenceWalker extends RbWalker {
        private final Set<String> ids;

        private ReferenceWalker(Set<String> ids) {
            this.ids = ids;
        }

        @Override
        public void visit(RbNode node) throws Exception {
            if (node instanceof RbElement) {
                for (RbAccessor accessor : RbMetaModel.getMetaModel(node.getClass()).getAccessors()) {
                    Object value = accessor.getValue(node);

                    if (value instanceof Properties) {
                        processProperties(node, accessor, (Properties)value);
                    }

                    if (!(value instanceof String)) {
                        continue;
                    }

                    switch (accessor.getIdType()) {
                        case ID_REF:
                            addReference((String)value, node, accessor, null);
                            break;

                        case ID_REF_LIST:
                            List<String> ids = XmlUtils.parseIdList(node, accessor, idMode);
                            if (ids != null) {
                                for (String id : ids) {
                                    addReference(id, node, accessor, null);
                                }
                            }
                            break;

                        default:
                            if (!IGNORE_ID_DETECTION.contains(accessor.getName())) {
                                // Sanity check whether there are properties that contain number sequences that appear in
                                // the ID set. If so, we may be missing an RbIdReference.

                                if (XmlUtils.looksLikeIdList((String)value, idMode)) {
                                    for (String id : XmlUtils.parseIdList(node, accessor, idMode)) {
                                        if (this.ids.contains(id)) {
                                            LOG.warnf(
                                                "Property '%s' of element '%s' has a value '%s' that looks like an ID",
                                                accessor.getName(),
                                                node.getClass().getName(),
                                                value
                                            );
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
            }

            super.visit(node);
        }

        private void processProperties(RbNode node, RbAccessor accessor, Properties properties) throws RollbaseException {
            for (String key : XmlUtils.getPropertyKeys(properties)) {
                String value = XmlUtils.getProperty(properties, key);

                if (XmlUtils.looksLikeIdList(value, idMode)) {
                    for (String id : XmlUtils.getIds(value)) {
                        // There are many many properties that reference ID's. Instead of processing them all and
                        // look whether the contents looks like an ID and we have it in our list.

                        if (this.ids.contains(id)) {
                            addReference(id, node, accessor, key);
                        }
                    }
                }
            }
        }

        private void addReference(String id, RbNode node, RbAccessor accessor, String key) throws RollbaseException {
            if (idMode == RbIdMode.LONG) {
                if (Long.parseLong(id) <= 0) {
                    return;
                }
            } else {
                Long magicId = UuidRewriter.getMagicId(UUID.fromString(id));
                if (magicId != null && magicId <= 0) {
                    return;
                }
            }

            if (!ids.contains(id)) {
                // Permissions seem to reference stuff that does not exist.
                if (node instanceof Permission) {
                    return;
                }

                throw new RollbaseException(String.format(
                    "Found ID '%s' on object '%s' that does not reference a known object",
                    id,
                    node.getClass().getName()
                ));
            }

            List<RbReference> references = RbObjectMap.this.references.get(id);

            if (references == null) {
                references = new ArrayList<>();
                RbObjectMap.this.references.put(id, references);
            }

            references.add(new RbReference(id, (RbElement)node, accessor, key));
        }
    }
}
