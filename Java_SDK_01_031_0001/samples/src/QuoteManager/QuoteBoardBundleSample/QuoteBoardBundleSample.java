package QuoteManager.QuoteBoardBundleSample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.ResourceStatus;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.BundleQuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest.CHAIN_TYPE;
import com.esignal.jstandard.beans.quote.dbc.DbcCallQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcMonthYearQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcPutQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcStrikeQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.dbc.DbcStrangeQuoteRequestFilter;
import com.esignal.jstandard.beans.quote.QuoteRequestFilter.OPERATORS;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.QuoteRequestListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;
import Util.SampleRunner;

/**
 * This sample allows one to specify an underlyer to retrieve a snapshot for all available contracts.
 * Supported contract types are future, future options and stock options contracts. Month, year and
 * put/call filters can further be specified for stock options and future options contracts.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */

public class QuoteBoardBundleSample {

   // Variables to collect symbols, credentials, server info and other relevant parameters 
   // from command line for connection to data farm, and to track request completion.
   private String host                  = "";
   private SockType connType;
   private String username              = "";
   private String password              = "";
   private String[] syms;               
   private String chainType             = "";
   private int monthfilter              = -1;
   private int dayfilter                = -1;
   private int yearfilter               = -1;
   private String putcallfilter         = "";
   private int requestCount             = 0;
   private long minstrike               = -1;
   private String minstrikeoperator     = "";
   private long maxstrike               = -1;
   private String maxstrikeoperator     = "";

   private String limit                 = "0";
   private boolean displayOutput        = true;

   
   // Declare quoteManager and event handler for returned data
   private QuoteManager quoteManager;
   QuoteRequestListener consoleQuoteListener = new MyConsoleQuoteListener(this);

   // Declare instance of properties file
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
    * @param <symbols>          Comma separated list of symbols for which to make a chain snapshot request. For example: O:AAPL,O:IBM
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
    */
   public QuoteBoardBundleSample(
		   		String host, SockType connType, String username, String password, 
		   		String[] symbols, String chaintype, 
		   		String month, String day, String year, String putcall, 
		   		String minstrike, String minstrikeoperator, String maxstrike, String maxstrikeoperator,
		   		String timeLimit, boolean display) {
      try {
         this.host              = host;
         this.connType          = connType;
         this.username          = username;
         this.password          = password;
         this.syms              = symbols;
         this.chainType         = chaintype;
         this.putcallfilter     = putcall;
         this.monthfilter       = Integer.parseInt(month);
         this.dayfilter         = Integer.parseInt(day);
         this.yearfilter        = Integer.parseInt(year);
         this.minstrike         = Integer.parseInt(minstrike);
         this.minstrikeoperator = minstrikeoperator;
         this.maxstrike         = Integer.parseInt(maxstrike);
         this.maxstrikeoperator = maxstrikeoperator;
         this.limit             = timeLimit;
         this.displayOutput     = display;

      } catch (Exception e) {
            SampleProperties.getInstance().displayHelp(true);
            System.exit(-1);
         }
   }

