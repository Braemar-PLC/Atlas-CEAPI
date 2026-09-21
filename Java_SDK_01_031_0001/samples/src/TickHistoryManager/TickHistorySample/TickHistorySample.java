package TickHistoryManager.TickHistorySample;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.List;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.tick.DefaultHistoricalTickRequest;
import com.esignal.jstandard.beans.tick.HistoricalTickRequest;
import com.esignal.jstandard.beans.tick.IntradayHistoricalTickRequest;
import com.esignal.jstandard.event.HistoricalTickListener;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.TickHistoryManager;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;
import com.esignal.jstandard.beans.RequestOption.OPERATORS;

// Options Used for Flexible Data Requests
import com.esignal.jstandard.beans.requestoption.DbcColumnsRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcDictIdRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcTimeSeriesRefRequestOption;

// Options Used for Tick History Requests
import com.esignal.jstandard.beans.requestoption.DbcApplyCidRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcBrokerIdRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcExchangeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcKeyValuePairRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcPriceRangeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcSeqNoRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcDividendRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcDividendRequestOption.ADJUSTMENT;
import com.esignal.jstandard.beans.requestoption.DbcNBarCountRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNBarPriceChangeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNBarRangeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNBarSecondsRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNBarVolumeRequestOption;
import com.esignal.jstandard.beans.requestoption.DbcNBarVolume2RequestOption;
import com.esignal.jstandard.beans.requestoption.DbcOpenAndCloseTimeRequestOption;

import Util.SampleProperties;

/**
 * Provides methods for requesting and processing tick or minute bars. Please see the HistoryManager
 * for Daily (or greater) bars.
 * 
 * @author
 *         <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 */
public class TickHistorySample {
    //---------------------------------------------------------------------------------------------
    // CLASS FIELDS
    //---------------------------------------------------------------------------------------------
    private static final int  MAX_CONCURRENT_REQUESTS          = 2;
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
    private int               m_interval                       = 0;
    private String            m_barLength                      = "";
    private long              m_count                          = 0;    // Completed requests
    private long              m_activeRequests                 = 0;    // Active requests
    private int               m_index                          = 0;    // For batching logic
    
    private ArrayList<Integer> m_qualFilters                   = new ArrayList<>();

    // Dictionary-specific
    private boolean           m_dictionaryRequest              = false;
    private int               m_dictClientVersion              = 0;

    // Used by MyConsoleTickHistoryListener for display purposes
    private boolean           isTradesOnly                     = false;
    private boolean           isQuotesOnly                     = false;

    // Display preferences
    private boolean m_displayOutput                            = true;
   
    // Manager and listener
   private TickHistoryManager          tickHistoryManager;
   private HistoricalTickListener      consoleHistoricalTickListener = new MyConsoleTickHistoryListener(this);

    // Request object
   private HistoricalTickRequest       intradayRequest;
   private long                        turnAroundValue         = 1000;

    // Track no-data responses
    private int               m_numRequestsWithNoRecords       = 0;

   // Properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();   

    //---------------------------------------------------------------------------------------------
    // CONSTRUCTORS
    //---------------------------------------------------------------------------------------------

   /**
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
    *            Specifies the start date for the record set. <yyyy/mm/dd hh:mm:ss>
    * @param <endDate>
    *            Secifies the ending date for the record set. <yyyy/mm/dd hh:mm:ss>
    * @param <interval>
    *            Specifies the interval for the request. Takes an int where:
    *            0 = Tick
    *            n > 0 = minute intervals (e.g. 1 = 1-minute bars, 5 = 5-minute bars, etc.)
    * @param <options>
    *            Comma separated list of options to apply to the tick history request.
    *            For example: "DIVIDEND=RATIO,SPLIT=F"
    * @param <symbols>
    *            One or more symbols for which to make the request. Multiple symbols
    *            must be separated by commas with no spaces. If one of the symbols contains a space,
    *            the entire list must be enclosed in quotes.
    * @param <display>
    *            Specifies the type of output to produce.
    *            True indicates format for console display; False indicates format for csv file.
    */
   public TickHistorySample(
           String host, SockType connType, String username, String password, 
           String beginDate, String endDate, String interval, String options, 
           String[] symbols, boolean display
   ){
      this.m_host              = host;
      this.m_connType          = connType;
      this.m_username          = username;
      this.m_password          = password;
      this.m_beginDate         = beginDate;
      this.m_endDate           = endDate;
      this.m_displayOutput     = display;
      this.m_dictionaryRequest = false;
 
      try {
         if (interval != null) {
            this.m_interval = Integer.parseInt(interval);
            this.m_barLength = interval;
         }

         this.m_symbols = symbols;

         if (!options.isEmpty())
            this.m_options = options.split(",");
      } catch (Exception e) {
         SampleProperties.getInstance().displayHelp(true);
         System.exit(0);
      }
   }

