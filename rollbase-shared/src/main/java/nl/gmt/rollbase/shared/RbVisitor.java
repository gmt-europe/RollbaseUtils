package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.RbNode;

public interface RbVisitor {
    void visit(RbNode node) throws Exception;
}
