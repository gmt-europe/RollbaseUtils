package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.*;
import nl.gmt.rollbase.shared.merge.schema.*;
import nl.gmt.rollbase.shared.schema.*;
import nl.gmt.rollbase.shared.schema.Properties;
import nl.gmt.rollbase.shared.schema.SchemaUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

public class UuidAdder extends UuidRewriter {
    public boolean addUuids(Application application, ApplicationVersions versions) throws RollbaseException {
        Validate.notNull(application, "application");
        Validate.notNull(versions, "versions");

        RbIdMaps idMaps = RbIdMaps.fromSchema(versions);

        // Get the current UUID of the application.

        UUID appId = null;
        String value = SchemaUtils.getProperty(application.getProps(), UuidRewriter.APP_ID_KEY);
        if (value != null) {
            appId = UUID.fromString(value);
        }

        // Load the mappings for the current application version.

        Map<Long, UUID> idMap;
        if (appId == null) {
            idMap = new HashMap<>();
        } else {
            idMap = loadMappedIds(idMaps.filter(appId));
        }

        // Load the map of id's to origId's.

        IdMapWalker idMapWalker = new IdMapWalker();

        try {
            idMapWalker.visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot load ID map", e);
        }

        // Perform the rewrite.

        UuidRemapperWalker walker = new UuidRemapperWalker(idMap, idMapWalker.map);

        try {
            walker.visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot add UUID's", e);
        }

        // Create a new version for the new mapped ID's. If no new ID's were created, we don't have to create
        // a new application version.

        if (walker.newIds.size() == 0) {
            return false;
        }

        ApplicationVersion version = bumpVersion(application, appId);

        // Write the mapped ID's.

        version.setIds(buildIdMap(walker.newIds));

        // And add the version to the list.

        versions.getVersion().add(version);

        // Signal that we modified the application and versions.

        return true;
    }

    private IdMap buildIdMap(Map<Long, UUID> ids) {
        IdMap idMap = new IdMap();
        List<IdMap.Id> mappedIds = idMap.getId();

        for (Map.Entry<Long, UUID> entry : ids.entrySet()) {
            IdMap.Id mappedId = new IdMap.Id();

            mappedId.setId(entry.getKey());
            mappedId.setMapped(entry.getValue().toString());

            mappedIds.add(mappedId);
        }

        Collections.sort(mappedIds, new Comparator<IdMap.Id>() {
            @Override
            public int compare(IdMap.Id a, IdMap.Id b) {
                return Long.compare(
                    a.getId(),
                    b.getId()
                );
            }
        });

        return idMap;
    }

    private static Map<Long, UUID> loadMappedIds(List<RbMappedId> mappedIds) throws RollbaseException {
        Map<Long, UUID> map = new HashMap<>();

        for (RbMappedId mappedId : mappedIds) {
            if (map.containsKey(mappedId.getFrom())) {
                throw new RollbaseException("Duplicate from ID");
            }

            map.put(mappedId.getFrom(), mappedId.getTo());
        }

        return map;
    }

    private static class IdMapWalker extends RbWalker {
        private final Map<Long, Long> map = new HashMap<>();

        @Override
        public void visit(RbNode node) throws Exception {
            if (node instanceof RbObject) {
                RbObject object = (RbObject)node;

                map.put(Long.parseLong(object.getId()), Long.parseLong(object.getOrigId()));
            }

            super.visit(node);
        }
    }

    private static class UuidRemapperWalker extends RbWalker {
        private final Map<Long, UUID> ids;
        private final Map<Long, Long> origIdMap;
        private final Map<Long, UUID> newIds = new HashMap<>();

        public UuidRemapperWalker(Map<Long, UUID> ids, Map<Long, Long> origIdMap) {
            this.ids = ids;
            this.origIdMap = origIdMap;
        }

        @Override
        public void visit(RbNode node) throws Exception {
            // Walk over all properties and rewrite as appropriate.

            for (RbAccessor accessor : RbMetaModel.getMetaModel(node.getClass()).getAccessors()) {
                Object value = accessor.getValue(node);

                if (value == null) {
                    continue;
                }

                if (value instanceof Properties) {
                    mapProperties(node, accessor);
                } else {
                    switch (accessor.getIdType()) {
                        case ID:
                        case ID_REF:
                            map(node, accessor, false);
                            break;

                        case ID_REF_LIST:
                            mapList(node, accessor, false);
                            break;

                        case ORIG_ID:
                            map(node, accessor, true);
                            break;
                    }
                }
            }

            super.visit(node);
        }

        private void mapProperties(RbNode node, RbAccessor accessor) throws RollbaseException {
            Properties properties = (Properties)accessor.getValue(node);

            for (String key : SchemaUtils.getPropertyKeys(properties)) {
                String value = SchemaUtils.getProperty(properties, key);

                // Does this look like it could contain an ID?

                if (!SchemaUtils.looksLikeIdList(value, RbIdMode.LONG)) {
                    continue;
                }

                List<String> ids = SchemaUtils.getIds(value);

                if ("0".equals(value) || "-1".equals(value)) {
                    continue;
                }

                // Do we have all ID's defined as an RbObject ID?

                boolean allMatch = true;
                for (String id : ids) {
                    long parsed = Long.parseLong(id);
                    if (parsed != 0 && parsed != -1 && !origIdMap.containsKey(parsed)) {
                        allMatch = false;
                        break;
                    }
                }

                if (!allMatch) {
                    continue;
                }

                // Map the ID's.

                StringBuilder sb = new StringBuilder();

                for (String id : ids) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(map(Long.parseLong(id), false, node).toString());
                }

                SchemaUtils.setProperty(properties, key, sb.toString());
            }
        }

        private void mapList(RbNode node, RbAccessor accessor, boolean isOrig) throws RollbaseException {
            StringBuilder sb = new StringBuilder();

            // When the parseIdList couldn't parse the ID's, but this is OK (i.e. RelationshipDef for dependant
            // object defs), we don't do anything.

            List<String> ids = SchemaUtils.parseIdList(node, accessor, RbIdMode.LONG);
            if (ids == null) {
                return;
            }

            for (String id : ids) {
                long parsed = Long.parseLong(id);
                UUID mapped = map(parsed, isOrig, node);

                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(mapped.toString());
            }

            accessor.setValue(node, sb.toString());
        }

        private void map(RbNode node, RbAccessor accessor, boolean isOrig) throws RollbaseException {
            long id = Long.parseLong((String)accessor.getValue(node));
            UUID mapped = map(id, isOrig, node);

            accessor.setValue(node, mapped.toString());
        }

        private UUID map(long id, boolean isOrig, RbNode node) throws RollbaseException {
            if (id == 0) {
                return ZERO_ID;
            }
            if (id == -1) {
                return MIN_ONE_ID;
            }

            if (!isOrig) {
                // Rewrite id's to their origId.

                Long origId = origIdMap.get(id);
                if (origId != null) {
                    id = origId;
                } else if (!(node instanceof Permission)) {
                    // Permission nodes reference system ID's.
                    throw new RollbaseException(String.format("Cannot find origId for id '%d'", id));
                }
            }

            UUID mapped = ids.get(id);

            if (mapped == null) {
                mapped = newIds.get(id);
            }

            if (mapped == null) {
                mapped = UUID.randomUUID();
                newIds.put(id, mapped);
            }

            return mapped;
        }
    }
}
