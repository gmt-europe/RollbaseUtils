package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.schema.RbNode;

interface RbNodeHelper<T extends RbNode> {
    String getNaturalName(T node);
}
