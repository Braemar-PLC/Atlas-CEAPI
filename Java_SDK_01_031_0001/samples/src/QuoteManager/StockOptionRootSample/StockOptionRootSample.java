package QuoteManager.StockOptionRootSample;

import java.io.IOException;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.StockOptionRootsListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;

/**
 * This sample allows one to retrieve all available stock option roots for a given stock.
 * This information can then be used to set the underlyer in the OptionMonthSample, the 
 * QuoteChainSample or the QuoteBoardBundleSample.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class StockOptionRootSample {
   
   // Variables to collect server and credentials from command line for
   // connection to data farm, and to track request completion.
   private String host        = "";
   private SockType connType;
   private String username    = "";
   private String password    = "";
   private String[] syms;
   private int requestCount   = 0;
   
   //Declare QuoteManager and event handler for returned data
   private QuoteManager quoteManager;
   StockOptionRootsListener stockOptionRootsListener = new MyConsoleStockOptionRootsListener(this);
   
   //Declare instance of properties file
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
    * @param <symbols>          Comma separated list of symbols for which to make a stock option root 
    *                           request. Multiple symbols must be enclosed in quotes with no 
    *                           spaces between symbols. For example: "IBM,GE,WFM"
    * @param <password>         Must be entered at command line. Please contact your sales or
    *                           pre-sales representative to obtain a trial account if you do not
    *                           have one.
    */
   public StockOptionRootSample(String host, SockType connType, String username, String password, String[] symbols)
   {
      this.host       = host;
      this.connType   = connType;
      this.username   = username;
      this.password   = password;
      this.syms       = symbols;
   }
   
   /**
    * Creates an instance of a quoteManager factory and a connection listener. Connects to the farm using 
    * connection settings retrieved from the command line via the getConnectionSettings() function.
    *  
    * @param factory Used to create a Quote manager
    * @throws Exception
    */
   public void runStockOptionRootSample(ResourceManagerFactory factory) throws Exception {
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

         ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
         this.quoteManager = factory.createQuoteManager();         
         quoteManager.connect(getConnectionSettings(), consoleConnectionListener);
      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
   }
   
   /**
    * Called from onConnected, this function requests data once a connection is made
    */
   public void getData () {
      
      //Parses symbol list in the proerties file and makes a request for each
      for (String symbol : syms) {
            quoteManager.requestStockOptionRoots(symbol, stockOptionRootsListener);
            this.requestCount++;
      }
   }
   
   /**
    * Disconnects from the server
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
    * Tracks number of requests made
    * @return
    */
   public int getRequestCount () {
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
      
      // If samples.connection.useproxy in StockOptionRootSample.properties is set
      // to true, collect proxy settings
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
