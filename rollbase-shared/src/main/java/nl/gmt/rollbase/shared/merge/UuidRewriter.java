package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.merge.schema.ApplicationVersions;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.SchemaUtils;
import org.apache.commons.lang.Validate;

import java.util.UUID;

public abstract class UuidRewriter {
    public static final String PROPERTY_PREFIX = "__rbmerge__";
    public static final String APP_ID_KEY = PROPERTY_PREFIX + "appId";
    public static final UUID ZERO_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final UUID MIN_ONE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    ApplicationVersions.Version bumpVersion(Application application, UUID appId) throws RollbaseException {
        ApplicationVersions.Version version = new ApplicationVersions.Version();

        if (appId != null) {
            version.setParentAppId(appId.toString());
        }

        // Create a new UUID for the application.

        UUID newAppId = UUID.randomUUID();
        SchemaUtils.setProperty(application.getProps(), APP_ID_KEY, newAppId.toString());
        version.setAppId(newAppId.toString());

        // Bump the version.

        int appVersion = application.getVersion() + 1;
        application.setVersion(appVersion);
        version.setAppVersion(appVersion);
        return version;
    }

    public static Long getMagicId(UUID id) {
        Validate.notNull(id, "id");

        if (ZERO_ID.equals(id)) {
            return 0L;
        }
        if (MIN_ONE_ID.equals(id)) {
            return -1L;
        }

        return null;
    }
}
