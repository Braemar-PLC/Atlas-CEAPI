package QuoteManager.QualifiersParsingSample;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.DefaultQuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;
import Util.SampleRunner;
import Util.SampleUtilities;

/**
 * Provides methods for requesting and processing current snapshot or streaming data.
 * This sample is designed to provide basic market data output to screen or detailed
 * market data for redirection to a file.
 * 
 * When the display setting is ON, the sample will provide basic market data out
 * to the console window. When the display setting is OFF, the sample will provide
 * detailed market data in CSV format. For example:
 * 
 * java -cp .;JStandardSamples.jar;lib/* QuoteManager.QualifiersParsingSample.Main \
 * -h host -u user -p pwd -m subscribe -s ICE -limit 00:05:10 -disp off > detailedoutput.csv
 * 
 * @author ICE Data Services - Developer Support
 */
public class QualifiersParsingSample {
    // ---------------------------------------------------------------------------------------------
    // CLASS FIELDS
    // ---------------------------------------------------------------------------------------------
    
    // The Level 1 Service enables a connection to SUBSCRIBE to the maximum number of symbols
    // the connected account is entitled to access. Although there is no strict limitation
    // on the number of SNAPSHOT requests a client can send simultaneously, this sample
    // enforces a fixed batch size to prevent overloading the server and to ensure optimal
    // application efficiency and control.
    //
    // Defines the maximum number of concurrent SNAPSHOT requests allowed in a batch.
    private static final int MAX_CONCURRENT_REQUESTS = 100;
    private String            m_appStartTime                   = "";

    // Network and authentication
    private String            m_host                           = "";
    private SockType          m_connType                       = SockType.SOCKTYPE_LEGACY;
    private String            m_username                       = "";
    private String            m_password                       = "";

    // Request and symbol-related fields
    private String[]          m_symbols;
    private String            m_mode;
    private String            m_limit;

    // Interval and concurrency
    private long              m_count                          = 0;    // Completed requests
    private long              m_activeRequests                 = 0;    // Active requests
    private int               m_index                          = 0;    // For batching logic
    
    private ArrayList<Integer> m_qualFilters                   = new ArrayList<>();

    private boolean           m_displayOutput                  = true;
    private int               m_displayUnknownFields           = 0;   // 0 = do not include, 1 = include, 2 = display unknown fields only

    // Manager and listener
    private QuoteManager                quoteManager;
    private MyConsoleQuoteListener      consoleQuoteListener;

    // Request object
    private QuoteRequest                quoteRequest;
    private long                        turnAroundValue = 20000L;

    // Track no-data responses; applies to SNAPSHOT requests
    private int               m_numRequestsWithNoRecords = 0;

    // Properties file
    Properties sampleProps = SampleProperties.getInstance().getProperties();

    // SampleUtilities instance
    private final SampleUtilities utilities = SampleUtilities.getInstance();    

    // ---------------------------------------------------------------------------------------------
    // CONSTRUCTORS
    // ---------------------------------------------------------------------------------------------

