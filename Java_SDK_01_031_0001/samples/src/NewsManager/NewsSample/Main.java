package NewsManager.NewsSample;

import java.util.Map;
import java.util.Properties;

import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;

import Util.SampleProperties;
import Util.SampleUtilities;

/**
 * Main entry point for the News Sample application.
 * Handles command-line arguments, properties loading, and application execution.
 * 
 * @author ICE Data Services - Developer Support
 */
public class Main {

    private static final String PROPERTIES_FILE = "/NewsSample.properties";
    private static final String ARG_HOST = "-h";
    private static final String ARG_CONN = "-conn";
    private static final String ARG_USERNAME = "-u";
    private static final String ARG_PASSWORD = "-p";
    private static final String ARG_REQUEST_TYPE = "-r";
    private static final String ARG_SERVICES = "-ns";
    private static final String ARG_STORY_ID = "-s";
    private static final String ARG_STORY_FORMAT = "-sf";
    private static final String ARG_CATEGORIES = "-c";
    private static final String ARG_HEADLINE_COUNT = "-hc";
    private static final String ARG_BEGIN_TIME = "-b";
    private static final String ARG_END_TIME = "-e";
    private static final String ARG_HEADLINE_DAYS = "-hd";
    private static final String ARG_ACTIONS = "-actions";
    private static final String ARG_SEARCH_TEXT = "-sp";
    private static final String ARG_SEARCH_FIELDS = "-f";
    private static final String ARG_SEARCH_PHRASE = "-phrase";
    private static final String ARG_ALL_WORD_SEARCH = "-all";
    private static final String ARG_FULL_TEXT_SEARCH = "-fulltext";
    private static final String ARG_WHOLE_WORD_SEARCH = "-wholeword";
    private static final String ARG_MATCH_CASE = "-matchcase";
    private static final String ARG_SORT_ORDER = "-so";
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
    private String username;
    private String password;
    private String requestType;
    private String services;
    private String[] symbols;
    private String storyID;
    private String storyFormat;
    private String categories;
    private String headlineCount;
    private String beginTime;
    private String endTime;
    private String headlineDays;
    private String allActions;
    private String searchText;
    private String searchFields;
    private String isPhraseSearch;
    private String isAllWordSearch;
    private String isFullTextSearch;
    private String isWholeWordSearch;
    private String isMatchCaseSearch;
    private String sortOrder;
    private String timeLimit;
    