   /**
    * Creates an instance of a quoteManager factory and a connection listener. Connects to the farm using connection settings
    * retrieved from the command line via the getConnectionSettings() function.
    * 
    * @param factory
    *            Used to create a quote manager
    * @throws ResourceManagerException
    */
   public void runQuoteBoardBundleSample(ResourceManagerFactory factory) throws Exception {
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

         this.quoteManager = factory.createQuoteManager();
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
    * connection is established. Parameters are added to an QuoteRequest object,
    * and then passed in via the request()function of the quoteManager.
    * An optional turnaround is set to track multiple requests.
    */
   public void getData() {
      // Run continuously?
      if (!this.limit.equalsIgnoreCase("0"))
        SampleRunner.runSampleForTime(this.limit, this);
      
      // note:  The above code will run the sample for the specified time limit, and then exit.
      //        However, this sample demonstrates non-streaming requests, therefore the time limit
      //        is not necessary.  The sample will exit after all requests are processed.
	   
      try {
         long turnaround = 20000;

         // Create quoteRequest
         QuoteRequest bundleRequest = new BundleQuoteRequest(turnaround);
         for (CHAIN_TYPE ct : CHAIN_TYPE.values()) {
            if (ct.toString().equalsIgnoreCase(chainType)) {
               bundleRequest.setChainType(CHAIN_TYPE.valueOf(chainType));
            }
         }

         // Process filters
         if (!chainType.equals("FUTURE_CHAIN")) {
            if ((monthfilter != -1) && (yearfilter != -1)) {
               Calendar cal = Calendar.getInstance();
               cal.set(yearfilter, monthfilter, 1);
               bundleRequest.addFilter(new DbcMonthYearQuoteRequestFilter(cal.getTime()));
            }

            if (!putcallfilter.equals("none")) {

               if (putcallfilter.equals("call")) {
                  bundleRequest.addFilter(new DbcCallQuoteRequestFilter());
               } else if (putcallfilter.equals("put")) {
                  bundleRequest.addFilter(new DbcPutQuoteRequestFilter());
               }
            }

            if ((minstrike != -1) && (!minstrikeoperator.isEmpty())) {
               // NOTE:  There is no OPERATORS.EQUAL Value available in the JStandard API
               //        To get a strike EQUAL to a specific value you must set the 
               //        minstrike and maxstrike value to the same value
               for (OPERATORS ot : OPERATORS.values()) {
                  if (ot.toString().equalsIgnoreCase(minstrikeoperator)) {
                     bundleRequest.addFilter(new DbcStrikeQuoteRequestFilter(OPERATORS.valueOf(minstrikeoperator), minstrike * 1000));
                  }
               }
            }

            if ((maxstrike != -1) && (!maxstrikeoperator.isEmpty())) {
               // NOTE:  There is no OPERATORS.EQUAL Value available in the JStandard API
               //        To get a strike EQUAL to a specific value you must set the 
               //        minstrike and maxstrike value to the same value
               for (OPERATORS ot : OPERATORS.values()) {
                  if (ot.toString().equalsIgnoreCase(maxstrikeoperator)) {
                     bundleRequest.addFilter(new DbcStrangeQuoteRequestFilter(OPERATORS.valueOf(maxstrikeoperator), maxstrike * 1000));
                  }
               }
            }
         }

         // The API does not provide a Day Filter interface like, DbcStrikeQuoteRequestFilter.
         // So, the day filtering performed by this sample is done by parsing the instruments
         // returned by the request and checking for the day indicator that is part of the 
         // instrument syntax.
         //
         // Also see getDayFilter() in this source file.

         // Retrieve symbol list and make request for each item
         for (String symbol : syms) {
            quoteManager.request(symbol, consoleQuoteListener, bundleRequest);
            bundleRequest.setTurnAround(++turnaround);
            this.requestCount++;
         }
      } catch (Exception e) {
            SampleProperties.getInstance().displayHelp(true);
            System.exit(-1);
         }
   }

   /**
    * Provides the day filter
    * 
    * @return dayfilter an int value representing the day to filter, or -1
    */
   public int getDayFilter() {
      return dayfilter;
   }

   /**
    * Called to disconnect the quoteManager
    * 
    * @throws Exception
    */
   public void exitSample (boolean disconnect) throws Exception {
      if (disconnect) {    	  
         System.out.println("");
         System.out.println("Exiting application...");
         System.out.println("(press Enter to exit)");
         System.in.read();  
         try {
        	this.quoteManager.disconnect();
         } catch (ResourceManagerException rme) {
        	  rme.printStackTrace();
         }
      }
      System.exit(0);
   }

   /**
    * Returns the request count which was incremented when a request was made for each symbol
    * 
    * @return this.requestCount
    */
   public int getRequestCount() {
      return this.requestCount;
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
}