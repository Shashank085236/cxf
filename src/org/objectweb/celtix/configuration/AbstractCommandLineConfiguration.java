package org.objectweb.celtix.configuration;

import java.util.ArrayList;
import java.util.Collection;

public class AbstractCommandLineConfiguration implements Configuration {

    Collection<CommandLineOption> options;
    
    // Configuration interface
    
    // methods available to concrete implementations
    
    protected AbstractCommandLineConfiguration() {
        options = new ArrayList<CommandLineOption>();
    }
    
    /**
     * Parses the arguments and initialises the options. 
     * 
     * @param args the command line arguments
     * @param consume specifies whether the command line options and their arguments
     * should be removed from the command line after processing.
     * @return boolean returns false if an unrecognised command line argument 
     * was found or no vaue or an in correct value was specified for an option
     * that requires arguments.
     */
    protected void parseCommandLine(String[] args, boolean consume) {
    }
    
    /**
     * Adds an option to the command line configuration. 
     * Typically invoked in static initializers.
     * 
     * @param option the <code>CommandLineOption</code> to add.
     */
    protected void addOption(CommandLineOption option) {
        options.add(option);
    }
    // private methods from here on
    
    /**
     * Returns the <code>CommandLineOption</code> identified by the name or 
     * null of no such option exists.
     * 
     * @param name identifies the option (shortcuts may be used)
     */
    private CommandLineOption getOption(String name) {
        /*
        for (CommandLineOption o : options) {
        }
        */
        return null;
    }

}
