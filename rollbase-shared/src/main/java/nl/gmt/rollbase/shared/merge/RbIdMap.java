package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.merge.schema.ApplicationVersions;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RbIdMap {
    private final UUID appId;
    private final UUID parentAppId;
    private final int appVersion;
    private final List<RbMappedId> mappedIds;

    static RbIdMap fromSchema(ApplicationVersions.Version version) {
        Validate.notNull(version, "version");

        UUID appId = UUID.fromString(version.getAppId());
        UUID parentAppId = null;
        if (version.getParentAppId() != null) {
            parentAppId = UUID.fromString(version.getParentAppId());
        }
        int appVersion = version.getAppVersion();

        List<RbMappedId> mappedIds = new ArrayList<>();
        parseMappedIds(version.getIds(), mappedIds);

        return new RbIdMap(appId, parentAppId, appVersion, mappedIds);
    }

    private static void parseMappedIds(ApplicationVersions.Version.Ids ids, List<RbMappedId> mappedIds) {
        for (ApplicationVersions.Version.Ids.Id id : ids.getId()) {
            mappedIds.add(new RbMappedId(
                id.getId(),
                UUID.fromString(id.getMapped())
            ));
        }
    }

    private RbIdMap(UUID appId, UUID parentAppId, int appVersion, List<RbMappedId> mappedIds) {
        Validate.notNull(appId, "appId");
        Validate.notNull(mappedIds, "mappedIds");

        this.appId = appId;
        this.parentAppId = parentAppId;
        this.appVersion = appVersion;
        this.mappedIds = Collections.unmodifiableList(mappedIds);
    }

    public UUID getAppId() {
        return appId;
    }

    public UUID getParentAppId() {
        return parentAppId;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public List<RbMappedId> getMappedIds() {
        return mappedIds;
    }
}
