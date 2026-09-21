package HistoryManager.DWMHistory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.history.DefaultHistoricalBarRequest;
import com.esignal.jstandard.beans.history.HistoricalBarRequest;
import com.esignal.jstandard.beans.history.HistoricalBarRequest.PERIOD;
import com.esignal.jstandard.event.HistoricalBarListener;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.HistoryManager;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

// Options Used for Flexible Data Requests
import com.esignal.jstandard.beans.requestoption.DbcColumnsRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcDictIdRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcFlexRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcTimeSeriesRefRequestOption;

// Options Used for Historical Data Requests
import com.esignal.jstandard.beans.requestoption.DbcBackAdjustRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcContinuousChartRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcCurrencyRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcDividendRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcForwardRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcFmrPricePercentBarDataRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcKeyValuePairRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcMaxBarsRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNBarCountRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNullBarRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcPriceTypeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcShiftOpenInterestRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcShiftVolumeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcSplitRequestOption;
import com.esignal.jstandard.beans.history.HistoricalBar.AGGREGATIONS;

import Util.SampleProperties;

/**
 * Provides methods for requesting and processing Daily bars, or other intervals
 * greater than a single day, such as Weekly or Monthly. Please see the
 * TickHistorySample for tick or minute bars.
 * 
 * @author
 *         <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 */
@SuppressWarnings("unused")
public class DWMHistorySample {
    //---------------------------------------------------------------------------------------------
    // CLASS FIELDS
    //---------------------------------------------------------------------------------------------
    private static final int  MAX_CONCURRENT_REQUESTS          = 35;
    private String            m_appStartTime                   = "";
    
    // Network and authentication
    private String            m_host                           = "";
    private SockType          m_connType                       = SockType.SOCKTYPE_LEGACY;
    private String            m_username                       = "";
    private String            m_password                       = "";
    
    // Date/time range
    private String            m_beginDate                      = "";
    private String            m_endDate                        = "";
    
    // Symbol-related fields
    private String[]          m_symbols;
    private String[]          m_options;

    // Interval and concurrency
    private PERIOD            m_interval                       = PERIOD.DAILY;
    private long              m_count                          = 0;    // Completed requests
    private long              m_activeRequests                 = 0;    // Active requests
    private int               m_index                          = 0;    // For batching logic

    // Dictionary-specific
    private boolean           m_dictionaryRequest              = false;
    private int               m_dictClientVersion              = 0;

    // User preferences
    private boolean           m_useInputFile                   = false;
    private boolean           m_displayOutput                  = true;

    // Manager and listener
    private HistoryManager              historyManager;
    private HistoricalBarListener       consoleHistoricalBarListener = new MyConsoleDWMHistoryListener(this);

    // Request object
    private HistoricalBarRequest        historyRequest;
    private long                        turnAroundValue        = 1000;
    
    // Track no-data responses
    private int               m_numRequestsWithNoRecords       = 0;
    
    Set<String> flexKeys = new HashSet<>(Arrays.asList("FLEX", "TIMESERIESREF", "COLUMNS", "DICTID"));


    // Properties file
    Properties sampleProps = SampleProperties.getInstance().getProperties();

    //---------------------------------------------------------------------------------------------
    // CONSTRUCTORS
    //---------------------------------------------------------------------------------------------

