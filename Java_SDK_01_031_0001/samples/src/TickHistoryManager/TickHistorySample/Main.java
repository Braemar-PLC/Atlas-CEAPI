package TickHistoryManager.TickHistorySample;

import java.util.Map;
import java.util.Properties;

import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;

import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main entry point for the Tick History Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {

    private static final String PROPERTIES_FILE = "/TickHistorySample.properties";
    private static final String DICTIONARY_MODE_LABEL = "Flexible Field Dictionary";
    private static final String HISTORICAL_MODE_LABEL = "Historical Data";
    private static final String NO_OPTIONS_LABEL = "No options";

    // Constants for command-line arguments
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_MODE = "-m";
    private static final String ARG_DICT_VERSION = "-ver";
    private static final String ARG_BEGIN_DATE = "-b";
    private static final String ARG_END_DATE = "-e";
    private static final String ARG_INTERVAL = "-i";
    private static final String ARG_INPUT_FILE = "-in";
    private static final String ARG_SYMBOLS = "-s";
    private static final String ARG_OPTIONS = "-o";
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
    private String beginDate;
    private String endDate;
    private String interval;
    private String options;
    private String dictClientVersion;
    private String timeLimit;
    
    private SockType connType;
    private boolean dictionaryRequest = false;

    private String display = "ON";
    private boolean displayON = true;

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
        
        mainObj.setupDisplayAndMode();
        mainObj.displayHeader(factory);
        mainObj.displaySampleSettings();
        mainObj.startSample(factory);
    }

    /**
     * Sets default values for the application.
     */
    private void setDefaultValues() {
        dictionaryRequest = false;
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
        sampleVersion = sampleProps.getProperty("samples.tickhistorysample.version");
        sampleBuild = sampleProps.getProperty("samples.tickhistorysample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        mode = sampleProps.getProperty("samples.tickhistorysample.mode");
        symbols = sampleProps.getProperty("samples.tickhistorysample.symbols").split(",");
        fileName = sampleProps.getProperty("samples.tickhistorysample.symbollist");
        beginDate = sampleProps.getProperty("samples.tickhistorysample.beginDate");
        endDate = sampleProps.getProperty("samples.tickhistorysample.endDate");
        interval = sampleProps.getProperty("samples.tickhistorysample.interval");
        options = sampleProps.getProperty("samples.tickhistorysample.options");
        dictClientVersion = sampleProps.getProperty("samples.tickhistorysample.dictionaryVersion");
        timeLimit = sampleProps.getProperty("samples.tickhistorysample.timeLimit");
        displayON = "ON".equalsIgnoreCase(sampleProps.getProperty("samples.tickhistorysample.display"));
    }

    /**
     * Processes parsed command-line arguments and overrides default values if provided.
     * 
     * @param parsedArgs Parsed arguments as a map
     */
    private void processCommandLineArguments(Map<String, String> parsedArgs) {
    	String tmpSymbols = "";
        host = parsedArgs.getOrDefault(ARG_HOST, host);
        tlsConnectionMode = parsedArgs.getOrDefault(ARG_CONN, tlsConnectionMode);
        username = parsedArgs.getOrDefault(ARG_USERNAME, username);
        password = parsedArgs.getOrDefault(ARG_PASSWORD, password);
        mode = parsedArgs.getOrDefault(ARG_MODE, mode);
        dictClientVersion = parsedArgs.getOrDefault(ARG_DICT_VERSION, dictClientVersion);
        beginDate = parsedArgs.getOrDefault(ARG_BEGIN_DATE, beginDate);
        endDate = parsedArgs.getOrDefault(ARG_END_DATE, endDate);
        interval = parsedArgs.getOrDefault(ARG_INTERVAL, interval);
        tmpSymbols = parsedArgs.getOrDefault(ARG_SYMBOLS, tmpSymbols);
        fileName = parsedArgs.getOrDefault(ARG_INPUT_FILE, fileName);
        if (!fileName.isEmpty()) {
            symbols = utilities.readSymbolsFromFile(fileName);
		} else {
			symbols = tmpSymbols.isEmpty() ? symbols : tmpSymbols.split(",");
		}
        options = parsedArgs.getOrDefault(ARG_OPTIONS, options);
        display = parsedArgs.getOrDefault(ARG_DISPLAY, display);
    }

    /**
     * Configures display and mode settings.
     */
    private void setupDisplayAndMode() {
        displayON = "ON".equalsIgnoreCase(display);
        dictionaryRequest = "dictionary".equalsIgnoreCase(mode);
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
                           "|            ICE DATA SERVICES - INTRADAY HISTORY SERVICE SAMPLE            |\n" +
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
    	if (!displayON)
    		return;
    	
        System.out.println("-----------------------------------------------------------------------------\n" +
                           "SAMPLE SETTINGS:\n" +
                           "-----------------------------------------------------------------------------");
        properties.displaySetting("Host Connection", host, 0);
        properties.displaySetting("Connection Type", tlsDescription, 0);

        if (dictionaryRequest) {
        	properties.displaySetting("Request Mode", DICTIONARY_MODE_LABEL, 0);
        	properties.displaySetting("Client Version", dictClientVersion, 0);
        	properties.displaySetting("Begin Date", "not applicable (dictionary)", 0);
        	properties.displaySetting("End Date", "not applicable (dictionary)", 0);
        	properties.displaySetting("Interval", "not applicable (dictionary)", 0);
        	properties.displaySetting("Symbols", "not applicable (dictionary)", 0);
        	properties.displaySetting("Options", "not applicable (dictionary)", 0);
        } else {
        	properties.displaySetting("Request Mode", HISTORICAL_MODE_LABEL, 0);
        	properties.displaySetting("Begin Date", beginDate, 0);
        	properties.displaySetting("End Date", endDate, 0);
        	properties.displaySetting("Interval", interval, 0);

            if (!fileName.isEmpty()) {
            	properties.displaySetting("Symbol File", fileName, 0);
            } else {
            	properties.displaySetting("Symbols", symbols, 100);
            }

            if (!options.isEmpty()) {
                String dispOptions = options.replace(",", " ").replace("|", ",");
                properties.displaySetting("Options", dispOptions, 100);
            } else {
            	properties.displaySetting("Options", NO_OPTIONS_LABEL, 0);
            }
        }
        System.out.println("-----------------------------------------------------------------------------");
    }

    /**
     * Starts the sample application.
     * 
     * @param factory ResourceManagerFactory instance
     */
    private void startSample(ResourceManagerFactory factory) {
        TickHistorySample tickHistorySample;
        if (dictionaryRequest) {
            tickHistorySample = new TickHistorySample(host, connType, username, password, dictClientVersion, displayON);
        } else {
            tickHistorySample = new TickHistorySample(host, connType, username, password, beginDate, endDate, interval, options, symbols, displayON);
        }

        try {
            tickHistorySample.runTickHistorySample(factory);
        } catch (Exception e) {
            System.err.println("An error occurred while running the Tick History Sample: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
