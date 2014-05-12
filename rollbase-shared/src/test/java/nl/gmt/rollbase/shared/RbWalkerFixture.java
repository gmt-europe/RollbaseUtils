package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.schema.*;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

@RunWith(JUnit4.class)
public class RbWalkerFixture {
    @Test
    public void walk() throws Exception {
        Application application = (Application)SchemaUtils.createUnmarshaller().unmarshal(TestUtils.openCrmXml());

        new PrintWalker().visit(application);
    }

    private static class PrintWalker extends RbWalker {
        private int indent;

        @Override
        protected void visitChildren(RbNode node) throws Exception {
            indent++;

            super.visitChildren(node);

            indent--;
        }

        @Override
        public void visit(RbNode node) throws Exception {
            System.out.println(String.format(
                "%s%s", StringUtils.repeat("  ", indent), node.getClass().getSimpleName()
            ));

            super.visit(node);
        }
    }

    @Test
    public void idVisitor() throws Exception {
        Application application = (Application)SchemaUtils.createUnmarshaller().unmarshal(TestUtils.openCrmXml());

        IdWalker idWalker = new IdWalker();

        idWalker.visit(application);

        List<String> ids = new ArrayList<>(idWalker.ids);
        Collections.sort(ids);
        System.out.println("Ids: " + StringUtils.join(ids, ", "));

        List<String> origIds = new ArrayList<>(idWalker.origIds);
        Collections.sort(origIds);
        System.out.println("OrigIds: " + StringUtils.join(origIds, ", "));
    }

    private static class IdWalker extends RbWalker {
        private final Set<String> ids = new HashSet<>();
        private final Set<String> origIds = new HashSet<>();

        @Override
        public void visit(RbNode node) throws Exception {
            if (node instanceof RbObject) {
                String id = ((RbObject)node).getId();

                if (ids.contains(id)) {
                    //if (!RelationshipDef.class.isInstance(node)) {
                        System.err.println(String.format(
                            "Duplicate id '%s' of type '%s'",
                            id,
                            node.getClass().getName()
                        ));
                    //}
                } else {
                    ids.add(id);
                }

                // Don't care about duplicates on origId's.

                origIds.add(((RbObject)node).getOrigId());
            }

            super.visit(node);
        }
    }
}
