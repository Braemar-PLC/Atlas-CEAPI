package MarketDepthManager.MarketDepthSample;

import java.util.Map;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;

import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main class for retrieving market depth data.
 * Demonstrates how to parse command-line arguments and properties,
 * display version information, and execute the console application.
 * 
 * @author 
 *   <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 */
public class Main {

    private static final String PROPERTIES_FILE = "/MarketDepthSample.properties";

    // Constants for command-line arguments
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_MODE = "-m";
    private static final String ARG_TYPE = "-t";
    private static final String ARG_SOURCE = "-x";
    private static final String ARG_SYMBOLS = "-s";
    private static final String ARG_TIME_LIMIT = "-limit";
    private static final String ARG_DISPLAY = "-disp";

    // SampleProperties instance
    private final SampleProperties properties = SampleProperties.getInstance();
    
    // SampleUtilities instance
    private final SampleUtilities utilities = SampleUtilities.getInstance();
    
    // Application-specific fields
    private String sampleVersion;
    private String sampleBuild;
    private String host;
    private String tlsConnectionMode;
    private String tlsDescription;
    private String username;
    private String password;
    private String mode;
    private String type;
    private String source;
    private String symbols;
    private String timeLimit;
    private String display;
    private SockType connType;
    private boolean displayON = true;

    /**
     * Main entry point for the Market Depth Sample application.
     * Handles command-line arguments, properties loading, and application execution.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        Main mainObj = new Main();

        // Initialize default values
        mainObj.setDefaultValues();

        ResourceManagerFactory factory = null;
        
        try {
            factory = ResourceManagerFactory.getFactory();
        } catch (ResourceManagerException e) {
            System.err.println("Error initializing ResourceManagerFactory: " + e.getMessage());
            System.exit(-1);
        }

        // Process command-line arguments
        if (mainObj.getCommandLineArguments(args) != 0) {
            System.out.println("Application could not process arguments required to run.");
            return;
        }

        // Validate arguments
        try {
            mainObj.validateArguments();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        // Configure connection type
        StringBuilder sb = new StringBuilder();
        try {
            mainObj.connType = mainObj.utilities.setupConnectionType(mainObj.tlsConnectionMode, sb);
            mainObj.tlsDescription = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainObj.setupDisplayMode();
        mainObj.displayHeader(factory);
        mainObj.displaySampleSettings();
        mainObj.startSample(factory);
    }

    /**
     * Sets default values for the application.
     */
    private void setDefaultValues() {
        displayON = true;
    }

    /**
     * Retrieves command-line arguments and populates member variables.
     * 
     * @param args Command-line arguments
     * @return 0 if successful, -1 otherwise
     */
    private int getCommandLineArguments(String[] args) {
        properties.setFileLocation(PROPERTIES_FILE);

        Properties sampleProps = properties.getProperties();
        properties.setHelpLocation(sampleProps.getProperty("samples.helpFile"));
        properties.setArgs(args);

        // Load default values from properties file
        loadProperties(sampleProps);

        if (args.length == 0) {
            properties.displayHelp(true);
            return -1;
        }

        Map<String, String> parsedArgs = properties.parseArguments();
        processCommandLineArguments(parsedArgs);
        return 0;
    }

