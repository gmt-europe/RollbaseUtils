package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.*;
import nl.gmt.rollbase.shared.merge.schema.ApplicationVersions;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.Properties;
import nl.gmt.rollbase.shared.schema.RbNode;
import nl.gmt.rollbase.shared.schema.XmlUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

public class UuidRemover extends UuidRewriter {
    public boolean removeUuids(Application application, ApplicationVersions versions) throws RollbaseException {
        Validate.notNull(application, "application");
        Validate.notNull(versions, "versions");

        RbIdMaps idMaps = RbIdMaps.fromSchema(versions);

        // Get the current UUID of the application.

        String value = XmlUtils.getProperty(application.getProps(), UuidRewriter.APP_ID_KEY);
        if (value == null) {
            throw new RollbaseException("Application does not have a UUID associated");
        }
        UUID appId = UUID.fromString(value);

        // Load the mappings for the current application version.

        Map<UUID, Long> idMap = loadMappedIds(idMaps.filter(appId));

        // Perform the rewrite.

        Walker walker = new Walker(idMap);

        try {
            walker.visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot remove UUID's", e);
        }

        // If we removed the ID's and there were UUID's that were not in the mapped ID's, it means that
        // the objects appeared because of a merge. If this happens, we need to update the application to
        // a new version.

        if (walker.newIds.size() == 0) {
            return false;
        }

        ApplicationVersions.Version version = bumpVersion(application, appId);

        // Write the mapped ID's.

        version.setIds(buildIdMap(walker.newIds));

        // And add the version to the list.

        versions.getVersion().add(version);

        // Signal that we modified the application and versions.

        return true;
    }

    private ApplicationVersions.Version.Ids buildIdMap(Map<UUID, Long> ids) {
        ApplicationVersions.Version.Ids idMap = new ApplicationVersions.Version.Ids();
        List<ApplicationVersions.Version.Ids.Id> mappedIds = idMap.getId();

        for (Map.Entry<UUID, Long> entry : ids.entrySet()) {
            ApplicationVersions.Version.Ids.Id mappedId = new ApplicationVersions.Version.Ids.Id();

            mappedId.setId(entry.getValue());
            mappedId.setMapped(entry.getKey().toString());

            mappedIds.add(mappedId);
        }

        Collections.sort(mappedIds, new Comparator<ApplicationVersions.Version.Ids.Id>() {
            @Override
            public int compare(ApplicationVersions.Version.Ids.Id a, ApplicationVersions.Version.Ids.Id b) {
                return Long.compare(
                    a.getId(),
                    b.getId()
                );
            }
        });

        return idMap;
    }

    public static Map<UUID, Long> loadMappedIds(List<RbMappedId> mappedIds) throws RollbaseException {
        Map<UUID, Long> map = new HashMap<>();

        for (RbMappedId mappedId : mappedIds) {
            if (map.containsKey(mappedId.getTo())) {
                throw new RollbaseException("Duplicate from ID");
            }

            map.put(mappedId.getTo(), mappedId.getFrom());
        }

        return map;
    }

    private static class Walker extends RbWalker {
        private final Map<UUID, Long> ids;
        private final Map<UUID, Long> newIds = new HashMap<>();
        private long maxId;

        public Walker(Map<UUID, Long> ids) {
            this.ids = ids;

            maxId = getMaxId(ids);
        }

        private long getMaxId(Map<UUID, Long> ids) {
            long max = -1;

            for (Long id : ids.values()) {
                if (id > max) {
                    max = id;
                }
            }

            // Id's up until 1000 seem to be system ID's. If we didn't have an ID, start with 1001.

            if (max == -1) {
                max = 1001;
            }

            return max;
        }

        @Override
        public void visit(RbNode node) throws Exception {
            // Walk over all properties and rewrite as appropriate.

            for (RbAccessor accessor : RbMetaModel.getMetaModel(node.getClass()).getAccessors()) {
                if (accessor.getValue(node) instanceof nl.gmt.rollbase.shared.schema.Properties) {
                    mapProperties(node, accessor);
                } else {
                    switch (accessor.getIdType()) {
                        case ID:
                        case ID_REF:
                        case ORIG_ID:
                            map(node, accessor);
                            break;

                        case ID_REF_LIST:
                            mapList(node, accessor);
                            break;
                    }
                }
            }

            super.visit(node);
        }

        private void mapProperties(RbNode node, RbAccessor accessor) throws RollbaseException {
            Properties properties = (Properties)accessor.getValue(node);

            for (String key : XmlUtils.getPropertyKeys(properties)) {
                String value = XmlUtils.getProperty(properties, key);

                // Does this look like it could contain an ID and it's not the appId?

                if (
                    APP_ID_KEY.equals(key) ||
                    !XmlUtils.looksLikeIdList(value, RbIdMode.UUID)
                ) {
                    continue;
                }

                // Map the ID's.

                StringBuilder sb = new StringBuilder();

                for (String id : XmlUtils.getIds(value)) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(Long.toString(map(UUID.fromString(id))));
                }

                XmlUtils.setProperty(properties, key, sb.toString());
            }
        }

        private void mapList(RbNode node, RbAccessor accessor) throws RollbaseException {
            StringBuilder sb = new StringBuilder();

            // When the parseIdList couldn't parse the ID's, but this is OK (i.e. RelationshipDef for dependant
            // object defs), we don't do anything.

            List<String> ids = XmlUtils.parseIdList(node, accessor, RbIdMode.UUID);
            if (ids == null) {
                return;
            }

            for (String id : ids) {
                UUID parsed = UUID.fromString(id);
                long mapped = map(parsed);

                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(Long.toString(mapped));
            }

            accessor.setValue(node, sb.toString());
        }

        private void map(RbNode node, RbAccessor accessor) {
            UUID id = UUID.fromString((String)accessor.getValue(node));
            long mapped = map(id);

            accessor.setValue(node, Long.toString(mapped));
        }

        private long map(UUID id) {
            Long mapped = getMagicId(id);
            if (mapped != null) {
                return mapped;
            }

            mapped = ids.get(id);

            if (mapped == null) {
                mapped = newIds.get(id);
            }

            if (mapped == null) {
                mapped = ++maxId;
                newIds.put(id, mapped);
            }

            return mapped;
        }
    }
}
