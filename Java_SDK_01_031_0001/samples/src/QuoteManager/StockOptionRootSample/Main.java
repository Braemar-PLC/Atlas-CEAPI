package QuoteManager.StockOptionRootSample;

import java.util.Map;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main entry point for the Stock Option Root Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {
    
    private static final String PROPERTIES_FILE = "/StockOptionRootSample.properties";
    
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_SYMBOLS = "-s";
    private static final String ARG_INPUT_FILE = "-in";    
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
    private String[] symbols;
    private String fileName;
    private String timeLimit;
    
    private SockType connType;

    private String display = "ON";
    private boolean displayON = true;
    
    private boolean useFile = false;
    
    public static void main(String[] args) {
        Main mainObj = new Main();
        
        mainObj.setDefaultValues();
        mainObj.initializeProperties();
        
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
        
        mainObj.displayHeader(factory);
        mainObj.displaySampleSettings();
        mainObj.startSample(factory);
    }
    
    private void setDefaultValues() {
        displayON = true;
        fileName = "";
        timeLimit = "0";
    }
    
    private void initializeProperties() {
        properties.setFileLocation(PROPERTIES_FILE);
        Properties sampleProps = properties.getProperties();
        properties.setHelpLocation(sampleProps.getProperty("samples.helpFile"));
        
        sampleVersion = sampleProps.getProperty("samples.stockoptionrootsample.version");
        sampleBuild = sampleProps.getProperty("samples.stockoptionrootsample.build");
        host = sampleProps.getProperty("samples.stockoptionrootsample.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        symbols = sampleProps.getProperty("samples.stockoptionrootsample.symbols").split(",");
    }
    
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
    
    private void validateArguments() {
        if (host.isEmpty() || username.isEmpty() || password.isEmpty() || symbols.length ==	0) {
            throw new IllegalArgumentException("Error: Missing required parameters.");
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
        sampleVersion = sampleProps.getProperty("samples.stockoptionrootsample.version");
        sampleBuild = sampleProps.getProperty("samples.stockoptionrootsample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        symbols = sampleProps.getProperty("samples.stockoptionrootsample.symbols").split(",");
        fileName = sampleProps.getProperty("samples.stockoptionrootsample.symbollist");
        timeLimit = sampleProps.getProperty("samples.stockoptionrootsample.timeLimit");
        displayON = "ON".equalsIgnoreCase(sampleProps.getProperty("samples.stockoptionrootsample.display"));
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
                           "|            ICE DATA SERVICES - STOCK OPTION ROOT SAMPLE                   |\n" +
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
        properties.displaySetting("Host Connection", host, 0);
        properties.displaySetting("Connection Type", tlsDescription, 0);
        properties.displaySetting("Symbols", symbols, 100);
    }
    
    private void startSample(ResourceManagerFactory factory) {
    	StockOptionRootSample stockOptionRootSample;
    	
        try {
            stockOptionRootSample = new StockOptionRootSample(
            								host, connType, username, password, symbols
            							);
            
            stockOptionRootSample.runStockOptionRootSample(factory);
        } catch (Exception e) {
            System.err.println("An error occurred while running the sample: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
