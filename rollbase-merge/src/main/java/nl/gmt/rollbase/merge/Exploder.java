package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.shared.RbAccessor;
import nl.gmt.rollbase.shared.RbMetaModel;
import nl.gmt.rollbase.shared.RbWalker;
import nl.gmt.rollbase.shared.schema.*;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Exploder {
    private static final String INVALID_PATH_CHARS = "\\/:*?\"<>|";

    public void explode(Source source, FileWriter writer) throws MergeException {
        Validate.notNull(source, "source");
        Validate.notNull(writer, "writer");

        try {
            Application application = (Application)XmlUtils.createUnmarshaller().unmarshal(source);

            new Walker(writer, application).visit(application);
        } catch (Exception e) {
            throw new MergeException("Cannot explode source", e);
        }
    }

    private static class Walker extends RbWalker {
        private final FileWriter writer;
        private final Stack<Level> levels = new Stack<>();
        private final RbObjectMap objectMap;

        private Walker(FileWriter writer, Application application) throws MergeException {
            this.writer = writer;

            try {
                objectMap = new RbObjectMap(application);
            } catch (Exception e) {
                throw new MergeException("Cannot load object map", e);
            }
        }

        @Override
        public void visit(RbNode node) throws Exception {
            super.visit(node);

            // We only explode classes that have XmlRootElement defined.

            if (!shouldExplode(node)) {
                return;
            }

            // The DependentDataObjectDef needs special handling because the name of the object is equal to the name of
            // DataObjectDef. Because of this, we need to clone the objects into a new isntance and save them.

            if (node instanceof Application) {
                Application application = (Application)node;
                RbMetaModel metaModel = RbMetaModel.getMetaModel(DependentDataObjectDefType.class);

                for (DependentDataObjectDefType objectDefType : application.getDependentDefs().getDataObjectDef()) {
                    DependentDataObjectDef copy = new DependentDataObjectDef();

                    copy(metaModel, objectDefType, copy);

                    save(
                        copy,
                        new File(
                            DependentDataObjectDef.class.getSimpleName(),
                            Utils.getNaturalName(copy) + ".xml"
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

            if (Utils.GENERATE_IN_FOLDER.contains(node.getClass())) {
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

        private void copy(RbMetaModel metaModel, DependentDataObjectDefType source, DependentDataObjectDefType destination) {
            for (RbAccessor accessor : metaModel.getAccessors()) {
                accessor.setValue(destination, accessor.getValue(source));
            }
        }

        private void save(Object node, File fileName) throws IOException, JAXBException {
            String xml;

            try (StringWriter writer = new StringWriter()) {
                Marshaller marshaller = XmlUtils.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(node, writer);

                xml = writer.toString();
            }

            writer.writeFile(fileName, xml);
        }

        private File getNaturalName(RbNode node) throws MergeException {
            File result = new File(getFileName(Utils.getNaturalName(node)));

            // Web pages are associated with an object. Find the object and return a name that has both
            // the object name and the web page name.

            if (node instanceof WebPageDef) {
                WebPageDef webPageDef = (WebPageDef)node;

                if (!webPageDef.getObjDefId().equals("-1")) {
                    DataObjectDef dataObject = (DataObjectDef)objectMap.getObject(webPageDef.getObjDefId());

                    if (dataObject == null) {
                        throw new MergeException(String.format("Cannot resolve DataObjectDef '%s'", webPageDef.getObjDefId()));
                    }

                    result = new File(getFileName(dataObject.getSingularName()), result.getPath());
                }
            }

            return result;
        }

        private boolean shouldExplode(RbNode node) {
            return node.getClass().isAnnotationPresent(XmlRootElement.class);
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

        public File getPath() throws MergeException {
            File result = null;

            for (Level level : levels) {
                RbNode parent = level.getParent();

                // Put application into the root of the target folder.

                if (parent instanceof Application) {
                    continue;
                }

                if (shouldExplode(parent)) {
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

            void remove(RbNode node) throws InvocationTargetException, IllegalAccessException, MergeException;
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
            public void remove(RbNode node) throws InvocationTargetException, IllegalAccessException, MergeException {
                if (accessor.getValue(parent) != node) {
                    throw new MergeException("Expected current value of property to be equal to the node");
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
            public void remove(RbNode node) throws InvocationTargetException, IllegalAccessException, MergeException {
                int index = list.indexOf(node);

                if (index == -1) {
                    throw new MergeException("Expected node to be in the list");
                }

                list.remove(index);
            }
        }
    }
}
