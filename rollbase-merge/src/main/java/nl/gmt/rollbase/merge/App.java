package nl.gmt.rollbase.merge;

import nl.gmt.rollbase.logging.RedirectAppender;
import nl.gmt.rollbase.shared.RollbaseException;
import nl.gmt.rollbase.shared.RollbaseProject;
import nl.gmt.rollbase.shared.merge.JAXBUtils;
import nl.gmt.rollbase.shared.schema.Application;
import nl.gmt.rollbase.shared.schema.XmlUtils;
import org.apache.log4j.Level;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("org.jboss.logging.provider", "log4j");

        try {
            Arguments arguments = new Arguments(args);

            switch (arguments.getVerbosity()) {
                case INFO: RedirectAppender.setLevel(Level.INFO); break;
                case DEBUG: RedirectAppender.setLevel(Level.DEBUG); break;
            }

            switch (arguments.getMode()) {
                case LOAD: performLoad(arguments); break;
                case SAVE: performSave(arguments); break;
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static void performLoad(Arguments arguments) throws RollbaseException, JAXBException, IOException, TransformerException {
        Application application = new RollbaseProject(new File(arguments.getProject())).load();

        try (OutputStream os = new FileOutputStream(arguments.getFile())) {
            JAXBUtils.marshalFormatted(
                XmlUtils.createMarshaller(),
                application,
                new StreamResult(os)
            );
        }
    }

    private static void performSave(Arguments arguments) throws RollbaseException, JAXBException {
        Application application = (Application)XmlUtils.createUnmarshaller()
            .unmarshal(new File(arguments.getFile()));

        new RollbaseProject(new File(arguments.getProject())).save(application);
    }
}
