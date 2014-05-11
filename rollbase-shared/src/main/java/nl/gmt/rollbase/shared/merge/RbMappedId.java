package nl.gmt.rollbase.shared.merge;

import org.apache.commons.lang.Validate;

import java.util.UUID;

public class RbMappedId {
    private final long from;
    private final UUID to;

    public RbMappedId(long from, UUID to) {
        Validate.notNull(from, "from");
        Validate.notNull(to, "to");

        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public UUID getTo() {
        return to;
    }
}
