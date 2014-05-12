package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.merge.UuidRewriter;
import nl.gmt.rollbase.shared.schema.*;
import nl.gmt.rollbase.shared.schema.Properties;
import org.apache.commons.lang.Validate;
import org.jboss.logging.Logger;

import java.util.*;

public class RbObjectMap {
    private static final Logger LOG = Logger.getLogger(RbObjectMap.class);
    private static final Set<String> IGNORE_ID_DETECTION = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "Id", "OrigId", "OrderNo", "PageType"
    )));

    private final Application application;
    private final Map<String, RbObject> objects = new HashMap<>();
    private final RbIdMode idMode;

    public RbObjectMap(Application application, RbIdMode idMode) throws RollbaseException {
        Validate.notNull(application, "application");
        Validate.notNull(idMode, "idMode");

        this.application = application;
        this.idMode = idMode;

        try {
            new ObjectWalker().visit(application);

            // Verify the ID's. This cross references id references to check that the object actually exists, and
            // does a sanity check on on the schema where it checks the contents of properties that are not marked
            // as an RbId but do look like an ID.

            new IdVerifierWalker(objects.keySet()).visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot create object map", e);
        }
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
            }

            super.visit(node);
        }
    }

    private class IdVerifierWalker extends RbWalker {
        private final Set<String> ids;

        private IdVerifierWalker(Set<String> ids) {
            this.ids = ids;
        }

        @Override
        public void visit(RbNode node) throws Exception {
            if (node instanceof RbElement) {
                for (RbAccessor accessor : RbMetaModel.getMetaModel(node.getClass()).getAccessors()) {
                    Object value = accessor.getValue(node);

                    if (value instanceof Properties) {
                        processProperties(node, (Properties)value);
                    }

                    if (!(value instanceof String)) {
                        continue;
                    }

                    switch (accessor.getIdType()) {
                        case ID_REF:
                            verifyId((String)value, node);
                            break;

                        case ID_REF_LIST:
                            List<String> ids = SchemaUtils.parseIdList(node, accessor, idMode);
                            if (ids != null) {
                                for (String id : ids) {
                                    verifyId(id, node);
                                }
                            }
                            break;

                        default:
                            if (!IGNORE_ID_DETECTION.contains(accessor.getName())) {
                                // Sanity check whether there are properties that contain number sequences that appear in
                                // the ID set. If so, we may be missing an RbIdReference.

                                if (SchemaUtils.looksLikeIdList((String)value, idMode)) {
                                    for (String id : SchemaUtils.parseIdList(node, accessor, idMode)) {
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

        private void processProperties(RbNode node, Properties properties) throws RollbaseException {
            for (String key : SchemaUtils.getPropertyKeys(properties)) {
                String value = SchemaUtils.getProperty(properties, key);

                if (SchemaUtils.looksLikeIdList(value, idMode)) {
                    for (String id : SchemaUtils.getIds(value)) {
                        // There are many many properties that reference ID's. Instead of processing them all and
                        // look whether the contents looks like an ID and we have it in our list.

                        if (this.ids.contains(id)) {
                            verifyId(id, node);
                        }
                    }
                }
            }
        }

        private void verifyId(String id, RbNode node) throws RollbaseException {
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
        }
    }
}
