package nl.gmt.rollbase.merge;

public class Arguments {
    private Mode mode;
    private String project;
    private String file;
    private Verbosity verbosity = Verbosity.WARN;

    public Arguments(String[] args) throws ArgumentsException {
        if (args.length < 1) {
            throw new ArgumentsException("Expected 'load' or 'save' as the first argument");
        }

        switch (args[0]) {
            case "load":
                parseLoadArguments(args);
                break;

            case "save":
                parseSaveArguments(args);
                break;

            default:
                throw unexpectedArgument(args[0]);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public String getProject() {
        return project;
    }

    public String getFile() {
        return file;
    }

    public Verbosity getVerbosity() {
        return verbosity;
    }

    private ArgumentsException unexpectedArgument(String arg) throws ArgumentsException {
        return new ArgumentsException(String.format("Unexpected argument '%s'", arg));
    }

    private void parseLoadArguments(String[] args) throws ArgumentsException {
        mode = Mode.LOAD;

        boolean expectProject = false;
        boolean expectTarget = false;

        for (int i = 1; i < args.length; i++) {
            if (expectProject) {
                project = args[i];
                expectProject = false;
            } else if (expectTarget) {
                file = args[i];
                expectTarget = false;
            } else {
                switch (args[i]) {
                    case "-p": expectProject = true; break;
                    case "-t": expectTarget = true; break;
                    default: parseArgument(args[i]); break;
                }
            }
        }

        if (expectProject || expectTarget) {
            throw new ArgumentsException("Missing argument");
        }
        if (project == null) {
            throw new ArgumentsException("Project is mandatory");
        }
        if (file == null) {
            throw new ArgumentsException("Target is mandatory");
        }
    }

    private void parseSaveArguments(String[] args) throws ArgumentsException {
        mode = Mode.SAVE;

        boolean expectProject = false;
        boolean expectSource = false;

        for (int i = 1; i < args.length; i++) {
            if (expectProject) {
                project = args[i];
                expectProject = false;
            } else if (expectSource) {
                file = args[i];
                expectSource = false;
            } else {
                switch (args[i]) {
                    case "-p": expectProject = true; break;
                    case "-s": expectSource = true; break;
                    default: parseArgument(args[i]); break;
                }
            }
        }

        if (expectProject || expectSource) {
            throw new ArgumentsException("Missing argument");
        }
        if (project == null) {
            throw new ArgumentsException("Project is mandatory");
        }
        if (file == null) {
            throw new ArgumentsException("Source is mandatory");
        }
    }

    private void parseArgument(String arg) throws ArgumentsException {
        switch (arg) {
            case "-v": verbosity = Verbosity.INFO; break;
            case "-vv": verbosity = Verbosity.DEBUG; break;
            default: throw unexpectedArgument(arg);
        }
    }
}
