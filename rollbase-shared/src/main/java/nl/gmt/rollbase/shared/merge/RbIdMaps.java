package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.merge.schema.ApplicationVersion;
import nl.gmt.rollbase.shared.merge.schema.ApplicationVersions;
import org.apache.commons.lang.Validate;

import java.util.*;

public class RbIdMaps {
    private final List<RbIdMap> idMaps;

    public static RbIdMaps fromSchema(ApplicationVersions schema) {
        Validate.notNull(schema, "schema");

        List<RbIdMap> idMaps = new ArrayList<>();

        for (ApplicationVersion version : schema.getVersion()) {
            idMaps.add(RbIdMap.fromSchema(version));
        }

        return new RbIdMaps(idMaps);
    }

    private RbIdMaps(List<RbIdMap> idMaps) {
        this.idMaps = Collections.unmodifiableList(idMaps);
    }

    public List<RbIdMap> getIdMaps() {
        return idMaps;
    }

    public List<RbMappedId> filter(UUID appId) {
        Validate.notNull(appId, "appId");

        Map<UUID, RbIdMap> idMaps = buildIdMapMap();

        List<RbMappedId> ids = new ArrayList<>();

        while (appId != null) {
            RbIdMap idMap = idMaps.get(appId);
            if (idMap == null) {
                throw new IllegalArgumentException(String.format(
                    "Cannot find IdMap for application version '%s'",
                    appId
                ));
            }

            ids.addAll(idMap.getMappedIds());

            appId = idMap.getParentAppId();
        }

        return ids;
    }

    private Map<UUID, RbIdMap> buildIdMapMap() {
        Map<UUID, RbIdMap> idMaps = new HashMap<>();

        for (RbIdMap idMap : this.idMaps) {
            idMaps.put(idMap.getAppId(), idMap);
        }

        return idMaps;
    }
}