    /**
     * Constructor for QualifiersParsingSample.
     * 
     * @param host         Connection string for ICE Data Services network.
     * @param connType     Connection type (Legacy, TLS, TLS_PLUS).
     * @param username     Account username.
     * @param password     Account password.
     * @param mode         Request mode (snapshot or subscribe).
     * @param symbols      Array of symbols to request data for.
     * @param qualfilters  Up to 10 qualfier IDs.
     * @param displayUnknownFields  Display unknown fields (0 = no, 1 = yes, 2 = display only).
     * @param timeLimit    Duration the sample will run for subscribe mode.
     * @param display      Display format (true for console, false for CSV).
     */
    public QualifiersParsingSample(
                String host, SockType connType, String username, String password, 
                String mode, String[] symbols, String qualfilters, int displayUnknownFields,
                String timeLimit, boolean display
    ) {
        this.m_host          = host;
        this.m_connType      = connType;
        this.m_username      = username;
        this.m_password      = password;
        this.m_mode          = mode;
        this.m_symbols       = symbols;
        this.m_limit         = timeLimit;
        this.m_displayOutput = display;
        this.m_displayUnknownFields = displayUnknownFields;
        processQualFilterOption(qualfilters);
        
    }

    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY Main -- START
     * ---------------------------------------------------------------------------------------------
     */
    /**
     * Initializes and runs the QuoteManager sample.
     * 
     * @param factory ResourceManagerFactory to create QuoteManager.
     * @throws Exception if an error occurs during setup or connection.
     */
    public void runQuoteBoardSample(ResourceManagerFactory factory) throws Exception {
        try {
            // Check if the client will use Amazon Web Services
            if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useAWS"))) {
                UsernameInfo info = factory.acquireUsername(utilities.getConnectionSettings(m_host, m_username, m_password, m_connType, sampleProps));
                
                if (info.getStatusEnum() == DBCAPI.SUCCESS) {
                    this.m_username = info.getUsername();
                    this.m_password = info.getPassword();
                    System.out.println("account acquired . . . ");
                } else {
                    DBCAPI_ERROR errorCode = DBCAPI_ERROR.valueOf(info.getStatusEnum().get().name());
                    switch (errorCode) {
                        case INVALIDNAME:
                            System.out.println("\nThe host specified is not valid. Verify the host value and try again.");
                            exitSample(false);
                            break;
                        case NOT_ENTITLED:
                            System.out.println("\nThe account specified is not entitled for the AWS service.");
                            exitSample(false);
                            break;
                        case WRONG_USERNAMEPASSWORD:
                            System.out.println("\nThe account username and/or password is not recognized.");
                            exitSample(false);
                            break;
                        default:
                            System.out.println("\nAn error occurred when attempting to acquire the username:  " + info.getStatusEnum());
                            exitSample(false);
                            break;
                    }
                }
            }

            // Create Quote Manager, Prepare the Request, and Connect
            this.quoteManager = factory.createQuoteManager();
            ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
            
            // Prepare the request object
            prepareQuoteRequests();
            
            quoteManager.connect(utilities.getConnectionSettings(m_host, m_username, m_password, m_connType, sampleProps), consoleConnectionListener);

        } catch (ResourceManagerException rme) {
            rme.printStackTrace();
        }
    }
    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY Main -- END
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * Processes the `qualfilter` option to filter updates based on specified qualifier IDs.
     *
     * @param value A string of qualifier IDs separated by pipes (|). If empty or null, no filters are added.
     */
    private void processQualFilterOption(String value) {
        if (value == null || value.isEmpty()) {
            // No qualifiers specified; m_qualFilters remains empty.
            return;
        }

        try {
            String[] qualIds = value.split("\\|");
            for (String id : qualIds) {
                m_qualFilters.add(Integer.parseInt(id.trim()));
            }
        } catch (Exception e) {
            System.out.println("Error processing QUALFILTER: " + e.getMessage());
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleQuoteListener -- START
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * Checks if data should be displayed in the console.
     * 
     * @return True if console display is enabled, false otherwise.
     */
    public boolean displayData() {
        return this.m_displayOutput;
    }

    /**
     * Prepares and submits quote requests based on the specified mode.
     */
    public void requestData() {
        if (this.m_mode.equalsIgnoreCase("snapshot")) {
            requestSnapshotData(getCurrentIndex());
        } else {
            subscribeToData();
        }
    }

    /**
     * Sets the number of requested that returned no data.
     */
    public void setNumberRequestsWithNoRecords(int trackRequestsWithoutData) {
        this.m_numRequestsWithNoRecords = trackRequestsWithoutData;
    }
    
    /**
     * Retrieves the number of requests that returned no data.
     * @return The count of requests with no data.
     */
    public int getNumberRequestsWithNoRecords() {
        return this.m_numRequestsWithNoRecords;
    }
    
    /**
     * Retrieves the current placement (index) in the symbols list.
     * This value represents the starting point for the next batch of requests.
     * 
     * @return The current index in the symbols list.
     */
    public int getCurrentIndex() {
        return this.m_index;
    }
    
    /**
     * Provides the list of qualifiers supplied to the application.
     * The sample will display data that includes those qualifiers only.
     *
     * @return qualiferFilters
     */
    public ArrayList<Integer> getQualifierFilters() {
        return m_qualFilters;
    }
    
	/**
	 * Determines if the application should include unknown fields as part of the output, or
	 * display them only.
	 * 
	 * @return an int.
	 */
	public int displayUnknownFields() {
		return m_displayUnknownFields;
	}

    public String markTime() {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM-dd-yyyy / HH:mm:ss");
        return dateTimeFormat.format(new Date());
    }
   
   /**
    * Called from MyConsoleQuoteListener onComplete event.
    * This method increments the number of completed SNAPSHOT requests and disconnects from the
    * Level 1 Quote service if all SNAPSHOT requests have been processed.
    */
   public void setDataComplete() {
       // Increment number of completed requests
       ++this.m_count;
       --this.m_activeRequests;
       if (this.m_activeRequests < MAX_CONCURRENT_REQUESTS) {
         requestData();
       }
       
       // Determine if all SNAPSHOT requests have been processed, and if so, disconnect
       // from the Level 1 server
       if (this.m_count >= (this.m_symbols.length)) {
          if (displayData()) {
              System.out.println("-------------------------------------------------------------------------------");
              System.out.println("SAMPLE START TIME: " + m_appStartTime);
              System.out.println("-------------------------------------------------------------------------------");
              System.out.printf("             TOTAL SYMBOLS: %d\n", this.m_symbols.length);
              System.out.printf("TOTAL SYMBOLS WITH NO DATA: %d\n", getNumberRequestsWithNoRecords());
              System.out.println("-------------------------------------------------------------------------------");
              System.out.printf("  SAMPLE END TIME: %s\n", markTime());
              System.out.println("-------------------------------------------------------------------------------");
          }
          else {
              System.out.println("REQUESTS WITHOUT DATA," + getNumberRequestsWithNoRecords() + " of " + this.m_symbols.length);
          }        
          try {
              exitSample(true);
           } catch (Exception e) {
              e.printStackTrace();
           }
       }
   }
   
    /**
     * Requests snapshot data for each symbol.
     * The number of concurrent requests is limited to MAX_CONCURRENT_REQUESTS.
     * The Level 1 service doesn't impose this limit.  This is a self-governed
     * limit by this sample application to prevent overloading the server and
     * to ensure optimal application efficiency and control.
     *
     * @param index The starting index for requests in the symbol list.
     */
    private void requestSnapshotData(int index) {
        if (index == 0)
            m_appStartTime = markTime();
    
        for (int i = index; i < m_symbols.length; i++) {
            String symbol = m_symbols[i];
            try {
                // Submit the request and update counters
                quoteManager.request(symbol, consoleQuoteListener, quoteRequest);
                quoteRequest.setTurnAround(++turnAroundValue);
                m_activeRequests++;
                
                if (m_activeRequests >= MAX_CONCURRENT_REQUESTS) {
                    // Pause submitting new requests until one completes
                    m_index = i + 1;
                    return;
                }
            } catch (Exception e) {
                System.out.println("Error submitting request for symbol: " + symbol + " - " + e.getMessage());
            }
        }
    
        // All requests have been submitted
        m_index = m_symbols.length;
    }

    /**
     * Subscribes to streaming data for each symbol.
     */
    private void subscribeToData() {
        if (m_index == 0)
            m_appStartTime = markTime();
    
        for (String symbol : m_symbols) {
            try {
                quoteManager.subscribe(symbol, consoleQuoteListener);
            } catch (Exception e) {
                System.out.println("Error subscribing to data for symbol: " + symbol + " - " + e.getMessage());
            }
        }

        if (!this.m_limit.equalsIgnoreCase("0")) {
            SampleRunner.runSampleForTime(this.m_limit, this);
        }
    }

    /**
     * This method disconnects from the Level 1 service when the run-time limit
     * is reached.
     */   
    public void setTimerComplete() {
        if (displayData()) {
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println("SAMPLE START TIME: " + m_appStartTime);
            System.out.println("-------------------------------------------------------------------------------");
            System.out.println("-------------------------------------------------------------------------------");
            System.out.printf( "SAMPLE END TIME: %s\n", markTime());
            System.out.println("-------------------------------------------------------------------------------");
        }
        try {
            this.exitSample(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   

    /**
     * Disconnects from the QuoteManager and exits the sample.
     * 
     * @param disconnect If true, disconnect before exiting.
     */
    public synchronized void exitSample(boolean disconnect) {
        if (disconnect) {
            if (displayData()) {
            	
            	if (!this.m_mode.equalsIgnoreCase("snapshot")) {
            		System.out.println("-------------------------------------------------------------------------------");            		
                    for (String symbol : m_symbols) {
                        try {
                            quoteManager.unsubscribe(symbol, consoleQuoteListener);
                        } catch (Exception e) {
                            System.out.println("Error unsubscribing to data for symbol: " + symbol +  e.getMessage());
                        }
                    }                    
            	}            	
                // The sample maintains the connection until the user presses Enter.
                // The reason for maintaining the connection is to allow clients and support to verify connectivity is needed.
                System.out.println("");
                System.out.println("Exiting application...");
                System.out.println("(press Enter to exit)");
                System.out.println("-------------------------------------------------------------------------------");                                
                
                try {
                    System.in.read();
                } catch (IOException e) {
                	e.printStackTrace();
                }  
                
                try {
                	this.quoteManager.disconnect();
                } catch (ResourceManagerException rme) {
                    rme.printStackTrace();
                }
            }
        }
        System.exit(0);
    }
    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleQuoteListener -- END
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * Prepares quote requests by initializing required parameters and settings.
     */
    private void prepareQuoteRequests() {
        // Add any necessary initialization or setup logic for quote requests here.
        turnAroundValue = 20000L; // Reset turnaround value
        consoleQuoteListener = new MyConsoleQuoteListener(this);
        quoteRequest = new DefaultQuoteRequest(turnAroundValue);
    }
}
