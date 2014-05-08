package nl.gmt.rollbase.support;

import nl.gmt.rollbase.shared.schema.RbNode;

public interface RbVisitor {
    void visit(RbNode node);
}
