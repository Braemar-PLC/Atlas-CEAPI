package QuoteManager.SymbolCheckSample;

import java.io.IOException;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.LOOKUPTYPE;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.QuoteKeyListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;

/**
 * Provides methods for requesting and processing current snapshot or streaming data.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *
 */
public class SymbolCheckSample {

   // Variables to collect symbols, credentials, server info and other relevant parameters 
   // from command line for connection to data farm, and to track request completion.
   private String host            = "";
   private SockType connType;
   private String username        = "";
   private String password        = "";
   private String[] syms;         
   private boolean failedOnly     = false;
   private boolean readyToRequest = true;  // Set to true to perform the initial request
   
   private LOOKUPTYPE conversionType;
   private int requestCount = -1;

   // Declare quoteManager and consoleKeyListener 
   private QuoteManager quoteManager;
   private QuoteKeyListener consoleKeyListener = new MyConsoleSymbolCheckListener(this);
   
   // Declare instance of properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();
   
   /**
    * @param <host>          Specifies connection string for ICE Data Services network.
    *                        Usually cm*.dataservices.theice.com.
    * @param <username>      The username for the account entitled to receive data from ICE Data Services.
    *                        If you do not have one, please contact your ICE Data Services representative.
    * @param <password>      The password for the account entitled to receive data from ICE Data Services.
    *                        If you do not have one, please contact your ICE Data Services representative.
    * @param <conversion>    Indicates the type of lookup/conversion to perform.  For example, for a provided 
    *                        SEDOL, lookup and return the associated instruments.  Valid values include any of
    *                        the LOOKUPTYPE enum values.
    * @param <symbols>       One or more symbols for which to make a <code>keyLookup</code> request. Multiple 
    *                        must be separated by commas with no spaces. If one of the symbols contains a space,
    *                        the entire list must be enclosed in quotes. All symbols must be of the same conversion type.
    *                        For example, you may not mix SEDOL requests with CUSIP requests.
    * @param <failedOnly>    Print only if the request failed; do not print the successful results.  <codE>true</code> to print
    *                        failures only, <code>false</code> to print everything.  The default is <code>false</code>.
    */
   public SymbolCheckSample(String host, SockType connType, String username, String password, String conversion, String[] symbols, boolean failedOnly) {
      try {
         this.host = host;
         this.connType = connType;
         this.username = username;
         this.password = password;
         this.failedOnly = failedOnly;
           
         if (conversion.equalsIgnoreCase("SEDOL")) 
               this.conversionType = LOOKUPTYPE.SEDOL;
           else if (conversion.equalsIgnoreCase("CUSIP"))
               this.conversionType = LOOKUPTYPE.CUSIP;
           else if (conversion.equalsIgnoreCase("ISIN"))
               this.conversionType = LOOKUPTYPE.ISIN;
           else if (conversion.equalsIgnoreCase("WKN"))
               this.conversionType = LOOKUPTYPE.WKN;
           else if (conversion.equalsIgnoreCase("SYM2CUSIP"))
              this.conversionType = LOOKUPTYPE.SYM_TO_CUSIP;
           else if (conversion.equalsIgnoreCase("SYM2MIC"))
              this.conversionType = LOOKUPTYPE.SYM_TO_MIC;
           else if (conversion.equalsIgnoreCase("OSI21"))
              this.conversionType = LOOKUPTYPE.OSI21;
           else if (conversion.equalsIgnoreCase("IDCO22"))
              this.conversionType = LOOKUPTYPE.IDCO22;
         
         this.syms = symbols;
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
    * @throws Exception
    */
   public void runSymbolCheckSample(ResourceManagerFactory factory) throws Exception {            
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
    * Called from the onConnected event, this function requests snapshot or streaming 
    * data based on the mode parameter.  
    */
   public void getData () {
      if (performNextRequest()) {
         setReadyToRequest(false);
         requestCount++;
         if (requestCount < syms.length) {
            quoteManager.keyLookup(syms[requestCount], this.conversionType, consoleKeyListener);
         }
      }
   }
   
   public void setReadyToRequest(boolean nextRequest) {
      readyToRequest = nextRequest;
   }
   
   public boolean performNextRequest() {
      return readyToRequest;
   }
   
   /**
    * Called to disconnect the quoteManager
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
    * @return this.requestCount
    */
   public int getRequestCount () {
      return syms.length;  // the request count *will be* the length of the symbol list;
                             // therefore we use syms.length here. In addition requestCount is being used
                           // to perform request-- ie syms[requestCount];
   }
   
    /**
     * Returns the option value for printing only the failed items or printing everything
     * @return this.failedOnly
     */
    public boolean printFailedOnly () {
        return this.failedOnly;
    }

    /**
    * Returns the conversion type for this request.
    * @return this.conversionType
    */
   public LOOKUPTYPE getConversionType () {
       return this.conversionType;
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
