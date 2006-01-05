package org.objectweb.celtix.tools.common.toolspec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.objectweb.celtix.tools.common.toolspec.parser.BadUsageException;
import org.objectweb.celtix.tools.common.toolspec.parser.CommandDocument;
import org.objectweb.celtix.tools.common.toolspec.parser.CommandLineParser;

public abstract class AbstractToolContainer implements ToolContainer {

    private static boolean isVerbose;
    private static String arguments[];

    protected ToolSpec toolspec;

    private boolean isQuiet;
    private CommandDocument commandDoc;
    private CommandLineParser parser;
    private OutputStream outOutputStream;
    private OutputStream errOutputStream;

    public class GenericOutputStream extends OutputStream {
        public void write(int b) throws IOException {

        }
    }

    public AbstractToolContainer(ToolSpec ts) throws BadUsageException {
        toolspec = ts;
    }

    public void setCommandLine(String[] args) throws BadUsageException {
        arguments = new String[args.length];
        System.arraycopy(args, 0, arguments, 0, args.length);
        setMode(args);
        if (isQuietMode()) {
            redirectOutput();
        }
        if (toolspec != null) {
            parser = new CommandLineParser(toolspec);
            commandDoc = parser.parseArguments(args);
        }
    }

    public void setMode(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-q")) {
                isQuiet = true;
            }
            if (args[i].equals("-quiet")) {
                isQuiet = true;
            }
            if (args[i].equals("-V")) {
                isVerbose = true;
            }
            if (args[i].equals("-verbose")) {
                isVerbose = true;
            }
        }
    }

    public void init() throws ToolException {
        // initialize
        if (toolspec == null) {
            throw new ToolException("Tool specification has to be set before initializing");
        }
    }

    public CommandDocument getCommandDocument() {
        return commandDoc;
    }

    public CommandLineParser getCommandLineParser() {
        return parser;
    }

    public void redirectOutput() {
        outOutputStream = new GenericOutputStream();
        errOutputStream = new GenericOutputStream();
        System.setErr(new PrintStream(errOutputStream));
        System.setOut(new PrintStream(outOutputStream));
    }

    public boolean isQuietMode() {
        return isQuiet;
    }

    public static boolean isVerboseMode() {
        return isVerbose;
    }

    public static String[] getArgument() {
        return arguments;
    }

    public OutputStream getOutOutputStream() {
        return outOutputStream;
    }

    public OutputStream getErrOutputStream() {
        return errOutputStream;
    }

    public abstract void execute(boolean exitOnFinish) throws ToolException;

}