    /**
     * Validates essential arguments for application execution.
     */
    private void validateArguments() throws IllegalArgumentException {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Error: Host is required.");
        }
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Error: Username and password are required.");
        }
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Error: At least one symbol is required.");
        }
    }

    /**
     * Loads properties from the properties file.
     * 
     * @param sampleProps The loaded properties
     */
    private void loadProperties(Properties sampleProps) {
        username = "";
        password = "";
        sampleVersion = sampleProps.getProperty("samples.marketdepthsample.version");
        sampleBuild = sampleProps.getProperty("samples.marketdepthsample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        type = sampleProps.getProperty("samples.marketdepthsample.depthtype");
        source = sampleProps.getProperty("samples.marketdepthsample.source");
        symbols = sampleProps.getProperty("samples.marketdepthsample.symbols");
        mode = sampleProps.getProperty("samples.marketdepthsample.mode");
        timeLimit = sampleProps.getProperty("samples.marketdepthsample.timeLimit");
        display = sampleProps.getProperty("samples.marketdepthsample.display");
    }

    /**
     * Processes parsed command-line arguments and overrides default values if provided.
     * 
     * @param parsedArgs Parsed arguments as a map
     */
    private void processCommandLineArguments(Map<String, String> parsedArgs) {
        host = parsedArgs.getOrDefault(ARG_HOST, host);
        tlsConnectionMode = parsedArgs.getOrDefault(ARG_CONN, tlsConnectionMode);
        username = parsedArgs.getOrDefault(ARG_USERNAME, username);
        password = parsedArgs.getOrDefault(ARG_PASSWORD, password);
        mode = parsedArgs.getOrDefault(ARG_MODE, mode);
        type = parsedArgs.getOrDefault(ARG_TYPE, type);
        source = parsedArgs.getOrDefault(ARG_SOURCE, source);
        symbols = parsedArgs.getOrDefault(ARG_SYMBOLS, symbols);
        timeLimit = parsedArgs.getOrDefault(ARG_TIME_LIMIT, timeLimit);
        display = parsedArgs.getOrDefault(ARG_DISPLAY, display);
    }

    /**
     * Configures display settings.
     */
    private void setupDisplayMode() {
        displayON = "ON".equalsIgnoreCase(display);
    }

    /**
     * Displays the application header and details.
     * 
     * @param factory Instance of ResourceManagerFactory
     */
    private void displayHeader(ResourceManagerFactory factory) {
      if (!displayON)
         return;
      
        String[] versionInfo = utilities.getJStandardVersion(factory);
        String apiVersion = versionInfo[0];
        String releaseDate = versionInfo[1];
        
        String[] javaInfo = utilities.getJavaVersion();
        String javaVersion = javaInfo[0];
        String javaVendor = javaInfo[1];
        String javaHome = javaInfo[2];

        System.out.println("-----------------------------------------------------------------------------\n" +
                           "|            ICE DATA SERVICES - MARKET DEPTH SAMPLE                        |\n" +
                           "-----------------------------------------------------------------------------\n" +
             String.format("|            Version Date:  %-48s|\n", sampleVersion) +
             String.format("|                   Build:  %-48s|\n", sampleBuild) +
                           "|                                                                           |\n" +
             String.format("|             API Version:  %-48s|\n", apiVersion) +
             String.format("|            Release Date:  %-48s|\n", releaseDate) +
                           "|                                                                           |\n" +
             String.format("|            JAVA Version:  %-48s|\n", javaVersion) +
             String.format("|                  Vendor:  %-48s|\n", javaVendor) +
             String.format("|                    Home:  %-48s|\n", javaHome) +
                           "-----------------------------------------------------------------------------\n");
    }

    private void displaySampleSettings() {
        if (!displayON) return;
        properties.displaySetting("Host Connection", host, 0);
        properties.displaySetting("Connection Type", tlsDescription, 0);
        properties.displaySetting("Type", type, 0);
        properties.displaySetting("Source", source, 0);
        properties.displaySetting("Symbols", symbols, 100);
        properties.displaySetting("Mode", mode, 0);
        properties.displaySetting("Time Limit", "0".equals(timeLimit) ? "unlimited" : timeLimit, 0);
    }

    /**
     * Initializes and runs the MarketDepthSample application.
     * 
     * @param factory The ResourceManagerFactory object
     */
    private void startSample(ResourceManagerFactory factory) {    	
        MarketDepthSample depthSample = new MarketDepthSample(host, connType, username, password, type, source, symbols, mode, timeLimit, displayON);

        try {
        	depthSample.runMarketDepthSample(factory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
