package nl.gmt.rollbase.shared.merge;

import nl.gmt.rollbase.shared.*;
import nl.gmt.rollbase.shared.schema.*;
import nl.gmt.rollbase.shared.schema.Properties;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class Exploder {
    private static final String INVALID_PATH_CHARS = "\\/:*?\"<>|";

    public void explode(Application application, FileWriter writer, RbIdMode idMode) throws RollbaseException {
        Validate.notNull(application, "application");
        Validate.notNull(writer, "writer");

        try {
            new SanitizationWalker().visit(application);
            new ExploderWalker(writer, application, idMode).visit(application);
        } catch (Exception e) {
            throw new RollbaseException("Cannot explode source", e);
        }
    }

    private static class ExploderWalker extends RbWalker {
        private final FileWriter writer;
        private final RbIdMode idMode;
        private final Stack<Level> levels = new Stack<>();
        private final RbObjectMap objectMap;

        private ExploderWalker(FileWriter writer, Application application, RbIdMode idMode) throws RollbaseException {
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
                    SchemaUtils.createMarshaller(),
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

            void remove(RbNode node) throws RollbaseException;
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
            public void remove(RbNode node) throws RollbaseException {
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
            public void remove(RbNode node) throws RollbaseException {
                int index = list.indexOf(node);

                if (index == -1) {
                    throw new RollbaseException("Expected node to be in the list");
                }

                list.remove(index);
            }
        }
    }

    private static class SanitizationWalker extends RbWalker {
        @Override
        public void visit(RbNode node) throws Exception {
            super.visit(node);

            RbMetaModel metaModel = RbMetaModel.getMetaModel(node.getClass());
            RbAccessor propertiesAccessor = metaModel.getAccessor("Props");

            if (propertiesAccessor != null) {
                Properties properties = (Properties)propertiesAccessor.getValue(node);

                // These properties are set here because they are set by an import. They do not appear in an export
                // of an application, but because they are set during an import, they will appear in an export after
                // an application has been imported. To make the exploded files more stable, we add them here.

                boolean hasIsManaged = metaModel.getKnownProperties().contains("isManaged");
                boolean hasDefProcess = metaModel.getKnownProperties().contains("defProcess");

                if ((hasIsManaged || hasDefProcess) && properties == null) {
                    properties = new Properties();
                    propertiesAccessor.setValue(node, properties);
                }

                if (hasIsManaged) {
                    setDefaultProperty(properties, "isManaged", node instanceof Application ? "0" : "false");
                }
                if (hasDefProcess) {
                    setDefaultProperty(properties, "defProcess", "-1");
                }

                // Exported properties do not have a deterministic order. Sort them here to make the export more
                // stable.

                if (properties != null) {
                    sortProperties(properties);
                }
            }
        }

        private void setDefaultProperty(Properties properties, String key, String value) throws RollbaseException {
            if (SchemaUtils.getProperty(properties, key) == null) {
                SchemaUtils.setProperty(properties, key, value);
            }
        }

        private void sortProperties(Properties properties) {
            Collections.sort(properties.getAny(), new Comparator<Object>() {
                @Override
                public int compare(Object a, Object b) {
                    return ((Element)a).getTagName().compareToIgnoreCase(((Element)b).getTagName());
                }
            });
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void visitList(RbNode parent, List list, RbAccessor accessor) throws Exception {
            // Sort lists that we can sort. The first sort level is the natural name. If the object doesn't specify a
            // natural name or it doesn't make the order unique, we fall back to the ID of the object.

            if (list.size() > 0) {
                Object firstObject = list.get(0);
                final RbNodeHelper helper = MergeUtils.getHelper((Class<? extends RbNode>)firstObject.getClass());

                if (helper != null || firstObject instanceof RbObject) {
                    Collections.sort(list, new Comparator() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public int compare(Object a, Object b) {
                            int result = 0;

                            if (helper != null) {
                                result = helper.getNaturalName((RbNode)a).compareToIgnoreCase(helper.getNaturalName((RbNode)b));
                            }

                            if (result == 0 && a instanceof RbObject) {
                                result = ((RbObject)a).getId().compareToIgnoreCase(((RbObject)b).getId());
                            }

                            return result;
                        }
                    });
                }
            }

            super.visitList(parent, list, accessor);
        }
    }
}
