package TriggeredQuoteManager.SnapOnTriggerSample;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;

import Util.SampleProperties;
import Util.SampleUtilities;
/**
 * Main entry point for the Snap-on-Trigger Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {        
    private static final String PROPERTIES_FILE = "/SnapOnTriggerSample.properties";

    // Constants for command-line arguments
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_MODE = "-m";
    private static final String ARG_INPUT_FILE = "-in";
    private static final String ARG_SYMBOLS = "-s";
    private static final String ARG_NAME_TRIGGER = "-name";
    private static final String ARG_FIELD_IDS = "-ids";
    private static final String ARG_TIME_LIMIT = "-limit";   
    private static final String ARG_DISPLAY = "-disp";
    
    /* NOTE:
     * This sample allows only one trigger to be specified as a command-line
     * argument.
     * A trigger consists of one set of field IDs, one trigger name, and one set
     * of symbols.
     * To define multiple triggers, use the SnapOnTriggerSample.properties file.
     */

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
    private int numberOfTriggers;
    private String triggerName;
    private String[] fieldIDs;
    private String timeLimit;
    private String[] triggers;
    private String listRequest;
    private String shortList;
    private String detailedList;
    private boolean isListRequest;

    private SockType connType;

    private String display;
    private boolean displayON;
    
    /**
     * Creates an instance of a ResourceManagerFactory factory, parses command
     * line arguments and passes them to a new instance of SnapOnTriggerSample.
     * 
     * @param String[] args
     * <br>
     * The args string may array contain the following:
     * <br>
     * @param <host>           Specifies the address to use to connect to the ICE Data Services 
     *                         network. For example: cm*.dataservices.theice.com
     *
     * @param <connectionType> Specifies the type of connection to use with the ICE Data Services
     *                         network.  Valid values include:
     *                         
     *                            0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
     *                            1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
     *                            2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
     *                         
     *                         note: Currently, secured TLS connections are not suppoorted for the
     *                               Snap-on-Trigger service.
     *                         
     * @param <username>       The username for the account entitled to receive data from ICE Data Services.
     *                         If you do not have one, please contact your ICE Data Services representative.
     * @param <password>       The password for the account entitled to receive data from ICE Data Services.
     *                         If you do not have one, please contact your ICE Data Services representative.
     * @param <mode>           Identifies the type of request to make.  The following modes may be specified:
     *                         
     *                            - addTrigger
     *                              Adds each defined trigger to the Snap-on-Trigger service.
     *                              The service provides snapshot data each time a trigger's
     *                              specifications are met.
     *                              
     *                            - shortList
     *                              Lists the names of all active triggers.
     *                              
     *                            - detailedList
     *                              Lists all active triggers along with their details.
     *                                
     * @param <timeLimit>      The duration this sample will run before exiting.
     *                         Time duration applies to subscribe requests only.
     *                         Syntax for setting the time duration is:
     *                         
     *                            HH:MM:SS
     *                         
     *                         Setting the limit to 0 or 00:00:00 indicates to run
     *                         continuously without end; sample application must be
     *                         forcibly stopped.
     * 
     * @param <display>        Specifies the type of output to produce. 
     *                         True indicates format for console display; False indicates format for csv file.
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
        numberOfTriggers = 0;
        triggerName = "";
        timeLimit = "0";
        isListRequest = false;
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
        sampleVersion = sampleProps.getProperty("samples.snapontrigger.version");
        sampleBuild = sampleProps.getProperty("samples.snapontrigger.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        mode = sampleProps.getProperty("samples.snapontrigger.mode");
        symbols = sampleProps.getProperty("samples.snapontrigger.symbols").split(",");
        fileName = sampleProps.getProperty("samples.snapontrigger.symbollist");
        shortList = sampleProps.getProperty("samples.snapontrigger.short");
        detailedList = sampleProps.getProperty("samples.snapontrigger.detailed");
        
        // Check if this is a Trigger List request or a Trigger Request
        listRequest = sampleProps.getProperty("samples.snapontrigger.list");
        isListRequest = mode.contains("List");

        if (isListRequest) {
            listRequest += mode.equalsIgnoreCase("detailedList") ? detailedList : shortList;
        }

        timeLimit = sampleProps.getProperty("samples.snapontrigger.timeLimit");
        display = sampleProps.getProperty("samples.snapontrigger.display");

        // Load triggers from properties file
        try {
           numberOfTriggers = Integer.parseInt(sampleProps.getProperty("samples.snapontrigger.numTriggers"));
           triggers = new String[numberOfTriggers];
           
           for (int i = 0; i < numberOfTriggers; i++) {
              triggers[i] = sampleProps.getProperty("samples.snapontrigger.trigger" + (i + 1));
           }
        } catch (NumberFormatException nfe) {
           System.out.println("ERROR: Invalid number of triggers: " + numberOfTriggers);
           nfe.printStackTrace();
           System.exit(-1);
        }
    }

    /**
     * Processes parsed command-line arguments and overrides default values if provided.
     * 
     * @param parsedArgs Parsed arguments as a map
     */
    private void processCommandLineArguments(Map<String, String> parsedArgs) {
        String tmpSymbols = "";
        String tmpFieldIDs = "";
        
        host = parsedArgs.getOrDefault(ARG_HOST, host);
        tlsConnectionMode = parsedArgs.getOrDefault(ARG_CONN, tlsConnectionMode);
        username = parsedArgs.getOrDefault(ARG_USERNAME, username);
        password = parsedArgs.getOrDefault(ARG_PASSWORD, password);
        mode = parsedArgs.getOrDefault(ARG_MODE, mode);
        triggerName = parsedArgs.getOrDefault(ARG_NAME_TRIGGER, "");
        tmpFieldIDs = parsedArgs.getOrDefault(ARG_FIELD_IDS, "");
        tmpSymbols = parsedArgs.getOrDefault(ARG_SYMBOLS, tmpSymbols);
        fileName = parsedArgs.getOrDefault(ARG_INPUT_FILE, fileName);
        
        if (tmpSymbols.isEmpty()) {
            if (!fileName.isEmpty())
                symbols = utilities.readSymbolsFromFile(fileName);
        } else {
            symbols = tmpSymbols.split(",");
        }
        
        fieldIDs = tmpFieldIDs.isEmpty() ? fieldIDs : tmpFieldIDs.split(",");

        // Check if this is a Trigger List request or a Trigger Request
        isListRequest = mode.contains("List");

        if (isListRequest) {
            listRequest += mode.equalsIgnoreCase("detailedList") ? detailedList : shortList;
        } else {
            // Check if the client supplied a Trigger and Field IDs at the command-line
            if (!triggerName.isEmpty() && !tmpFieldIDs.isEmpty()) {
                // We can create a trigger that includes all the fields and instruments
                // specified.
                // 
                // note: The instruments do not need to be submitted at the command-line.
                numberOfTriggers = 1;
                // Clear the triggers array and create a new one
                triggers = new String[0];
                // Create a new trigger
                triggers = new String[1];
                
                triggers[0] = "{\"triggerName\":\"" + triggerName + "\",\"triggerID\":" + numberOfTriggers +
                              ",\"triggerType\":\"field\",\"symbols\":[";
                
                // Add all the instruments into the Trigger
                triggers[0] += "\"" + String.join("\",\"", symbols) + "\"";
                triggers[0] += "],\"fields\":[";
                // Add all the fields into the Trigger
                triggers[0] += String.join(",",fieldIDs);
                triggers[0] += "]}";
            } else {
                // NOTE: If the either the Trigger Name or the Field IDs are missing 
                //       from the command-line, the sample will use the settings from the 
                //       SnapOnTriggerSample.properties file.
                //
                // This sample requires that at least 1 Trigger is defined in the
                // SnapOnTriggerSample.properties file regardless of command-line arguments.
                // Processing the properties values takes place prior to command-line
                // argument processing.
            }
        }

        timeLimit = parsedArgs.getOrDefault(ARG_TIME_LIMIT, timeLimit);
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
                           "|            ICE DATA SERVICES - LEVEL 1 SNAP-ON-TRIGGER SAMPLE             |\n" +
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
        
        if (isListRequest)
            mode = mode.equalsIgnoreCase("detailedList") ? "Detailed List" : "Short List";
        else
            mode = "Add Trigger";
        
        properties.displaySetting("Request Mode", mode, 0);
        
		if (!isListRequest) {
	        properties.displaySetting("Trigger Requests", String.valueOf(numberOfTriggers), 0);

	        int reqNum = 0;
	        for (String triggeredRequest : triggers) {
	           System.out.printf("   %s. %s\n", ++reqNum, triggeredRequest);
	           
	           byte[] bytes = triggeredRequest.getBytes(StandardCharsets.UTF_8);
				if (bytes.length < (1024 *32)) {
					System.out.println("   Trigger Request Length: " + bytes.length + " bytes");
				} else {
					System.out.println("   Trigger Request Length: " + bytes.length + " bytes (too large to display)");
				}          	           
	        }                        
		}
		
		System.out.println();
        properties.displaySetting("Time Limit", "0".equals(timeLimit) ? "Unlimited" : timeLimit, 0);
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * Starts the sample application.
     * 
     * @param factory ResourceManagerFactory instance
     */
    private void startSample(ResourceManagerFactory factory) {
        SnapOnTriggerSample triggerQuoteSample;
        
        triggerQuoteSample = new SnapOnTriggerSample(
                                   host, connType, username, password, 
                                   triggers, timeLimit, listRequest, isListRequest,
                                   displayON
                               );

        try {
            triggerQuoteSample.runSnapOnTriggerSample(factory);
        } catch (Exception e) {
            System.err.println("An error occurred while running the sample: " + e.getMessage());
            e.printStackTrace();
        }
    }
}