   /**
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
    * @param <version>
    *            Indicates the version of the dictionary known to the client.
    * @param <display>
    *            Specifies the type of output to produce.
    *            True indicates format for console display; False indicates format for csv file.
    */
   public TickHistorySample(String host, SockType connType, 
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
           if (version != null)
               this.m_dictClientVersion = Integer.parseInt(version);
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
    * Creates an instance of a tickHistoryManager factory and a connection listener. Connects to the farm using connection settings
    * retrieved from the command line via the getConnectionSettings() function.
    * 
    * @param factory
    *            Used to create a history manager
    * @throws ResourceManagerException
    */
   public void runTickHistorySample(ResourceManagerFactory factory) throws Exception {
	  if (!isDictionaryRequest())
		  prepareHistoryRequests();
	  
      try {
         // Check if the client will use Amazon Web Services
         if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useAWS"))) {                
            UsernameInfo info = factory.acquireUsername(this.getConnectionSettings());
             
            if (info.getStatusEnum() == DBCAPI.SUCCESS ) {
               this.m_username = info.getUsername();
               this.m_password = info.getPassword();
               System.out.println("account acquired . . . ");
            }
            else {
               DBCAPI_ERROR errorCode = DBCAPI_ERROR.valueOf(info.getStatusEnum().get().name());
               
               switch (errorCode) {
                  case INVALIDNAME:
                     System.out.println("\nThe host specified is not valid.  Verify the host value and try again.");
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
         // Create Tick History Manager and Connect
         this.tickHistoryManager = factory.createTickHistoryManager();
         ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
         tickHistoryManager.connect(this.getConnectionSettings(), consoleConnectionListener);
      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
   }
   /* ---------------------------------------------------------------------------------------------
    * METHODS CALLED BY Main -- END
    * ---------------------------------------------------------------------------------------------
    */
   
   /* ---------------------------------------------------------------------------------------------
    * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleTickHistoryListener -- START
    * ---------------------------------------------------------------------------------------------
    */
   
   public boolean isDictionaryRequest() {
       // note: The "flexible data" features are available for the "desktop offerings" only.
       return this.m_dictionaryRequest;
   }

   public boolean displayData() {
       return this.m_displayOutput;
   }
   
   /**
    * Provides the list of qualifiers supplied to the application.
    * The sample will display data that includes those qualifiers only.
    * 
    * note:  The Intraday History service will return all the tics within the date/time
    *        range for the request.  The sample will filter what is displayed based upon
    *        the qualifiers provided
    *        
    * @return qualiferFilters
    */
   public ArrayList<Integer> getQualifierFilters() {
       return m_qualFilters;
   }
   
   /**
    * Determines whether the current request is restricted to trades only.
    *
    * <p>This method checks if the request is configured to retrieve only trade 
    * data (e.g., buy/sell transactions) and excludes quote data such as bid/ask prices.</p>
    *
    * @return `true` if the request is restricted to trades only; `false` otherwise.
    */
   public boolean isTradesOnly() {
       return this.isTradesOnly;
   }
   
   /**
    * Determines whether the current request is restricted to quotes only.
    *
    * <p>This method checks if the request is configured to retrieve only quote 
    * data (e.g., bid/ask prices) and excludes trade data such as buy/sell transactions.</p>
    *
    * @return `true` if the request is restricted to quotes only; `false` otherwise.
    */
   public boolean isQuotesOnly() {
       return this.isQuotesOnly;
   }
   
   /**
    * Called from the MyConsoleConnectionListener.onConnected event.
    * Processes dictionary requests to retrieve Flexible Data dictionary information.
    */
   public void requestDictionary() {
       try {
           // Prepare and send a dictionary request
           tickHistoryManager.requestDictionary(
               m_dictClientVersion,
               consoleHistoricalTickListener,
               ++turnAroundValue
           );
       } catch (Exception e) {
           System.out.println("Error requesting dictionary: " + e.getMessage());
           e.printStackTrace();
       }
   }
   
   /**
    * Processes and submits history requests in batches to manage concurrency limits.
    * The TickHistoryManager supports up to 35 concurrent requests.
    * @param index The starting index for requests in the symbol list.
    */
   public void requestHistory(int index) {
       if (index == 0)
           m_appStartTime = markTime();
   
       for (int i = index; i < m_symbols.length; i++) {
           String symbol = m_symbols[i];
           try {
               // Submit the request and update counters
               tickHistoryManager.request(symbol, consoleHistoricalTickListener, intradayRequest);
               intradayRequest.setTurnAround(++turnAroundValue);
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

   public String markTime() {
      SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM-dd-yyyy / HH:mm:ss");
      return dateTimeFormat.format(new Date());
   }
   
   /**
    * Called from MyConsoleTickHistoryListener onComplete event.
    * This method increments the number of completed requests and disconnects from the
    * Intraday History service if all requests have been processed.
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
    * Called from MyConsoleHistoricalTickListener onComplete event.
    * This method disconnects from the Intraday History service.
    */   
   public void setDictionaryComplete() {
       if (displayData()) {
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
     * Disconnects from the TickHistoryManager and exits the sample.
     * 
     * @param disconnect If true, disconnect before exiting.
     */
   public void exitSample (boolean disconnect) {
      if (disconnect) {         
       if (displayData()) {
          // The sample maintains the connection until the user presses Enter.
          // The reason for maintaining the connection is to allow clients and support to verify connectivity is needed.
            System.out.println("");
            System.out.println("Exiting application...");
            System.out.println("(press Enter to exit)");
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }  
            
            try {
                this.tickHistoryManager.disconnect();
            } catch (ResourceManagerException rme) {
                rme.printStackTrace();
            }
         }
      }
      System.exit(0);
   }
   /* ---------------------------------------------------------------------------------------------
    * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleTickHistoryListener -- END
    * ---------------------------------------------------------------------------------------------
    */
    
   /**
    * Retrieves connection settings from either the command line or properties
    * file, and returns to the calling function.
    * 
    * @return A fully populated ConnectionSettings object.
    */
   private ConnectionSettings getConnectionSettings() {
      ConnectionSettings connectionSettings = new ConnectionSettings(this.m_host, this.m_username, this.m_password, this.m_connType);
      
      if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useproxy"))) {
         String proxyusername = "";
         String proxypassword = "";
         connectionSettings.setProxyInfo(sampleProps.getProperty("samples.connection.proxyhost"), Integer.parseInt(sampleProps.getProperty("samples.connection.proxyport")), proxyusername, proxypassword);
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
    * Called from the MyConsoleConnectionListener.onConnected event.
    * Prepares and executes a historical tick or bar data request based on user-specified parameters.
    */
   private void prepareHistoryRequests() {
       try {
           setupRequestObject();
           
           processOptions((key, value) -> setFlexRequestOptions(key, value));
           processOptions((key, value) -> setCommonRequestOptions(key, value));
           processOptions((key, value) -> setTickRequestOptions(key, value));
           processOptions((key, value) -> setBarRequestOptions(key, value));

           setRequestDateRange();
       } catch (Exception e) {
           System.out.println("Error initializing request: " + e.getMessage());
           SampleProperties.getInstance().displayHelp(true);
           System.exit(0);
       }
   }
   
   /**
    * Configures the appropriate request object (tick or bar) based on the interval parameter.
    * Handles:
    * - Historical Tick Requests (when interval is 0).
    * - Historical Bar Requests (when interval > 0).
    */
   private void setupRequestObject() {
       turnAroundValue = 1000; // Initialize turnaround ID
       if (m_interval > 0) {
           // Create a request for bar data
           intradayRequest = new IntradayHistoricalTickRequest(turnAroundValue, m_interval);
       } else {
           // Create a request for tick data
           intradayRequest = new DefaultHistoricalTickRequest(turnAroundValue);
       }
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
               intradayRequest.addOption(new DbcColumnsRequestOption(value.replace("|", ",")));
               break;
           case "DICTID":
               addDictIdOption(value);
               break;
           case "TIMESERIESREF":
               intradayRequest.addOption(new DbcTimeSeriesRefRequestOption(value));
               break;
           case "FLEX":
               intradayRequest.addOption(new DbcKeyValuePairRequestOption("FLEX", "T"));
               break;
           default:
               break;
       }
   }
   
   /**
    * Handles options common to both TICK and INTRADAY requests.
    * - EXCHANGE: Specifies the exchange codes to filter.
    * - FORMT, FORWARD, NULLBAR, OUTSESSION, SETTLEMENT, SPLIT: Boolean flags for request options.
    */
   private void setCommonRequestOptions(String key, String value) {
       switch (key) {
           case "FORMT":
           case "FORWARD":
           case "NULLBAR":
           case "OUTSESSION":
           case "SETTLEMENT":
           case "SPLIT":
               // The JStandard API provides interfaces for each option (ie DbcSplitRequestOption),
               // but this sample uses the DbcKeyValuePairRequestOption to consolidate options 
               // that can be set to either T or F.
               intradayRequest.addOption(new DbcKeyValuePairRequestOption(key, value));
               break;
           case "EXCHANGE":
               intradayRequest.addOption(new DbcExchangeRequestOption(value.replace("|", ",")));
               break;
           default:
               break;
       }
   }
   
   /**
    * Handles options specific to TICK requests.
    * - APPLYCID, BBOONLY, CID, DELETES, NTICS, NTICSEAL, QUOTEONLY: Boolean flags.
    * - BROKERID: Filters data by broker IDs.
    * - PRICE, VALUE, SIZE: Range-based options.
    * - QUALFILTER: Filters by qualifier IDs.
    */
   private void setTickRequestOptions(String key, String value) {
       switch (key) {
           case "APPLYCID":
               intradayRequest.addOption(new DbcApplyCidRequestOption(Integer.parseInt(value)));
               break;
           case "BBOONLY":
           case "TRADESONLY":
           case "CID":
           case "DELETES":
           case "NTICSEAL":
           case "QUOTEONLY":
               // The JStandard API provides interfaces for each option (ie DbcBboOnlyRequestOption),
               // but this sample uses the DbcKeyValuePairRequestOption to consolidate options 
               // that can be set to either T or F.
               intradayRequest.addOption(new DbcKeyValuePairRequestOption(key, value));
               if ("TRADESONLY".equals(key))
            	    this.isTradesOnly = true;
            	if ("QUOTEONLY".equals(key))
            	    this.isQuotesOnly = true;
               break;
           case "NTICS":
               // Set the number of ticks to return.
               // The ticks returned will either count forward from the START DATE
               // or backward from the END DATE of the request range depending upon
               // the FORWARD option.  Default behavior counts backward from the END DATE.
               try {
                   intradayRequest.setBarCount(Integer.parseInt(value));
               } catch (Exception e) {                 
                   System.out.println("ERROR: Could not set the NTICS option.");
                   System.out.println("       " + e.getCause() + "\t( " + e.getMessage() + ")\n");
               }
               break;
           case "BROKERID":
               intradayRequest.addOption(new DbcBrokerIdRequestOption(parseBrokerIds(value)));
               break;
           case "PRICE":
           case "VALUE":
           case "SIZE":
               processRangeOption(key, value);
               break;
           case "SEQNO":
               intradayRequest.addOption(new DbcSeqNoRequestOption(value));
               break;
           case "QUALFILTER":
               processQualFilterOption(value);
               break;
           default:
               break;
       }
   }
   
   /**
    * Handles options specific to INTRADAY (bar) requests.
    * - CLOSETIME, OPENTIME: Configures session times.
    * - NTICBAR, NTICBAR_PRCCHG, NTICBAR_RANGE, NTICBAR_SEC, NTICBAR_VOL, NTICBAR_VOL2: Bar-based options.
    */
   private void setBarRequestOptions(String key, String value) {
       switch (key) {
           case "CLOSETIME":
           case "OPENTIME":
               processOpenCloseTimeOption(key, value);
               break;
           case "NTICBAR":
               intradayRequest.addOption(new DbcNBarCountRequestOption(Integer.parseInt(value)));
               break;
           case "NTICBAR_PRCCHG":
               intradayRequest.addOption(new DbcNBarPriceChangeRequestOption(Integer.parseInt(value)));
               break;
           case "NTICBAR_RANGE":
               intradayRequest.addOption(new DbcNBarRangeRequestOption(Float.parseFloat(value)));
               break;
           case "NTICBAR_SEC":
               intradayRequest.addOption(new DbcNBarSecondsRequestOption(Integer.parseInt(value)));
               break;
           case "NTICBAR_VOL":
               intradayRequest.addOption(new DbcNBarVolumeRequestOption(Integer.parseInt(value)));
               break;
           case "NTICBAR_VOL2":
               intradayRequest.addOption(new DbcNBarVolume2RequestOption(Integer.parseInt(value)));
               break;
           case "NBARS":
               // Set the number of bars to return.
               // The bars returned will either count forward from the START DATE
               // or backward from the END DATE of the request range depending upon
               // the FORWARD option.  Default behavior counts backward from the END DATE.
               try {
                   intradayRequest.setBarCount(Integer.parseInt(value));
               } catch (Exception e) {                 
                   System.out.println("ERROR: Could not set the NBARS option.");
                   System.out.println("       " + e.getCause() + "\t( " + e.getMessage() + ")\n");
               }
               break;
           case "DIVIDEND":
               setDividendOption(value);
               break;
           default:
               break;
       }
   }
   
   /**
    * Processes range-based options like PRICE, VALUE, and SIZE.
    * Uses supported operators: =, >, <, >=, <=.
    */
   private void processRangeOption(String key, String value) {
       try {
           String[] parts = value.split(",");
           if (parts.length != 2) throw new IllegalArgumentException("Invalid format for " + key);
   
           String operator = parts[0].trim();
           double operand = Double.parseDouble(parts[1].trim());
   
           OPERATORS op;
           switch (operator) {
               case "=":
                   op = OPERATORS.EQUAL;
                   break;
               case ">":
                   op = OPERATORS.GREATER_THAN;
                   break;
               case "<":
                   op = OPERATORS.LESS_THAN;
                   break;
               case ">=":
                   op = OPERATORS.GREATER_THAN_OR_EQUAL;
                   break;
               case "<=":
                   op = OPERATORS.LESS_THAN_OR_EQUAL;
                   break;
               default:
                   throw new IllegalArgumentException("Invalid operator in " + key);
           }
           intradayRequest.addOption(new DbcPriceRangeRequestOption(op, (float) operand));
       } catch (Exception e) {
           System.out.println("Error processing " + key + ": " + e.getMessage());
       }
   }
   
   /**
    * Processes QUALFILTER option for tick requests.
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
   
   /**
    * Processes OPENTIME and CLOSETIME options for bar requests.
    */
   private void processOpenCloseTimeOption(String key, String value) {
       try {
           int timeValue = Integer.parseInt(value);
           int hour = timeValue / 100;
           int minute = timeValue % 100;
   
           if (key.equals("OPENTIME")) {
               intradayRequest.addOption(new DbcOpenAndCloseTimeRequestOption(hour, minute, -1, -1));
           } else if (key.equals("CLOSETIME")) {
               intradayRequest.addOption(new DbcOpenAndCloseTimeRequestOption(-1, -1, hour, minute));
           }
       } catch (Exception e) {
           System.out.println("Error processing " + key + ": " + e.getMessage());
       }
   }
   
   /**
    * Adds a dictionary ID option to the request.
    */
   private void addDictIdOption(String value) {
       try {
           long dictId = Long.parseLong(value);
           intradayRequest.addOption(new DbcDictIdRequestOption(dictId));
       } catch (NumberFormatException e) {
           System.out.println("Invalid DICTID value: " + value);
       }
   }
   
   /**
    * Sets the dividend option for INTRADAY requests.
    */
   private void setDividendOption(String value) {
       try {
           int adjustment = Integer.parseInt(value);
           ADJUSTMENT adj;
           switch (adjustment) {
               case 1:
                   adj = ADJUSTMENT.SCALE;
                   break;
               case 2:
                   adj = ADJUSTMENT.RATIO;
                   break;
               default:
                   adj = ADJUSTMENT.NONE;
           }
           intradayRequest.addOption(new DbcDividendRequestOption(adj));
       } catch (NumberFormatException e) {
           System.out.println("Invalid DIVIDEND value: " + value);
       }
   }
   
   /**
    * Parses broker IDs from a pipe-separated string.
    */
   private List<Integer> parseBrokerIds(String value) {
       List<Integer> brokerIds = new ArrayList<>();
       try {
           String[] ids = value.split("\\|");
           for (String id : ids) {
               brokerIds.add(Integer.parseInt(id.trim()));
           }
       } catch (NumberFormatException e) {
           System.out.println("Invalid BROKERID value: " + value);
       }
       return brokerIds;
   }
   
   /**
    * Sets the date range for the request.
    */
   private void setRequestDateRange() {
       try {
           SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
           intradayRequest.setBeginDate(format.parse(m_beginDate));
           intradayRequest.setEndDate(format.parse(m_endDate));
       } catch (ParseException e) {
           throw new RuntimeException("Invalid date format for BeginDate or EndDate", e);
       }
   }
   
   /* ---------------------------------------------------------------------------------------------
    * PROCESS REQUEST OPTIONS -- END
    * ---------------------------------------------------------------------------------------------
    */
   
}