    /**
     * Primary constructor for requesting daily/weekly/monthly (DWM) data.
     * 
     * @param <host>
     *            Specifies connection string for ICE Data Services network.
     *            Usually cm*.dataservices.theice.com.
     * @param <connType>
     *            Specifies the type of connection to use with the ICE Data Services network.
     *            
     *               0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
     *               1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
     *               2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
     * @param <username>
     *            The username for the account entitled to receive data from ICE Data Services.
     *            If you do not have one, please contact your ICE Data Services representative.
     * @param <password>
     *            The password for the account entitled to receive data from ICE Data Services.
     *            If you do not have one, please contact your ICE Data Services representative.
     * @param <beginDate>
     *            Specifies the start date for the record set. <yyyy/mm/dd>
     * @param <endDate>
     *            Secifies the ending date for the record set. <yyyy/mm/dd>
     * @param <interval>
     *            Specifies the interval for the request. Takes a <code>String</code>:
     *            DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, etc.
     * @param <options>
     *            Comma separated list of options to apply to the tick history request.
     *            For example: "FORWARD=T,SPLIT=F"
     * @param <symbols>
     *            One or more symbols for which to make the request. Multiple symbols
     *            must be separated by commas with no spaces. If one of the symbols contains a space,
     *            the entire list must be enclosed in quotes.
     * @param <display>
     *            Specifies the type of output to produce.
     *            True indicates format for console display; False indicates format for csv file.
     */
    public DWMHistorySample(
               String host, SockType connType, String username, String password,
               String beginDate, String endDate, String options,
               String[] symbols, boolean display
    ) {
        this.m_host           = host;
        this.m_connType       = connType;
        this.m_username       = username;
        this.m_password       = password;
        this.m_beginDate      = beginDate;
        this.m_endDate        = endDate;
        this.m_displayOutput  = display;
        this.m_dictionaryRequest = false;
        
        try {
        	this.m_symbols = symbols;

        	if (!options.isEmpty()) {
        	    this.m_options = options.split(",");

        	    for (int i = 0; i < m_options.length; i++) {
        	        m_options[i] = m_options[i].replace("|", ",");
        	    }
        	}

        } catch (Exception e) {
            SampleProperties.getInstance().displayHelp(true);
            System.exit(0);
        }
    }

