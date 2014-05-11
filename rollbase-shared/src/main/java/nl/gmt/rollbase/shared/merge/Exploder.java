package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.*;
import nl.gmt.rollbase.shared.schema.*;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

public class Exploder {
    private static final String INVALID_PATH_CHARS = "\\/:*?\"<>|";

    public void explode(Application application, FileWriter writer, RbIdMode idMode) throws RollbaseException {
        Validate.notNull(application, "application");
        Validate.notNull(writer, "writer");

        try {
            new Walker(writer, application, idMode).visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot explode source", e);
        }
    }

    private static class Walker extends RbWalker {
        private final FileWriter writer;
        private final RbIdMode idMode;
        private final Stack<Level> levels = new Stack<>();
        private final RbObjectMap objectMap;

        private Walker(FileWriter writer, Application application, RbIdMode idMode) throws RollbaseException {
            this.writer = writer;
            this.idMode = idMode;

            try {
                objectMap = new RbObjectMap(application, idMode);
            } catch (Exception e) {
                throw new RollbaseException("Cannot load object map", e);
            }
        }

        @Override
        public void visit(RbNode node) throws Exception {
            super.visit(node);

            // We only explode classes that have XmlRootElement defined.

            if (!MergeUtils.isRootClass(node.getClass())) {
                return;
            }

            // The DependentDataObjectDef needs special handling because the name of the object is equal to the name of
            // DataObjectDef. Because of this, we need to clone the objects into a new isntance and save them.

            if (node instanceof Application) {
                Application application = (Application)node;
                RbMetaModel metaModel = RbMetaModel.getMetaModel(DependentDataObjectDefType.class);

                for (DependentDataObjectDefType objectDefType : application.getDependentDefs().getDataObjectDef()) {
                    DependentDataObjectDef copy = new DependentDataObjectDef();

                    MergeUtils.copy(metaModel, objectDefType, copy);

                    save(
                        copy,
                        new File(
                            DependentDataObjectDef.class.getSimpleName(),
                            MergeUtils.getNaturalName(copy) + ".xml"
                        )
                    );
                }

                application.getDependentDefs().getDataObjectDef().clear();
            }

            File fileName = null;

            // Put the application XML in the root directory.

            if (!(
                node instanceof Application ||
                (levels.peek().getParent() instanceof Application && node instanceof RbCollection)
            )) {
                fileName = new File(getPath(), node.getClass().getSimpleName());
            }

            if (MergeUtils.GENERATE_IN_FOLDER.contains(node.getClass())) {
                fileName = new File(fileName, getNaturalName(node).getPath());
                fileName = new File(fileName, node.getClass().getSimpleName() + ".xml");
            } else {
                fileName = new File(fileName, getNaturalName(node).getPath() + ".xml");
            }

            save(node, fileName);

            // Remove the object from the current level. The object has now be stored in a separate file, so we
            // don't have to store it elsewhere.

            if (levels.size() > 0) {
                Level parentLevel = levels.size() > 1 ? levels.get(levels.size() - 2) : null;

                if (parentLevel instanceof ListLevel) {
                    parentLevel.remove(node);
                } else {
                    levels.peek().remove(node);
                }
            }
        }

        private void save(Object node, File fileName) throws IOException, JAXBException, TransformerException {
            String xml;

            try (StringWriter writer = new StringWriter()) {
                JAXBUtils.marshalFormatted(
                    XmlUtils.createMarshaller(),
                    node,
                    new StreamResult(writer)
                );

                xml = writer.toString();
            }

            writer.writeFile(fileName, xml);
        }

        private File getNaturalName(RbNode node) throws RollbaseException {
            File result = new File(getFileName(MergeUtils.getNaturalName(node)));

            // Web pages are associated with an object. Find the object and return a name that has both
            // the object name and the web page name.

            if (node instanceof WebPageDef) {
                WebPageDef webPageDef = (WebPageDef)node;

                Long objDefId;
                if (idMode == RbIdMode.LONG) {
                    objDefId = Long.parseLong(webPageDef.getObjDefId());
                } else {
                    objDefId = UuidRewriter.getMagicId(UUID.fromString(webPageDef.getObjDefId()));
                }

                if (objDefId == null || objDefId > 0) {
                    RbObject dataObject = objectMap.getObject(webPageDef.getObjDefId());

                    if (dataObject == null) {
                        throw new RollbaseException(String.format("Cannot resolve DataObjectDef '%s'", webPageDef.getObjDefId()));
                    }

                    String name;
                    if (dataObject instanceof DataObjectDef) {
                        name = ((DataObjectDef)dataObject).getSingularName();
                    } else {
                        name = ((DependentDataObjectDefType)dataObject).getSingularName();
                    }

                    result = new File(getFileName(name), result.getPath());
                }
            }

            return result;
        }

        @Override
        protected void visitChildren(RbNode node) throws Exception {
            super.visitChildren(node);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void visitList(RbNode parent, List list, RbAccessor accessor) throws Exception {
            levels.push(new ListLevel(parent, list));

            // We give super a copy of the list because we're modifying it.
            super.visitList(parent, new ArrayList(list), accessor);

            levels.pop();
        }

        @Override
        protected void visitChild(RbNode parent, RbNode child, RbAccessor accessor) throws Exception {
            levels.push(new ChildLevel(parent, accessor));

            super.visitChild(parent, child, accessor);

            levels.pop();
        }

        public File getPath() throws RollbaseException {
            File result = null;

            for (Level level : levels) {
                RbNode parent = level.getParent();

                // Put application into the root of the target folder.

                if (parent instanceof Application) {
                    continue;
                }

                if (MergeUtils.isRootClass(parent.getClass())) {
                    result = new File(result, parent.getClass().getSimpleName());
                    result = new File(result, getNaturalName(parent).getPath());
                }
            }

            return result;
        }

        private String getFileName(String naturalName) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < naturalName.length(); i++) {
                char c = naturalName.charAt(i);

                if (INVALID_PATH_CHARS.indexOf(c) != -1) {
                    sb.append('_');
                } else {
                    sb.append(c);
                }
            }

            return sb.toString();
        }

        private static interface Level {
            RbNode getParent();

            void remove(RbNode node) throws InvocationTargetException, IllegalAccessException, RollbaseException;
        }

        private static class ChildLevel implements Level {
            private final RbNode parent;
            private final RbAccessor accessor;

            private ChildLevel(RbNode parent, RbAccessor accessor) {
                this.parent = parent;
                this.accessor = accessor;
            }

            public RbNode getParent() {
                return parent;
            }

            @Override
            public void remove(RbNode node) throws InvocationTargetException, IllegalAccessException, RollbaseException {
                if (accessor.getValue(parent) != node) {
                    throw new RollbaseException("Expected current value of property to be equal to the node");
                }

                accessor.setValue(parent, null);
            }
        }

        private static class ListLevel implements Level {
            private final RbNode parent;
            private final List list;

            private ListLevel(RbNode parent, List list) {
                this.parent = parent;
                this.list = list;
            }

            public RbNode getParent() {
                return parent;
            }

            @Override
            public void remove(RbNode node) throws InvocationTargetException, IllegalAccessException, RollbaseException {
                int index = list.indexOf(node);

                if (index == -1) {
                    throw new RollbaseException("Expected node to be in the list");
                }

                list.remove(index);
            }
        }
    }
}
