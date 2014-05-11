package nl.gmt.rollbase.shared;

import nl.gmt.rollbase.shared.merge.*;
import nl.gmt.rollbase.shared.merge.FileWriter;
import nl.gmt.rollbase.shared.merge.schema.ApplicationVersions;
import nl.gmt.rollbase.shared.merge.schema.XmlUtils;
import nl.gmt.rollbase.shared.schema.Application;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class RollbaseProject {
    private final File project;

    public RollbaseProject(File project) {
        Validate.notNull(project, "project");

        this.project = project;
    }

    /**
     * Save a new version of the application into the project.
     *
     * @param application The application to save into the project.
     */
    public void save(Application application) throws RollbaseException {
        Validate.notNull(application, "application");

        // Add the UUID's to the application.

        ApplicationVersions versions = loadVersions(false);

        new UuidAdder().addUuids(application, versions);

        // Save the application and versions.

        try (FileWriter fileWriter = new FileWriter(project)) {
            // Save the exploded application.

            new Exploder().explode(application, fileWriter, RbIdMode.UUID);

            // Save the versions.

            try (Writer writer = new StringWriter()) {
                JAXBUtils.marshalFormatted(
                    XmlUtils.createMarshaller(),
                    versions,
                    new StreamResult(writer)
                );

                fileWriter.writeFile(new File(XmlUtils.FILE_NAME), writer.toString());
            }
        } catch (IOException | JAXBException | TransformerException e) {
            throw new RollbaseException("Cannot save project", e);
        }
    }

    /**
     * Load an application from the project.
     *
     * @return The loaded application.
     */
    public Application load() throws RollbaseException {
        // Implode the application form the project.

        Application application = new Imploder().implode(project);

        // Remove the UUID's from the loaded application.

        ApplicationVersions versions = loadVersions(true);

        boolean changed = new UuidRemover().removeUuids(application, versions);

        if (changed) {
            // If removing the UUID's generated a new version, we need to update the project. The versions are written
            // as normal, but the application ID also changed. We fix this by hand.

            try (OutputStream os = new FileOutputStream(new File(project, XmlUtils.FILE_NAME))) {
                JAXBUtils.marshalFormatted(
                    XmlUtils.createMarshaller(),
                    versions,
                    new StreamResult(os)
                );
            } catch (IOException | JAXBException | TransformerException e) {
                throw new RollbaseException("Cannot save project", e);
            }

            // And fix the appId and version of the application.

            updateApplicationVersion(
                application.getVersion(),
                nl.gmt.rollbase.shared.schema.XmlUtils.getProperty(application.getProps(), UuidRewriter.APP_ID_KEY)
            );
        }

        return application;
    }

    private void updateApplicationVersion(int version, String appId) throws RollbaseException {
        try {
            File file = new File(project, "Application.xml");

            Application application = (Application)nl.gmt.rollbase.shared.schema.XmlUtils.createUnmarshaller()
                .unmarshal(file);

            application.setVersion(version);
            nl.gmt.rollbase.shared.schema.XmlUtils.setProperty(application.getProps(), UuidRewriter.APP_ID_KEY, appId);

            try (OutputStream os = new FileOutputStream(file)) {
                JAXBUtils.marshalFormatted(
                    nl.gmt.rollbase.shared.schema.XmlUtils.createMarshaller(),
                    application,
                    new StreamResult(os)
                );
            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        } catch (IOException | JAXBException e) {
            throw new RollbaseException("Cannot update application", e);
        }
    }

    private ApplicationVersions loadVersions(boolean throwWhenMissing) throws RollbaseException {
        File file = new File(project, XmlUtils.FILE_NAME);

        if (!throwWhenMissing && !file.exists()) {
            return new ApplicationVersions();
        }

        try (InputStream is = new FileInputStream(file)) {
            return (ApplicationVersions)XmlUtils.createUnmarshaller().unmarshal(is);
        } catch (IOException | JAXBException e) {
            throw new RollbaseException("Cannot load ID map", e);
        }
    }
}
