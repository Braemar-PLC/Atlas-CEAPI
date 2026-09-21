package QuoteManager.Level1QuoteRequest;

import java.util.Map;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main entry point for the Level 1 Quote Request Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {

    private static final String PROPERTIES_FILE = "/Level1QuoteRequestSample.properties";
    
    // Constants for command-line arguments
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_MODE = "-m";
    private static final String ARG_INPUT_FILE = "-in";
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
    private String tlsConnectionMode;  // TLS connection mode from properties file or command-line
    private String tlsDescription;     // Connection type for display purposes
    private String mode;
    private String username;
    private String password;
    private String[] symbols;
    private String fileName;
    private String timeLimit;
    private String datasetType;
    
    private SockType connType;

    private String display = "ON";
    private boolean displayON = true;
    
    private boolean useFile = false;

    /**
     * Main entry point for the Level 1 Quote Sample application.
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
            return;
        }
        
        // Process command-line arguments
        if (mainObj.getCommandLineArguments(args) != 0) {
            System.out.println("Application could not process required arguments.");
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

        mainObj.setupDisplayAndMode();
        mainObj.displayHeader(factory);
        mainObj.displaySampleSettings();
        mainObj.startSample(factory);
    }

    /**
     * Sets default values for the application.
     */
    private void setDefaultValues() {
        displayON = true;
        fileName = "";
        timeLimit = "0";
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
    }

    /**
     * Loads properties from the properties file.
     * 
     * @param sampleProps The loaded properties
     */
    private void loadProperties(Properties sampleProps) {
        username = "";
        password = "";
        sampleVersion = sampleProps.getProperty("samples.quoterequestsample.version");
        sampleBuild = sampleProps.getProperty("samples.quoterequestsample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        mode = sampleProps.getProperty("samples.quoterequestsample.mode");
        symbols = sampleProps.getProperty("samples.quoterequestsample.symbols").split(",");
        fileName = sampleProps.getProperty("samples.quoterequestsample.symbollist");
        timeLimit = sampleProps.getProperty("samples.quoterequestsample.timeLimit");
        displayON = "ON".equalsIgnoreCase(sampleProps.getProperty("samples.quoterequestsample.display"));
        datasetType = sampleProps.getProperty("samples.quoterequestsample.datasetType");
    }

    /**
     * Processes parsed command-line arguments and overrides default values if provided.
     * 
     * @param parsedArgs Parsed arguments as a map
     */
    private void processCommandLineArguments(Map<String, String> parsedArgs) {
        String tmpSymbols = ""; 
        String tmpFileName = "";
        host = parsedArgs.getOrDefault(ARG_HOST, host);
        tlsConnectionMode = parsedArgs.getOrDefault(ARG_CONN, tlsConnectionMode);
        username = parsedArgs.getOrDefault(ARG_USERNAME, username);
        password = parsedArgs.getOrDefault(ARG_PASSWORD, password);
        mode = parsedArgs.getOrDefault(ARG_MODE, mode);
        tmpSymbols = parsedArgs.getOrDefault(ARG_SYMBOLS, tmpSymbols);
        tmpFileName = parsedArgs.getOrDefault(ARG_INPUT_FILE, tmpFileName);
        
        symbols = tmpSymbols.isEmpty() ? symbols : tmpSymbols.split(",");        
        fileName = tmpFileName.isEmpty() ? fileName : tmpFileName;
        
        if (fileName != null && !fileName.isEmpty() && tmpSymbols.isEmpty()) {
	        symbols = utilities.readSymbolsFromFile(fileName);
	        useFile = true;
	    }        
        
        display = parsedArgs.getOrDefault(ARG_DISPLAY, display);

        timeLimit = parsedArgs.getOrDefault(ARG_TIME_LIMIT, timeLimit);
        displayON = "ON".equalsIgnoreCase(display);
    }

    /**
     * Configures display and mode settings.
     */
    private void setupDisplayAndMode() {
        displayON = "ON".equalsIgnoreCase(display);
    }

    /**
     * Displays the application header and details.
     * 
     * @param factory Instance of ResourceManagerFactory
     */
    private void displayHeader(ResourceManagerFactory factory) {
        if (!displayON) {
            return;
        }
        
        String[] versionInfo = utilities.getJStandardVersion(factory);
        String apiVersion = versionInfo[0];
        String releaseDate = versionInfo[1];
        
        String[] javaInfo = utilities.getJavaVersion();
        String javaVersion = javaInfo[0];
        String javaVendor = javaInfo[1];
        String javaHome = javaInfo[2];

        System.out.println("-----------------------------------------------------------------------------\n" +
                           "|            ICE DATA SERVICES - LEVEL 1 QUOTE REQUEST SAMPLE               |\n" +
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

    /**
     * Displays sample settings.
     */
    private void displaySampleSettings() {
        if (!displayON) {
            return;
        }
        
        System.out.println("-----------------------------------------------------------------------------\n" +
                           "SAMPLE SETTINGS:\n" +
                           "-----------------------------------------------------------------------------");
        properties.displaySetting("Host Connection", host, 0);
        properties.displaySetting("Connection Type", tlsDescription, 0);
        properties.displaySetting("Request Mode", mode, 0);
        properties.displaySetting("Time Limit", "0".equals(timeLimit) ? "Unlimited" : timeLimit, 0);
        if (useFile) {
            properties.displaySetting("Symbol File", fileName, 0);
        } else {
            properties.displaySetting("Symbols", symbols, 100);
        }
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * Starts the sample application.
     * 
     * @param factory ResourceManagerFactory instance
     */
    private void startSample(ResourceManagerFactory factory) {
        Level1QuoteRequestSample quoteSample;

        quoteSample = new Level1QuoteRequestSample(
                               host, connType, username, password, 
                               mode, symbols, timeLimit, displayON, datasetType);
        try {
            quoteSample.runQuoteBoardSample(factory);
        } catch (Exception e) {
            System.err.println("An error occurred while running the sample: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
