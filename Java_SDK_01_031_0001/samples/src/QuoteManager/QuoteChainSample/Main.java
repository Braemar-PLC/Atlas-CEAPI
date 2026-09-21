package QuoteManager.QuoteChainSample;

import java.util.Map;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main entry point for the Quote Chain Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {
    
    private static final String PROPERTIES_FILE = "/QuoteChainSample.properties";
    
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_MODE = "-req";
    private static final String ARG_SYMBOLS = "-s";
    private static final String ARG_INPUT_FILE = "-in";    
    private static final String ARG_CHAIN_TYPE = "-t";
    private static final String ARG_MONTH = "-m";
    private static final String ARG_YEAR = "-y";
    private static final String ARG_PUTCALL = "-pc";
    private static final String ARG_MIN_STRIKE = "-min";
    private static final String ARG_MAX_STRIKE = "-max";
    private static final String ARG_TIME_LIMIT = "-limit";
    private static final String ARG_DISPLAY = "-disp";
    
    private final SampleProperties properties = SampleProperties.getInstance();
    private final SampleUtilities utilities = SampleUtilities.getInstance();
    
    private String sampleVersion;
    private String sampleBuild;
    private String host;
    private String tlsConnectionMode;
    private String tlsDescription;
    private String username;
    private String password;
    private String mode;
    private String[] symbols;
    private String fileName;
    private String chainType;
    private String month;
    private String year;
    private String putcall;
    private String minStrike;
    private String maxStrike;
    private String timeLimit;
    private boolean isSnapshotRequest;

    private SockType connType;

    private String display = "ON";
    private boolean displayON = true;
    
    private boolean useFile = false;
    
    public static void main(String[] args) {
        Main mainObj = new Main();
        
        mainObj.setDefaultValues();
        
        ResourceManagerFactory factory;
        try {
            factory = ResourceManagerFactory.getFactory();
        } catch (ResourceManagerException e) {
            System.err.println("Error initializing ResourceManagerFactory: " + e.getMessage());
            return;
        }
        
        if (mainObj.getCommandLineArguments(args) != 0) {
            System.out.println("Application could not process required arguments.");
            return;
        }
        
        try {
            mainObj.validateArguments();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        
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
    
    private void setDefaultValues() {
        displayON = true;
        isSnapshotRequest = false;
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
     * Loads properties from the properties file.
     * 
     * @param sampleProps The loaded properties
     */
    private void loadProperties(Properties sampleProps) {
        username = "";
        password = "";
        sampleVersion = sampleProps.getProperty("samples.quotechainsample.version");
        sampleBuild = sampleProps.getProperty("samples.quotechainsample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");

        mode = sampleProps.getProperty("samples.quotechainsample.mode");
        symbols = sampleProps.getProperty("samples.quotechainsample.symbols").split(",");
        chainType = sampleProps.getProperty("samples.quotechainsample.chaintype");
        month = sampleProps.getProperty("samples.quotechainsample.monthfilter");
        year = sampleProps.getProperty("samples.quotechainsample.yearfilter");
        putcall = sampleProps.getProperty("samples.quotechainsample.putcallfilter");
        minStrike = sampleProps.getProperty("samples.quotechainsample.minstrike");
        maxStrike = sampleProps.getProperty("samples.quotechainsample.maxstrike");

        fileName = sampleProps.getProperty("samples.quotechainsample.symbollist");
        timeLimit = sampleProps.getProperty("samples.quotechainsample.timeLimit");
        displayON = "ON".equalsIgnoreCase(sampleProps.getProperty("samples.quotechainsample.display"));
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
        chainType = parsedArgs.getOrDefault(ARG_CHAIN_TYPE, chainType);
        month = parsedArgs.getOrDefault(ARG_MONTH, month);
        year = parsedArgs.getOrDefault(ARG_YEAR, year);
        putcall = parsedArgs.getOrDefault(ARG_PUTCALL, putcall);
        minStrike = parsedArgs.getOrDefault(ARG_MIN_STRIKE, minStrike);
        maxStrike = parsedArgs.getOrDefault(ARG_MAX_STRIKE, maxStrike);
        timeLimit = parsedArgs.getOrDefault(ARG_TIME_LIMIT, timeLimit);
        tmpSymbols = parsedArgs.getOrDefault(ARG_SYMBOLS, tmpSymbols);
        tmpFileName = parsedArgs.getOrDefault(ARG_INPUT_FILE, tmpFileName);
        
        symbols = tmpSymbols.isEmpty() ? symbols : tmpSymbols.split(",");        
        fileName = tmpFileName.isEmpty() ? fileName : tmpFileName;
        
        if (fileName != null && !fileName.isEmpty()) {
	        symbols = utilities.readSymbolsFromFile(fileName);
	        useFile = true;
	    }        
        
        display = parsedArgs.getOrDefault(ARG_DISPLAY, display);
    }
    
    private void validateArguments() {
        if (host.isEmpty() || username.isEmpty() || password.isEmpty() ) {
            throw new IllegalArgumentException("Error: Missing required parameters.");
        }
    }
    
    private void setupDisplayAndMode() {
        isSnapshotRequest = "Snapshot".equalsIgnoreCase(mode);
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
                           "|            ICE DATA SERVICES - QUOTE CHAIN SAMPLE                         |\n" +
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
        if (!displayON) {
            return;
        }

        System.out.println("-----------------------------------------------------------------------------\n" +
                           "SAMPLE SETTINGS:\n" +
                           "-----------------------------------------------------------------------------");
        properties.displaySetting("Host Connection", host, 0);
        properties.displaySetting("Connection Type", tlsDescription, 0);
        properties.displaySetting("Request Mode", mode, 0);
        properties.displaySetting("Chain Type", chainType, 0);
        properties.displaySetting("Month Filter", month, 0);
        properties.displaySetting("Year Filter", year, 0);
        properties.displaySetting("Put/Call Filter", putcall, 0);
        properties.displaySetting("Min Strike", minStrike, 0);
        properties.displaySetting("Max Strike", maxStrike, 0);
        properties.displaySetting("Time Limit", "0".equals(timeLimit) ? "Unlimited" : timeLimit, 0);
        
        if (useFile) {
            properties.displaySetting("Symbol File", fileName, 0);
        } else {
            properties.displaySetting("Symbols", symbols, 100);
        }
        System.out.println("-------------------------------------------------------------------------------");

    	
    	
    	properties.displaySetting("Host Connection", host, 0);
        properties.displaySetting("Connection Type", tlsDescription, 0);
        properties.displaySetting("Request Mode", mode, 0);
        properties.displaySetting("Chain Type", chainType, 0);
        properties.displaySetting("Symbols", symbols, 100);
    }
    
    private void startSample(ResourceManagerFactory factory) {
    	QuoteChainSample quoteChainSample;
    	
        try {
        	quoteChainSample = new QuoteChainSample(
        								host, connType, username, password, 
        								symbols, chainType, month, year, putcall, minStrike, maxStrike, 
        								timeLimit, isSnapshotRequest, displayON
        							);
        	
        	quoteChainSample.runQuoteChainSample(factory);
        } catch (Exception e) {
            System.err.println("An error occurred while running the sample: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
