package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.merge.schema.ApplicationVersion;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.SchemaUtils;
import org.apache.commons.lang.Validate;

import java.util.UUID;

public abstract class UuidRewriter {
    private static final String PROPERTY_PREFIX = "__rbmerge__";
    public static final String APP_ID_KEY = PROPERTY_PREFIX + "appId";
    static final UUID ZERO_ID = new UUID(0, 0);
    static final UUID MIN_ONE_ID = new UUID(0, -1);

    ApplicationVersion bumpVersion(Application application, UUID appId) throws RollbaseException {
        ApplicationVersion version = new ApplicationVersion();

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