    private SockType connType;

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
        sampleVersion = sampleProps.getProperty("samples.newssample.version");
        sampleBuild = sampleProps.getProperty("samples.newssample.build");
        host = sampleProps.getProperty("samples.connection.host");
        tlsConnectionMode = sampleProps.getProperty("samples.connection.tls.mode");
        symbols = sampleProps.getProperty("samples.newssample.symbols").split(",");
        requestType = sampleProps.getProperty("samples.newssample.requesttype");
        services = sampleProps.getProperty("samples.newssample.services");
        storyID = sampleProps.getProperty("samples.newssample.storyID");
        storyFormat = sampleProps.getProperty("samples.newssample.storyFormat");
        categories = sampleProps.getProperty("samples.newssample.categories");
        headlineCount = sampleProps.getProperty("samples.newssample.headlineCount");
        beginTime = sampleProps.getProperty("samples.newssample.beginTime");
        endTime = sampleProps.getProperty("samples.newssample.endTime");
        headlineDays = sampleProps.getProperty("samples.newssample.headlineDays");
        allActions = sampleProps.getProperty("samples.newssample.subscribeAllActions");
        searchText = sampleProps.getProperty("samples.newssample.search");
        searchFields = sampleProps.getProperty("samples.newssample.searchFields");   
        isPhraseSearch = sampleProps.getProperty("samples.newssample.phrasequery");
        isAllWordSearch = sampleProps.getProperty("samples.newssample.matchAllWords");
        isFullTextSearch = sampleProps.getProperty("samples.newssample.fullTextQuery");
        isWholeWordSearch = sampleProps.getProperty("samples.newssample.matchWholeWord");
        isMatchCaseSearch = sampleProps.getProperty("samples.newssample.matchCase");
        sortOrder = sampleProps.getProperty("samples.newssample.sortOrder");        
        timeLimit = sampleProps.getProperty("samples.qualifiersparsingsample.timeLimit");
        displayON = "ON".equalsIgnoreCase(sampleProps.getProperty("samples.newssample.display"));
    }

    /**
     * Processes parsed command-line arguments and overrides default values if provided.
     * 
     * @param parsedArgs Parsed arguments as a map
     */
    private void processCommandLineArguments(Map<String, String> parsedArgs) {
    	Properties sampleProps = properties.getProperties();
        String tmpSymbols = "";
        host = parsedArgs.getOrDefault(ARG_HOST, host);
        tlsConnectionMode = parsedArgs.getOrDefault(ARG_CONN, tlsConnectionMode);
        username = parsedArgs.getOrDefault(ARG_USERNAME, username);
        password = parsedArgs.getOrDefault(ARG_PASSWORD, password);
        requestType = parsedArgs.getOrDefault(ARG_REQUEST_TYPE, requestType);
        timeLimit = parsedArgs.getOrDefault(ARG_TIME_LIMIT, timeLimit);
		if (requestType.equalsIgnoreCase("story")) {
	        storyID = parsedArgs.getOrDefault(ARG_STORY_ID, storyID);
		} else {			
			tmpSymbols = parsedArgs.getOrDefault(ARG_STORY_ID, sampleProps.getProperty("samples.newssample.symbols"));
			symbols = tmpSymbols.isEmpty() ? symbols : tmpSymbols.split(",");
		}
        services = parsedArgs.getOrDefault(ARG_SERVICES, services);

        storyFormat = parsedArgs.getOrDefault(ARG_STORY_FORMAT, storyFormat);	
        categories = parsedArgs.getOrDefault(ARG_CATEGORIES, categories);        
        headlineCount = parsedArgs.getOrDefault(ARG_HEADLINE_COUNT, headlineCount);
        beginTime = parsedArgs.getOrDefault(ARG_BEGIN_TIME, beginTime);
        endTime = parsedArgs.getOrDefault(ARG_END_TIME, endTime);
        headlineDays = parsedArgs.getOrDefault(ARG_HEADLINE_DAYS, headlineDays);
        allActions = parsedArgs.getOrDefault(ARG_ACTIONS, allActions);
        searchText = parsedArgs.getOrDefault(ARG_SEARCH_TEXT, searchText);
        searchFields = parsedArgs.getOrDefault(ARG_SEARCH_FIELDS, searchFields);
        isPhraseSearch = parsedArgs.getOrDefault(ARG_SEARCH_PHRASE, isPhraseSearch);
        isAllWordSearch = parsedArgs.getOrDefault(ARG_ALL_WORD_SEARCH, isAllWordSearch);
        isFullTextSearch = parsedArgs.getOrDefault(ARG_FULL_TEXT_SEARCH, isFullTextSearch);
        isWholeWordSearch = parsedArgs.getOrDefault(ARG_WHOLE_WORD_SEARCH, isWholeWordSearch);
        isMatchCaseSearch = parsedArgs.getOrDefault(ARG_MATCH_CASE, isMatchCaseSearch);
        sortOrder = parsedArgs.getOrDefault(ARG_SORT_ORDER, sortOrder);        
        display = parsedArgs.getOrDefault(ARG_DISPLAY, display);        
    }

    /**
     * Configures display and mode settings.
     */
    private void setupDisplayAndMode() {
        displayON = "ON".equalsIgnoreCase(display);
    }

	/**
     * Sets default values for the application.
     */
    private void setDefaultValues() {
    	displayON = true;
        timeLimit = "0";
    }

    /**
     * Validates essential arguments for application execution.
     */
    private void validateArguments() {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Error: Host is required.");
        }
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Error: Username and password are required.");
        }
        if (requestType == null || requestType.isEmpty()) {
            throw new IllegalArgumentException("Error: Request type is required.");
        }
    }

    /**
     * Configures the connection type.
     */
    private void configureConnectionType() {
        try {
            connType = SockType.getSockType(Integer.parseInt(tlsConnectionMode));
            switch (connType) {
                case SOCKTYPE_LEGACY:
                    tlsConnectionMode = "Standard TCP/IP (non-TLS)";
                    break;
                case SOCKTYPE_TLS:
                    tlsConnectionMode = "Secure TLS Connection";
                    break;
                case SOCKTYPE_TLS_PLUS:
                    tlsConnectionMode = "Secure TLS+ Not Supported; using Secure TLS Connection";
                    connType = SockType.SOCKTYPE_TLS;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid connection type.");
            }
        } catch (NumberFormatException e) {
            tlsConnectionMode = "Unknown Type Specified; using Standard TCP/IP (non-TLS)";
            connType = SockType.SOCKTYPE_LEGACY;
        }
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
                           "|            ICE DATA SERVICES - NEWS SAMPLE                                |\n" +
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
        System.out.println();
        properties.displaySetting("Request Type", requestType, 0);
        properties.displaySetting("Time Limit", "0".equals(timeLimit) ? "Unlimited" : timeLimit, 0);
        System.out.println();       
		if (requestType.equalsIgnoreCase("story")) {
			properties.displaySetting("Story ID", storyID, 0);
		} else {
			properties.displaySetting("News Services", services, 0);
			properties.displaySetting("Begin Time", "0".equals(beginTime) ? "Go back forever" : beginTime, 0);
			properties.displaySetting("End Time", "0".equals(endTime) ? "Time of request" : endTime, 0);
			properties.displaySetting("Return Amount", headlineCount, 0);
			properties.displaySetting("Days to Search", headlineDays, 0);
			properties.displaySetting("Categories", categories, 0);
			
			switch (Integer.parseInt(searchFields)) {
				case 1:
					properties.displaySetting("Search Fields", "Headline", 0);
					break;
				case 2:
					properties.displaySetting("Search Fields", "All", 0);
					break;
				case 4:
					properties.displaySetting("Search Fields", "Story", 0);
					break;
				case 8:
					properties.displaySetting("Search Fields", "Category", 0);
					break;
				case 16:
					properties.displaySetting("Search Fields", "Symbols", 0);
					break;
				default:
					properties.displaySetting("Search Fields", "** unknown **", 0);
					break;
			}
					
			properties.displaySetting("Search Text", searchText, 0);
			properties.displaySetting("Phrase", Boolean.parseBoolean(isPhraseSearch) ? "Search text is a phrase" : "Search text is not a phrase", 0);
            properties.displaySetting("Match All Words", Boolean.parseBoolean(isAllWordSearch) ? "Match all words" : "Match any word", 0);			
            properties.displaySetting("Full Text", Boolean.parseBoolean(isFullTextSearch) ? "Full text search" : "Partial text search", 0);
            properties.displaySetting("Match Whole Word", Boolean.parseBoolean(isWholeWordSearch) ? "Match whole word" : "Partial word match allowed", 0);
            properties.displaySetting("Match Case", Boolean.parseBoolean(isMatchCaseSearch) ? "Yes" : "No", 0);
			properties.displaySetting("Find", Boolean.parseBoolean(allActions) ? "All headlines/stories" : "New headlines/stories only", 0);
			
			switch (Integer.parseInt(sortOrder)) {
			    case 0:
			    	properties.displaySetting("Return Order", "Time Descending", 0);
			    	break;
				case 1:
					properties.displaySetting("Return Order", "Time Ascending", 0);
					break;
				case 2:
					properties.displaySetting("Return Order", "Relevance Scoring", 0);
					break;
				default:
					properties.displaySetting("Return Order", "** unknown **", 0);
					break;
			}

			properties.displaySetting("Symbols", symbols, 100);
		}
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * Starts the News Sample application.
     * 
     * @param factory ResourceManagerFactory instance
     * @throws Exception 
     */
    private void startSample(ResourceManagerFactory factory) {
        NewsSample newsSample = new NewsSample(	host, connType, username, password, requestType, services, storyID, symbols,
        										storyFormat, categories, headlineCount, beginTime, endTime, headlineDays, allActions, searchText,
        										searchFields, isPhraseSearch, isAllWordSearch, isFullTextSearch, isWholeWordSearch, isMatchCaseSearch,
        										sortOrder, timeLimit, displayON);
        try {            
        	newsSample.runNewsSample(factory);        
        } catch (Exception e) {
            System.err.println("Error running News Sample: " + e.getMessage());
        }
    }
}
