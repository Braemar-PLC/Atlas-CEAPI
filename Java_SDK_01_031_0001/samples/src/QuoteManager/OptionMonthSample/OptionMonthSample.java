package QuoteManager.OptionMonthSample;

import java.io.IOException;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.DefaultQuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest.CHAIN_TYPE;
import com.esignal.jstandard.event.OptionMonthsListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;
import Util.SampleRunner;

/**
 * Provides methods for requesting and processing available months for options contracts.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class OptionMonthSample {

   // Variables to collect server and credentials from command line for
   // connection to data farm, and to track request completion.
   private String host      = "";
   private SockType connType;
   private String username  = "";
   private String password  = "";
   private String[] syms;
   private String chaintype = "";
   private int requestCount = 0;
   private String limit;
   private boolean displayOutput = true;

   // Declare quoteManager and event handler for option months
   public QuoteManager quoteManager;
   OptionMonthsListener consoleOptionListener = new MyConsoleOptionMonthsListener(this);

   //Declare properties file instance
   private Properties sampleProps = SampleProperties.getInstance().getProperties();

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
    *                              <li>STOCK_OPTIONS</li><br>
    *                                  Indicates the request is for a stock.<br>
    *                                  The returned data will be limited to the months and years 
    *                                  available for a given stock.<br><br></li>
    *                              <li>FUTURE_OPTIONS</li><br>
    *                                  Indicates the request is for a future.<br>
    *                                  The returned data will be limited to the months and years 
    *                                  available for a given future.<br><br></li>
    * @param <symbols>          The symbol, or symbols, to use for making the Option Month request. 
    *                           A single instrument may be entered, or a comma separated list of symbols. 
    *                           Symbols that include a space must be enclosed in quotes. 
    *                         
    *                           Stock Example:
    *                         
    *                              -s IBM
    *                            
    *                           Futures Example:
    *                         
    *                              -s ES
    *                            
    *                           Multiple Symbols Example:
    *                         
    *                              -s  IBM,AAPL
    *                            
    *                           IMPORTANT:
    *                           You cannot request stocks and futures together.
    */
   public OptionMonthSample(String host, SockType connType, String username, String password, String[] symbols, String chaintype, String timeLimit, boolean display) {
      try {
          this.host          = host;
          this.connType      = connType;
          this.username      = username;
          this.password      = password;
          this.chaintype     = chaintype;
          this.syms          = symbols;
          this.limit         = timeLimit;
          this.displayOutput = display;
       } catch (Exception e) {
          SampleProperties.getInstance().displayHelp(true);
          System.exit(-1);
       }
   }

   /**
    * Creates an instance of a quoteManager factory and a connection
    * listener. Connects to the farm using connection settings retrieved from
    * the command line via the getConnectionSettings() function.
    * 
    * @param factory
    *            Used to create a quote manager
    * @throws Exception
    */
   public void runOptionMonthSample(ResourceManagerFactory factory) throws Exception {
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
         MyConsoleConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);

         quoteManager.connect(this.getConnectionSettings(), consoleConnectionListener);

      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
   }

   /**
    * Called from onConnected event, this method makes a request once a
    * connection is established. Parameters are added to a QuoteRequest
    * object, and then passed in via the requestOptionMonths() function
    * of the quoteManager. An optional turnaround is set to track
    * multiple requests.
    */
   public void getData() {
      // Optional turnaround. Highly recommended when requesting multiple
      // symbols.
      long turnaround = 20000L;

      // Create instance of a QuoteRequest
      QuoteRequest quoteRequest = new DefaultQuoteRequest(turnaround);

      //Set the type of chain to be requested
      for (CHAIN_TYPE ct : CHAIN_TYPE.values()) {
         if (ct.toString().equalsIgnoreCase(chaintype)) {
            quoteRequest.setChainType(ct);
         }
      }

      //Request months for all symbols
      for (String symbol : syms) {
         quoteRequest.setTurnAround(turnaround++);
         this.quoteManager.requestOptionMonths(symbol, consoleOptionListener, quoteRequest);
         this.requestCount++;
      }
      
      // Run continuously?
      if (!this.limit.equalsIgnoreCase("0"))
        SampleRunner.runSampleForTime(this.limit, this);
      
      // note:  The above code will run the sample for the specified time limit, and then exit.
      //        However, this sample demonstrates non-streaming requests, therefore the time limit
      //        is not necessary.  The sample will exit after all requests are processed.
   }

   /**
    * Disconnects from the server.
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
    * Retrieves the number of requests made.
    * 
    * @return
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