    /**
     * Overloaded constructor for dictionary requests (flexible data).
     * 
     * @param <host>            Connection string for ICE Data Services network.
     * @param <connType>        Type of connection (0 = Legacy, 1 = TLS, 2 = TLS_PLUS).
     * @param <username>        Account username.
     * @param <password>        Account password.
     * @param <version>         Dictionary version known to the client.
     * @param <display>         If true, format output for console; otherwise, CSV-friendly.
     */
    public DWMHistorySample(String host, SockType connType,
         String username, String password, String version,
            boolean display
    ) {
        this.m_host              = host;
        this.m_connType          = connType;
        this.m_username          = username;
        this.m_password          = password;
        this.m_displayOutput     = display;
        this.m_dictionaryRequest = true;

        try {
            if (version != null) {
                this.m_dictClientVersion = Integer.parseInt(version);
            }
        } catch (Exception e) {
            SampleProperties.getInstance().displayHelp(true);
            System.exit(0);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY Main -- START
     * ---------------------------------------------------------------------------------------------
     */
    /**
     * Creates an instance of the HistoryManager via ResourceManagerFactory, then connects
     * to the ICE Data Services network. Upon a successful connection, the onConnected event
     * will trigger further requests (either dictionary or DWM history).
     * 
     * @param factory ResourceManagerFactory used to create the HistoryManager and acquire credentials
     * @throws Exception if a resource or connection error occurs
     */
    public void runDWMHistorySample(ResourceManagerFactory factory) throws Exception {
        try {
            // Check if the client will use Amazon Web Services
            if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useAWS"))) {
                UsernameInfo info = factory.acquireUsername(this.getConnectionSettings());
                
                if (info.getStatusEnum() == DBCAPI.SUCCESS) {
                    this.m_username = info.getUsername();
                    this.m_password = info.getPassword();
                    System.out.println("account acquired . . . ");
                    System.out.println("username: " + this.m_username + ", password: " + this.m_password);
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

            // Create History Manager, Prepare the Request, and Connect
            this.historyManager = factory.createHistoryManager();
            ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
                        
            // Prepare the request object
            prepareHistoryRequests();
            
            historyManager.connect(this.getConnectionSettings(), consoleConnectionListener);

        } catch (ResourceManagerException rme) {
            rme.printStackTrace();
        }
    }
    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY Main -- END
     * ---------------------------------------------------------------------------------------------
     */

    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleDWMHistoryListener -- START
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * Determines whether the current request is for dictionary (flexible data).
     * @return True if dictionary (flexible data) is requested; false otherwise.
     */
    public boolean isDictionaryRequest() {
        return this.m_dictionaryRequest;
    }

    /**
     * Determines whether the output will be displayed to console or directed to a CSV layout.
     * @return True if display formatting is desired; false if CSV formatting is desired.
     */
    public boolean displayData() {
        return this.m_displayOutput;
    }

    /**
     * Called from the MyConsoleConnectionListener.onConnected event.
     * Requests the Flexible Data dictionary (if dictionaryRequest == true).
     */
    public void requestDictionary() {
        try {
           // Prepare and send a dictionary request
         historyManager.requestDictionary(
               m_dictClientVersion, 
               consoleHistoricalBarListener, 
               ++turnAroundValue
           );
        } catch (Exception e) {
            System.out.println("Error requesting dictionary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processes and submits history requests in batches to manage concurrency limits.
     * The HistoryManager supports up to 35 concurrent requests.
     *
     * @param index The starting index for requests in the symbol list.
     */
    public void requestHistory(int index) {
        if (index == 0)
            m_appStartTime = markTime();
    
        for (int i = index; i < m_symbols.length; i++) {
            String symbol = m_symbols[i];
            try {
                // Submit the request and update counters
                historyManager.request(symbol, consoleHistoricalBarListener, historyRequest);
                historyRequest.setTurnAround(++turnAroundValue);
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
     * Returns a string representation of the current date/time for logging.
     * @return Formatted date/time string.
     */
    public String markTime() {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM-dd-yyyy / HH:mm:ss");
        return dateTimeFormat.format(new Date());
    }

    /**
     * Called from the listener when a request completes. Manages concurrency:
     * - Decrements active request counter.
     * - Checks if all symbols have finished processing.
     */
    public void setHistComplete() {
        // Increment number of completed requests
        ++this.m_count;
       --this.m_activeRequests;
       if (this.m_activeRequests < MAX_CONCURRENT_REQUESTS) {
          requestHistory(this.m_index);
        }
        
        // Determine if all requests have been processed, and if so, disconnect
        // from the tick history server
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
     * Called from consoleHistoricalBarListener upon completion of dictionary retrieval.
     */
    public void setDictionaryComplete() {
        if (this.displayData()) {
            System.out.println("-------------------------------------------------------------------------------");
            System.out.printf("SAMPLE END TIME: %s\n", markTime());
            System.out.println("-------------------------------------------------------------------------------");
        }
        try {
            this.exitSample(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnects from ICE Data Services (if still connected) and exits the sample.
     * 
     * @param disconnect If true, attempt to disconnect before exiting.
     * @throws Exception if an error occurs during disconnection
     */
    public void exitSample(boolean disconnect) throws Exception {
        if (disconnect) {
            if (this.m_displayOutput) {
                System.out.println("");
                System.out.println("Exiting application...");
                System.out.println("(press Enter to exit)");
                System.in.read();
            }
            try {
                this.historyManager.disconnect();
            } catch (ResourceManagerException rme) {
                rme.printStackTrace();
            }
        }
        System.exit(0);
    }
    /* ---------------------------------------------------------------------------------------------
     * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleDWMHistoryListener -- END
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * Retrieves connection settings from the command line or properties file.
     * @return A fully populated ConnectionSettings object.
     */
    private ConnectionSettings getConnectionSettings() {
        ConnectionSettings connectionSettings =
                new ConnectionSettings(this.m_host, this.m_username, this.m_password, this.m_connType);

        if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useproxy"))) {
            String proxyusername = "";
            String proxypassword = "";
            connectionSettings.setProxyInfo(
                    sampleProps.getProperty("samples.connection.proxyhost"),
                    Integer.parseInt(sampleProps.getProperty("samples.connection.proxyport")),
                    proxyusername,
                    proxypassword
            );
        }

        // Connection Timeout Override
        // ===========================
        // The API allows up to 30 seconds to establish a connection before timing out
        // and making another attempt.  This default can be overridden using a property
        // setting from the jstandard.properties file, or by using the setConnectionTimeout
        // method.
        //
        // If the jstandard.DBC.timeouts.connection property is set in the jstandard.properties
        // file, the API will automatically retrieve the value and apply it to all connections.
        //
        // If the setConnectionTimeout method is used, it will take precedence over any value
        // provided in the jstandard.properties file.
        // 
        // The following code demonstrates how to set a timeout value using the setConnectionTimeout
        // method:

        //connectionSettings.setConnectionTimeout(30000);

        // note:  The above value of 30000 is the same as the default value.  This value can be
        //        changed.  The minimum value is 1000.

        // IMPORTANT: Use of the setConnectionTimeout method affects ALL connections (new and existing)
        //            including existing connections that experience a reconnection.  
        //            This is a *global* setting and affects all connections regardless of
        //            manager type.

        return connectionSettings;
    }

    /**
     * Sets up the HistoricalBarRequest object with required parameters, including
     * date range, interval, and any additional options provided on the command line.
     */
    private void prepareHistoryRequests() {
        try {
            setupRequestObject();
            
            processOptions(this::setCommonRequestOptions);
            processOptions(this::setFlexRequestOptions);

            setRequestDateRange();
        } catch (Exception e) {
            System.out.println("Error initializing request: " + e.getMessage());
            SampleProperties.getInstance().displayHelp(true);
            System.exit(0);
        }    
    }
     
    /**
     * Configures the appropriate request object.
     */
    private void setupRequestObject() {
        turnAroundValue = 1000; // Initialize turnaround ID
        historyRequest = new  DefaultHistoricalBarRequest(turnAroundValue);     
    }
    
    /* ---------------------------------------------------------------------------------------------
     * PROCESS REQUEST OPTIONS -- START
     * ---------------------------------------------------------------------------------------------
     */
    
    /**
     * Generalized method to process options for all request types.
     * @param optionProcessor A BiConsumer that processes each key-value pair.
     */
    private void processOptions(BiConsumer<String, String> optionProcessor) {
        if (m_options == null) return;
    
        for (String option : m_options) {
            if (option == null || option.trim().isEmpty()) continue;
    
            String[] parts = option.split("=");
            String key = parts[0].trim().toUpperCase();
            String value = parts.length > 1 ? parts[1].trim() : "";
    
            optionProcessor.accept(key, value);
        }
    }
    
    /**
     * Handles options specific to Flexible Data requests.
     * - COLUMNS: Specifies the data columns to return.
     * - DICTID: Adds a dictionary ID option.
     * - TIMESERIESREF: Sets the reference time for series data.
     * - FLEX: Enables Flexible Data requests.
     */
    private void setFlexRequestOptions(String key, String value) {
        switch (key) {
            case "COLUMNS":
                historyRequest.addOption(new DbcColumnsRequestOption(value));
                break;
            case "DICTID":
                addDictIdOption(value);
                break;
            case "TIMESERIESREF":
            case "FLEX":
                historyRequest.addOption(new DbcKeyValuePairRequestOption(key, value));
                break;
            default:
                break;
        }
    }
    
    /**
     * Processes individual options and maps them to the appropriate request configuration.
     */
    private void setCommonRequestOptions(String key, String value) {
        switch (key) {
            case "BACKADJT":
                addBackAdjustOption(value);
                break;
            case "CONTCHART":
                historyRequest.addOption(new DbcContinuousChartRequestOption(true));
                break;
            case "CURRENCY":
                setCurrencyOption(value);
                break;
            case "PRICETYPE":
                setPriceTypeOption(value);
                break;
            case "NBARS":
                setBarCount(value);
                break;
            case "MAXBARS":
                setMaxBarsOption(value);
                break;
            case "BARINTV":
				setInterval(value);
				break;
            case "FORWARD":
                historyRequest.addOption(new DbcForwardRequestOption(true));
                break;
            case "SPLIT":
                // By default is true; specifying SPLIT in this sample sets to false
                historyRequest.addOption(new DbcSplitRequestOption(false));
                break;
            case "SHIFTVOL":
                historyRequest.addOption(new DbcShiftVolumeRequestOption(true));
                break;
            case "SHIFTOI":
                historyRequest.addOption(new DbcShiftOpenInterestRequestOption(true));
                break;
            case "NULLBARS":
                historyRequest.addOption(new DbcNullBarRequestOption(true));
                break;
            case "REQUESTAGGREGATIONFIELD":
                setAggregationOption(value);
                break;
            case "ADJEQPCTCHGPRIORBAR":
                // Option to have the "adjustments" field represent % change from last bar's close
                historyRequest.addOption(new DbcKeyValuePairRequestOption("ADJEQPCTCHGPRIORBAR", "TRUE"));
                break;
            default:
            	// Default case for other non-flexible data keys
				if (!flexKeys.contains(key)) { 
					// Handle flexible data keys
					historyRequest.addOption(new DbcKeyValuePairRequestOption(key, value));
				    //System.out.println(key + "=" + value);
				}
                break;
        }
    }

    /**
     * Attempts to add a dictionary ID request option.
     */
    private void addDictIdOption(String value) {
        try {
            long dictId = Long.parseLong(value);
            historyRequest.addOption(new DbcDictIdRequestOption(dictId));
        } catch (NumberFormatException e) {
            System.out.println("Invalid DICTID value: " + value);
        }
    }

    /**
     * Adds a back-adjustment option to the request.
     */
    private void addBackAdjustOption(String value) {
        try {
            historyRequest.addOption(new DbcBackAdjustRequestOption(Integer.parseInt(value)));
        } catch (Exception e) {
            System.out.println("Could not set the BACKADJT option.");
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Sets the number of bars to be returned (counted backward from the END date).
     */
    private void setBarCount(String value) {
        try {
            historyRequest.setBarCount(Integer.parseInt(value));
        } catch (Exception e) {
            System.out.println("Could not set the NBARS option.");
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Configures the price type for the requested bars: e.g., KASSA, MID, VARIABLE, etc.
     */
    private void setPriceTypeOption(String value) {
        if (value.equalsIgnoreCase("KASSA")) {
            historyRequest.addOption(new DbcPriceTypeRequestOption(DbcPriceTypeRequestOption.CLOSE.KASSA));
        } else if (value.equalsIgnoreCase("MID")) {
            historyRequest.addOption(new DbcPriceTypeRequestOption(DbcPriceTypeRequestOption.CLOSE.MID));
        } else if (value.equalsIgnoreCase("VARIABLE")) {
            historyRequest.addOption(new DbcPriceTypeRequestOption(DbcPriceTypeRequestOption.CLOSE.VARIABLE));
        }
    }

    /**
     * Configures the currency request option.
     */
    private void setCurrencyOption(String value) {
        if (value.equalsIgnoreCase("GBP")) {
            historyRequest.addOption(new DbcCurrencyRequestOption(DbcCurrencyRequestOption.TYPE.GBP));
        } else if (value.equalsIgnoreCase("GBX")) {
            historyRequest.addOption(new DbcCurrencyRequestOption(DbcCurrencyRequestOption.TYPE.GBX));
        }
    }

    /**
     * Sets the maximum bars (MAXBARS) limit, which controls the maximum bars in each response chunk.
     */
    private void setMaxBarsOption(String value) {
        try {
            historyRequest.addOption(new DbcMaxBarsRequestOption(Integer.parseInt(value)));
        } catch (Exception e) {
            System.out.println("Could not set the MAXBARS option.");
            System.out.println("Error: " + e.getMessage());
        }
    }
    
	/**
	 * Sets the interval for the request.
	 * 
	 * @param value The interval value as a string (e.g., "DAILY", "WEEKLY", etc.).
	 */
	private void setInterval(String value) {
		try {
			m_interval = PERIOD.valueOf(value.toUpperCase());
			historyRequest.setPeriod(m_interval);
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid interval value: " + value);
			System.out.println("Please use a valid interval such as DAILY, WEEKLY, MONTHLY, etc.");
		}
	}

    /**
     * Configures optional aggregated fields (e.g., SIMPLEYIELD, 7DAYYIELD, etc.).
     */
    private void setAggregationOption(String value) {
        switch (value.toUpperCase()) {
            case "SIMPLEYIELD":
                historyRequest.addOption(new DbcKeyValuePairRequestOption(
                        AGGREGATIONS.getKey(), AGGREGATIONS.SIMPLE_YIELD.getValue()));
                break;
            case "7DAYYIELD":
                historyRequest.addOption(new DbcKeyValuePairRequestOption(
                        AGGREGATIONS.getKey(), AGGREGATIONS.SEVEN_DAY_YIELD.getValue()));
                break;
            case "30DAYYIELD":
                historyRequest.addOption(new DbcKeyValuePairRequestOption(
                        AGGREGATIONS.getKey(), AGGREGATIONS.THIRTY_DAY_YIELD.getValue()));
                break;
            default:
                break;
        }
    }
    
   /**
    * Sets the date range for the request.
    */
   private void setRequestDateRange() {
       try {
           SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

           historyRequest.setBeginDate(format.parse(m_beginDate));
           historyRequest.setEndDate(format.parse(m_endDate));
       } catch (ParseException e) {
           throw new RuntimeException("Invalid date format for BeginDate or EndDate", e);
       }
   }
    /* ---------------------------------------------------------------------------------------------
     * PROCESS REQUEST OPTIONS -- END
     * ---------------------------------------------------------------------------------------------
     */

}
