package QuoteManager.QuoteBoardBundleSample;

import java.util.Map;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;

import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main entry point for the Quote Board Bundle Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {
    
    private static final String PROPERTIES_FILE = "/QuoteBoardBundleSample.properties";
    
    // Constants for command-line arguments
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_SYMBOLS = "-s";
    private static final String ARG_INPUT_FILE = "-in";
    private static final String ARG_CHAIN_TYPE = "-ct";
    private static final String ARG_MONTH = "-mf";
    private static final String ARG_DAY = "-df";
    private static final String ARG_YEAR = "-yf";
    private static final String ARG_PUTCALL = "-pc";
    private static final String ARG_MIN_STRIKE = "-min";
    private static final String ARG_MIN_STRIKE_OP = "-minop";
    private static final String ARG_MAX_STRIKE = "-max";
    private static final String ARG_MAX_STRIKE_OP = "-maxop";
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
    private String chainType;
    private String month;
    private String day;
    private String year;
    private String putcall;
    private String minStrike;
    private String minStrikeOp;
    private String maxStrike;
    private String maxStrikeOp;
    private String timeLimit;

    private SockType connType;

    private String display = "ON";
    private boolean displayON = true;
    
    private boolean useFile = false;
    
    /**
     * Main entry point for the Quote Board Bundle Sample application.
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
		if (!chainType.equalsIgnoreCase("FUTURE_CHAIN")) {
			if (month == null || month.isEmpty() || day == null || day.isEmpty() || year == null || year.isEmpty()
					|| putcall == null || putcall.isEmpty()) {
				throw new IllegalArgumentException("Error: Month, day, year, and put/call are required.");
			}
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
        sampleVersion = sampleProps.getProperty("samples.quoteboardbundlesample.version");
        sampleBuild = sampleProps.getProperty("samples.quoteboardbundlesample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");

        symbols = sampleProps.getProperty("samples.quoteboardbundlesample.symbols").split(",");
        chainType = sampleProps.getProperty("samples.quoteboardbundlesample.chaintype");
        month = sampleProps.getProperty("samples.quoteboardbundlesample.monthfilter");
        day = sampleProps.getProperty("samples.quoteboardbundlesample.dayfilter");
        year = sampleProps.getProperty("samples.quoteboardbundlesample.yearfilter");
        putcall = sampleProps.getProperty("samples.quoteboardbundlesample.putcallfilter");
        minStrike = sampleProps.getProperty("samples.quoteboardbundlesample.minstrike");
        minStrikeOp = sampleProps.getProperty("samples.quoteboardbundlesample.minstrikeoperator");
        maxStrike = sampleProps.getProperty("samples.quoteboardbundlesample.maxstrike");
        maxStrikeOp = sampleProps.getProperty("samples.quoteboardbundlesample.maxstrikeoperator");

        fileName = sampleProps.getProperty("samples.quoteboardbundlesample.symbollist");
        timeLimit = sampleProps.getProperty("samples.quoteboardbundlesample.timeLimit");
        displayON = "ON".equalsIgnoreCase(sampleProps.getProperty("samples.quoteboardbundlesample.display"));
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
        chainType = parsedArgs.getOrDefault(ARG_CHAIN_TYPE, chainType);
        month = parsedArgs.getOrDefault(ARG_MONTH, month);
        day = parsedArgs.getOrDefault(ARG_DAY, day);
        year = parsedArgs.getOrDefault(ARG_YEAR, year);
        putcall = parsedArgs.getOrDefault(ARG_PUTCALL, putcall);
        minStrike = parsedArgs.getOrDefault(ARG_MIN_STRIKE, minStrike);
        minStrikeOp = parsedArgs.getOrDefault(ARG_MIN_STRIKE_OP, minStrikeOp);
        maxStrike = parsedArgs.getOrDefault(ARG_MAX_STRIKE, maxStrike);
        maxStrikeOp = parsedArgs.getOrDefault(ARG_MAX_STRIKE_OP, maxStrikeOp);
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
                           "|            ICE DATA SERVICES - QUOTE BOARD BUNDLE SAMPLE                  |\n" +
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
        properties.displaySetting("Chain Type", chainType, 0);
        
        if (!chainType.equalsIgnoreCase("FUTURE_CHAIN")) {
            properties.displaySetting("Month Filter", month, 0);
            properties.displaySetting("Day Filter", day, 0);
            properties.displaySetting("Year Filter", year, 0);
            properties.displaySetting("Put/Call Filter", putcall, 0);
            properties.displaySetting("Min Strike", minStrike, 0);
            properties.displaySetting("Min Strike Operator", Integer.parseInt(minStrike) != -1 ? minStrikeOp : "not set", 0);
            properties.displaySetting("Max Strike", maxStrike, 0);
            properties.displaySetting("Max Strike Operator", Integer.parseInt(maxStrike) != -1 ? maxStrikeOp : "not set", 0);
            properties.displaySetting("Time Limit", "0".equals(timeLimit) ? "Unlimited" : timeLimit, 0);
        }
        if (useFile) {
            properties.displaySetting("Symbol File", fileName, 0);
        } else {
            properties.displaySetting("Symbols", symbols, 100);
        }
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * Starts the sample application.
     */
    private void startSample(ResourceManagerFactory factory) {
    	QuoteBoardBundleSample quoteBoardBundleSample;
    	
        try {
        	quoteBoardBundleSample = new QuoteBoardBundleSample(
        									host, connType, username, password, 
        									symbols, chainType, month, day, year, putcall, minStrike, minStrikeOp, maxStrike, maxStrikeOp,
        									timeLimit, displayON
        								 );
        	quoteBoardBundleSample.runQuoteBoardBundleSample(factory);
        } catch (Exception e) {
            System.err.println("An error occurred while running the sample: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
