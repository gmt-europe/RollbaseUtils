package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.shared.RbAccessor;
import nl.gmt.rollbase.shared.RbMetaModel;
import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.schema.*;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jboss.logging.Logger;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;

public class Imploder {
    private static final Logger LOG = Logger.getLogger(Imploder.class);

    @SuppressWarnings("unchecked")
    public Application implode(File source) throws RollbaseException {
        Validate.notNull(source, "source");

        try {
            // We need to read the application separate because we need to have something to put everything into.

            String applicationFileName = "Application.xml";

            File file = new File(source, applicationFileName);
            if (!file.exists()) {
                for (String fileName : source.list(new SuffixFileFilter("_Application.xml"))) {
                    applicationFileName = fileName;
                    file = new File(source, fileName);
                    break;
                }
            }

            Application application = (Application)load(file);

            readLevel(application, source, applicationFileName);

            return application;
        } catch (JAXBException e) {
            throw new RollbaseException("Cannot load XML", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void readLevel(RbNode parent, File source, String ignore) throws RollbaseException, JAXBException {
        for (String fileName : source.list(new SuffixFileFilter(".xml", IOCase.INSENSITIVE))) {
            if (ignore != null && StringUtils.equalsIgnoreCase(fileName, ignore)) {
                continue;
            }

            File file = new File(source, fileName);
            Object object = load(file);

            RbAccessor accessor = findAccessor(parent.getClass(), object.getClass());
            if (accessor != null) {
                accessor.setValue(parent, object);
            } else {
                boolean isSet = false;

                // See whether we can put it into one of the collections.

                if (object instanceof DependentDataObjectDef) {
                    RbAccessor collectionAccessor = RbMetaModel.getMetaModel(parent.getClass()).getAccessor("DependentDefs");
                    accessor = findCollectionAccessor(collectionAccessor.getType(), DataObjectDef.class.getSimpleName());

                    if (accessor != null) {
                        // We need to convert this object to the correct type. The problem is that we had to convert the
                        // DependentDataObjectDefType to a DependentDataObjectDef because otherwise we would have
                        // issues with the schema. Here we convert it back.

                        DependentDataObjectDefType copy = new DependentDataObjectDefType();

                        Utils.copy(RbMetaModel.getMetaModel(DependentDataObjectDefType.class), object, copy);

                        isSet = true;
                        ((List)accessor.getValue(collectionAccessor.getValue(parent))).add(copy);
                    }
                } else {
                    for (RbAccessor collectionAccessor : RbMetaModel.getMetaModel(parent.getClass()).getAccessors()) {
                        // Don't let DataObjectDef's be assigned to the DependentDefs collection.

                        if (
                            Utils.isIgnorableType(collectionAccessor.getType()) ||
                            (object instanceof DataObjectDef && "DependentDefs".equals(collectionAccessor.getName()))
                        ) {
                            continue;
                        }

                        accessor = findCollectionAccessor(collectionAccessor.getType(), object.getClass().getSimpleName());

                        if (object instanceof DataObjectDef) {
                            int i = 0;
                        }

                        if (accessor != null) {
                            isSet = true;
                            ((List)accessor.getValue(collectionAccessor.getValue(parent))).add(object);
                            break;
                        }
                    }
                }

                if (!isSet) {
                    throw new RollbaseException(String.format("Cannot load '%s'", file));
                }
            }

            if (Utils.GENERATE_IN_FOLDER.contains(object.getClass())) {
                assert parent != object;

                parent = (RbNode)object;
            }
        }

        // Read the directories.

        for (String fileName : source.list(DirectoryFileFilter.DIRECTORY)) {
            readLevel(parent, new File(source, fileName), null);
        }
    }

    private RbAccessor findAccessor(Class<?> parent, Class<?> child) throws RollbaseException {
        for (RbAccessor accessor : RbMetaModel.getMetaModel(parent).getAccessors()) {
            if (accessor.getType() == child) {
                return accessor;
            }
        }

        return null;
    }

    private RbAccessor findCollectionAccessor(Class<?> parent, String name) throws RollbaseException {
        for (RbAccessor accessor : RbMetaModel.getMetaModel(parent).getAccessors()) {
            if (List.class.isAssignableFrom(accessor.getType()) && name.equals(accessor.getName())) {
                return accessor;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Object load(File file) throws JAXBException {
        LOG.infof("Loading '%s'", file);

        return XmlUtils.createUnmarshaller().unmarshal(file);
    }
}
