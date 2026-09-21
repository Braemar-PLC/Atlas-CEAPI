package MarketDepthManager.GetDepthInfoSample;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionFlags.CONNECTION_FLAGS;
import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.MarketDepthListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.MarketDepthManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;

/**
 * Provides methods for requesting and processing depth information about a symbol.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *
 */
public class GetDepthInfoSample {
    //---------------------------------------------------------------------------------------------
    // CLASS FIELDS
    //---------------------------------------------------------------------------------------------
    // The Market Depth Manager doesn't have a limit on the number of concurrent requests apart from
	// any limits imposed by entitlements.  However, for demonstration purposes, this sample limits
	// the number of concurrent requests to 100.  This value may be changed as needed.
	private static final int  MAX_CONCURRENT_REQUESTS          = 100;
    private String            m_appStartTime                   = "";
                                                     
    // Network and authentication                    
    private String            m_host                           = "";
    private SockType          m_connType                       = SockType.SOCKTYPE_LEGACY;
    private String            m_username                       = "";
    private String            m_password                       = "";
    
    // Symbol-related fields
    private String[]          m_symbols;              

    // Concurrent request tracking
    private long              m_count                          = 0;    // Completed requests
    private long              m_activeRequests                 = 0;    // Active requests
    private int               m_index                          = 0;    // For batching logic

    // Display preferences
    private boolean m_displayOutput                            = true;
   
    // Manager and listener
   private MarketDepthManager       marketDepthManager;
   private MarketDepthListener      consoleMarketDepthListener = new MyConsoleMarketDepthListener(this);

    // Request-related fields
   private long                     turnAroundValue            = 1000;

    // Track no-data responses
    private int               m_numRequestsWithNoRecords       = 0;

   // Properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();

    //---------------------------------------------------------------------------------------------
    // CONSTRUCTORS
    //---------------------------------------------------------------------------------------------
   
   /**
    * @param <host>
    *            Specifies the address to use to connect to the ICE Data Services
    *            network. Usually cm*.dataservices.theice.com.
    * @param <connType>     
    *            Specifies the type of connection to use with the ICE Data Services network.
    *            
    *               0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
    *               1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
    *               2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
    * @param <username>
    *            (Required) Please contact your sales or pre-sales representative
    *            to obtain a trial account if you do not have one.
    * @param <password>
    *            (Required) Please contact your sales or pre-sales representative to obtain
    *            a trial account if you do not have one.
    * @param <symbols>
    *            The symbol, or symbols, to use for making the Depth Info request.
    *            May be a single symbol, or a comma delimited string, that will be parsed
    *            into an array of symbols.
    * @param <display>      
    *            Specifies the type of output to produce. 
    *            True indicates format for console display; False indicates format for csv file.
    */
   public GetDepthInfoSample(String host, SockType connType, String username, String password, String[] symbols, boolean display) {
      sampleProps = SampleProperties.getInstance().getProperties();
      
      this.m_host           = host;
      this.m_connType       = connType;
      this.m_username       = username;
      this.m_password       = password;   
      this.m_symbols        = symbols;
      this.m_displayOutput  = display;
   }
   
   /* ---------------------------------------------------------------------------------------------
    * METHODS CALLED BY Main -- START
    * ---------------------------------------------------------------------------------------------
    */
    
   /**
    * Creates an instance of a marketDepthManager factory and a connection
    * listener. Connects to the farm using connection settings retrieved from
    * the command line via the getConnectionSettings() function.
    * 
    * @param factory
    *           Used to create a market depth manager
    * @throws ResourceManagerException
    */
   public void runGetDepthInfoSample(ResourceManagerFactory factory) throws Exception {
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
         // Create Market Depth Manager and Connect
         marketDepthManager = factory.createMarketDepthManager();
         ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
         ConnectionSettings conSet = this.getConnectionSettings();
         conSet.setFlags(CONNECTION_FLAGS.DBCAPI_FLAG_DEPTH_EX.getCode());
         marketDepthManager.connect(conSet, consoleConnectionListener);
      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
   }
   /* ---------------------------------------------------------------------------------------------
    * METHODS CALLED BY Main -- END
    * ---------------------------------------------------------------------------------------------
    */
   
   /* ---------------------------------------------------------------------------------------------
    * METHODS CALLED BY MyConsoleConnectionListener and MyConsoleMarketDepthListener -- START
    * ---------------------------------------------------------------------------------------------
    */   
   public boolean displayData() {
      return this.m_displayOutput;
   }

   public void exitSample (boolean disconnect) throws Exception {
     // disconnect is false when the application has already lost the connection to the service.
      if (disconnect) {         
       if (displayData()) {
          // The sample maintains the connection until the user presses Enter.
          // The reason for maintaining the connection is to allow clients and support to verify connectivity is needed.
            System.out.println("");
            System.out.println("Exiting application...");
            System.out.println("(press Enter to exit)");
            System.in.read();  
            try {
                this.marketDepthManager.disconnect();
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
    * @return connectionSettings
    */
   /**
    * Retrieves connection settings from either the command line or properties
    * file, and returns to the calling function.
    * 
    * @return connectionSettings
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
    * Called from the onConnected event, this function requests depth info for the specified symbol.
    */
   public void getDepthInfo (int index)
   {
      if (index == 0)
          m_appStartTime = markTime();
  
      for (int i = index; i < m_symbols.length; i++) {
          String symbol = m_symbols[i];
          try {
              // Submit the request and update counters
              marketDepthManager.getDepthInfo(symbol, consoleMarketDepthListener);
              m_activeRequests++;  
              
              if (m_activeRequests == MAX_CONCURRENT_REQUESTS) {
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
    * Called from onComplete event, this method increments the number of
    * completed requests and disconnects from the server if all
    * requests have been processed.
    */
   public void setGetDepthInfoComplete(boolean b) {
          // Increment number of completed requests
       ++this.m_count;
       --this.m_activeRequests;
       if (this.m_activeRequests < MAX_CONCURRENT_REQUESTS) {
         getDepthInfo(this.m_index);
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
}