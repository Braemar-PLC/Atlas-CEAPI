package QuoteManager.QuoteChainSample;

import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.DefaultQuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequestFilter;
import com.esignal.jstandard.beans.quote.QuoteRequest.CHAIN_TYPE;
import com.esignal.jstandard.beans.quote.dbc.DbcCallQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcMonthYearQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcPutQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcStrikeQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcStrangeQuoteRequestFilter;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.QuoteListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;
import Util.SampleRunner;

/**
 * Provides methods to specify an underlyer to retrieve streaming updates for all available contracts. Supported
 * contract types are future, future options and stock options contracts.  Month, year and put/call filters can 
 * further be specified for stock options and future options contracts.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class QuoteChainSample {
   
   // Variables to collect server and credentials from command line for
   // connection to data farm, and to track request completion.
   private String host               = "";
   private SockType connType;
   private String username           = "";
   private String password           = "";
   private String[] syms;            
   private String chainType          = "";
   private String putcallfilter      = "";
   private int monthfilter           = 0;
   private int yearfilter            = 2024;
   private long minStrike            = 0;
   private long maxStrike            = 0;
   private long turnaround           = 99;
   private String limit;
   private boolean isSnapshotRequest = false;
   
   private boolean displayOutput     = true;
   
   //Declare quoteManager and event handler for returned data
   private QuoteManager quoteManager;
   private QuoteListener consoleQuoteListener = new MyConsoleQuoteListener(this);

   //Declare instance of properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();
   
   /**
    * @param <host>             Specifies the address to use to connect to the ICE Data Services 
    *                           network. For example: cm*.dataservices.theice.com
    * @param <connetionType>    Specifies the type of connection to use with the ICE Data Services
    *                           network.  Valid values include:
    *
    *                              0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
    *                              1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
    *                              2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
    *
    * @param <username>         The username for the account entitled to receive data from ICE Data Services.
    *                           If you do not have one, please contact your ICE Data Services representative.
    * @param <password>         The password for the account entitled to receive data from ICE Data Services.
    *                           If you do not have one, please contact your ICE Data Services representative.
    * @param <type>             Specifies one of the following request types:<br><ul>
    *                           <li>STOCK_OPTIONS</li><br>
    *                               Indicates the snapshot request is for a stock.<br>
    *                               The returned data will be limited to a given month and year, and to either puts or calls.<br><br></li>
    *                           <li>FUTURE_OPTIONS</li><br>
    *                               Indicates the snapshot request is for a future.<br>
    *                               The returned data will be limited to a given month and year, and to either puts or calls.<br><br></li>
    *                           <li>FUTURE_CHAIN</li><br>
    *                               If the instrument is a stock, the returned data includes all available options, puts and calls, for the stock specified.<br><br>
    *                               If the instrument is a future, the returned data is a list of all the available month roots and their expirations.<br><br>
    *                               For example:<br><br>
    *                                  SYMBOL: ES H5          EXPIRATION: 3202015   TYPE: ---<br>       
    *                                  SYMBOL: ES #V          EXPIRATION: 3202015   TYPE: ---<br>   
    *                                  SYMBOL: ES #F          EXPIRATION: 3202015   TYPE: ---<br>
    *                                  SYMBOL: ES U5          EXPIRATION: 9182015   TYPE: ---<br>   
    *                                  SYMBOL: ES M5          EXPIRATION: 6192015   TYPE: ---<br>   
    *                                  SYMBOL: ES H6          EXPIRATION: 3182016   TYPE: ---<br>   
    *                                  SYMBOL: ES Z5          EXPIRATION: 12182015  TYPE: ---<br>   
    *                                  SYMBOL: ES #OI         EXPIRATION: 12182015  TYPE: ---<br><br>      
    *                               Subsequent FUTURE_OPTIONS requests could be made to get snapshots of all the March Puts, or September Calls.<br><br>
    *                               note:  Returned data may include instruments with # formats.  These instruments are not supported for FUTURE_OPTIONS requests.
    * @param <month>            A number representing a contract month.  Possible values include 0 through 11, where 0 is January and 11 is December.
    * @param <year>             A four digit year code.
    * @param <put/call>         Indicates to retrieve "puts" or "calls" for the given contract, month, and year.<br>
    *                           For example:<br><br>
    *                              -pc put
    *
    * @param <minstrike>        The minimum strike price boundary for returning data.
    * @param <maxstrike>        The maximum strike price boundary for returning data.
    *                           
    * @param <timeLimit>        The duration this sample will run before exiting.
    *                           Time duration applies to subscribe requests only.
    *                           Syntax for setting the time duration is: 
    *                                     
    *                              HH:MM:SS
    *                                        
    *                           Setting the limit to 0 or 00:00:00 indicates to run
    *                           continuously without end; sample application must be
    *                           forcibly stopped.
    * 
    * @param <snapshotRequest>  true if the request is a snapshot request; false otherwise.
    *
    * @param <display>          Specifies the type of output to produce. 
    *                           True indicates format for console display; False indicates format for csv file.
    * 
    */
   public QuoteChainSample(String host, SockType connType, String username, String password, String[] symbols, String chaintype, 
                         String month, String year, String putcall, String minStrike, String maxStrike, String timeLimit, 
                         boolean snapshotRequest, boolean display) {
      
      // Variables to collect server and credentials from command line for
      // connection to data farm, and to track request completion.
      this.host        = host;
      this.connType    = connType;
      this.username    = username;
      this.password    = password;
      this.syms        = symbols;
      this.chainType   = chaintype;
      
      if (!month.isEmpty())
         this.monthfilter = Integer.parseInt(month);
      else
         this.monthfilter = -1;
      
      if (!year.isEmpty())
         this.yearfilter = Integer.parseInt(year);
      else
         this.yearfilter = -1;
      
      if (!minStrike.isEmpty())
         this.minStrike = Long.parseLong(minStrike);
      else
         this.minStrike = -1;
      
      if (!maxStrike.isEmpty())
         this.maxStrike = Long.parseLong(maxStrike);
      else
         this.maxStrike = -1;
      
      this.putcallfilter = putcall;  
      this.limit = timeLimit;      
      this.isSnapshotRequest = snapshotRequest;      
      this.displayOutput = display;
      ((MyConsoleQuoteListener) this.consoleQuoteListener).setSnapshot(isSnapshotRequest);      
   }
   
   /**
    * Creates an instance of a quoteManager factory and a connection
    * listener. Connects to the farm using connection settings retrieved from
    * the command line via the getConnectionSettings() function.
    * 
    * @param factory Used to create a quote manager
    * @throws ResourceManagerException
    */
   public void runQuoteChainSample(ResourceManagerFactory factory) throws Exception {
      try {
         // Check if the client will use Amazon Web Services
         if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useAWS"))) {                
            UsernameInfo info = factory.acquireUsername(this.getConnectionSettings());
             
            if (info.getStatusEnum() == DBCAPI.SUCCESS ) {
               this.username = info.getUsername();
               this.password = info.getPassword();                            
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

         quoteManager = factory.createQuoteManager();
         ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
         
         quoteManager.connect(this.getConnectionSettings(), consoleConnectionListener);
         
      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
   }

   /**
    * Returns the chain type requested.
    *
    * @return CHAIN_TYPE
    */
   public CHAIN_TYPE getChainType() {
      return CHAIN_TYPE.valueOf(this.chainType);
   }
   
   /**
    * Called from onConnected event, this method makes a request once a
    * connection is established. Parameters are added to a
    * QuoteRequest object, and then passed in via the request()
    * function of the quoteManager. An optional turnaround is set to track
    * multiple requests.
    */
   public void getData () {
      ++turnaround;
      
      //Create an instance of a QuoteRequest with turnaround for tracking multiple requests
      QuoteRequest defaultRequest = new DefaultQuoteRequest(turnaround);
      
      //Determine type of chain to be requested
      for (CHAIN_TYPE ct : CHAIN_TYPE.values()) {
         if (ct.toString().equalsIgnoreCase(chainType)) {
            defaultRequest.setChainType(CHAIN_TYPE.valueOf(chainType));
         }
      }                        
      
      //Make request based on chain type
      if (!chainType.equals("FUTURE_CHAIN"))
       { 
         if ((monthfilter != -1) && (yearfilter != -1))
         {
            Calendar cal = Calendar.getInstance();   
            cal.set(yearfilter, monthfilter, 1);
            defaultRequest.addFilter(new DbcMonthYearQuoteRequestFilter(cal.getTime()));   
         }               
      
         if (!putcallfilter.equals ("none")) {

            if (putcallfilter.equals ("call")) {
               defaultRequest.addFilter(new DbcCallQuoteRequestFilter());   
            }
            else if (putcallfilter.equals ("put")) {
               defaultRequest.addFilter(new DbcPutQuoteRequestFilter());   
            }
         }
       }   
            
      // Set Price Filtering
      // note:  This sample only uses the GREATER_THAN_OR_EQUAL and LESS_THAN_OR_EQUAL Operators
      // 
      // IMPORTANT:  The strike values must be whole numbers and must be multiplied by 1000.  Failure to multiply by 1000
      //             will produce faulty results.
      
      // Set the MINIMUM Strike Limit
      if (minStrike != -1)
         defaultRequest.addFilter(new DbcStrikeQuoteRequestFilter(QuoteRequestFilter.OPERATORS.GREATER_THAN_OR_EQUAL, minStrike*1000));
      
      // Set the MAXIMUM Strike Limit
      if (maxStrike != -1)
         defaultRequest.addFilter(new DbcStrangeQuoteRequestFilter(QuoteRequestFilter.OPERATORS.LESS_THAN_OR_EQUAL, maxStrike*1000));
      
      for (String symbol : syms){         
         quoteManager.subscribeChain(symbol, consoleQuoteListener, defaultRequest);      
         defaultRequest.setTurnAround(++turnaround);
      }
      
      if (!this.isSnapshotRequest) {
         // Run continuously?
         if (!this.limit.equalsIgnoreCase("0")) {          
            SampleRunner.runSampleForTime(this.limit, this);
         }
      }
   }
   
   public void removeSymbol(String symbol) {
      quoteManager.unsubscribe(symbol, consoleQuoteListener);
   }
   
   /**
    * Called to disconnect the quoteManager
    * @throws Exception
    */
   public synchronized void exitSample (boolean disconnect) throws Exception {
      if (disconnect) {   
         try {           
            this.quoteManager.disconnect();
         } catch (ResourceManagerException rme) { 
            rme.printStackTrace();
         }
         
         if (!this.isSnapshotRequest)
            Thread.sleep(2000);
          
         if (this.displayOutput) {         
            System.out.println("");
            System.out.println("Exiting application...");
            System.out.println("(press Enter to exit)");
            System.in.read();
         }             
      }
      System.exit(0);
   }
   
   public boolean isSnapshot() {
      return isSnapshotRequest;
   }

   public boolean displayData() {
        return displayOutput;
   }   

   /**
    * Retrieves connection settings from either the command line or properties
    * file, and returns to the calling function.
    * 
    * @return connectionSettings
    */
   private ConnectionSettings getConnectionSettings() {

      ConnectionSettings connectionSettings = new ConnectionSettings(host, username, password, connType);
      
      if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useproxy"))) {
         String proxyusername="";
         String proxypassword="";
         connectionSettings.setProxyInfo(sampleProps.getProperty("samples.connection.proxyhost"),
               Integer.parseInt(sampleProps.getProperty("samples.connection.proxyport")),
               proxyusername, proxypassword);
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
}