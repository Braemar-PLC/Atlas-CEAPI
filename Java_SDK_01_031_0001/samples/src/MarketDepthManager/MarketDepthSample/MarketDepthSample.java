package MarketDepthManager.MarketDepthSample;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.ConnectionFlags.CONNECTION_FLAGS;
import com.esignal.jstandard.beans.marketdepth.DefaultMarketDepthRequest;
import com.esignal.jstandard.beans.marketdepth.MarketDepthRequest;
import com.esignal.jstandard.beans.marketdepth.MarketDepthRequest.eDEPTH_SOURCE;
import com.esignal.jstandard.beans.marketdepth.MarketDepthRequest.eDEPTH_TYPE;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.MarketDepthListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;
import com.esignal.jstandard.managers.MarketDepthManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;

import Util.SampleProperties;
import Util.SampleRunner;

/**
 * Provides methods for requesting and processing market depth for a symbol.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *
 */
public class MarketDepthSample {
   
   // Variables to collect symbols, credentials, server info and other relevant parameters 
   // from command line for connection to data farm, and to track request completion.
   private Properties sampleProps;
   private String host = "";
   private SockType connType;
   private String username = "";
   private String password = "";
   public int depthType = 0;
   public int depthSource = 0;
   private String[] syms;
   private String mode;
   private String limit;
   private boolean displayOutput = true;

   // Declare MarketDepthManager
   MarketDepthManager marketDepthManager;
   MarketDepthListener consoleMarketDepthListener ;
   
   /**
    * @param <host>
    *            <host> Specifies connection string for ICE Data Services network.
    *            Usually cm*.dataservices.theice.com.
    * @param <connType>     
    *            Specifies the type of connection to use with the ICE Data Services network.
    *            
    *               0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
    *               1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
    *               2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
    * @param <username>
    *            <username> Supplied by ICE Data Services. Please contact your sales or
    *            pre-sales representative to obtain a trial account if you do not
    *            have one.
    * @param <password>
    *            Supplied by ICE Data Services. Please contact your sales or
    *            pre-sales representative to obtain a trial account if you do not
    *            have one.
    * @param <type>
    *            Specifies the Depth Type used to get the Level2 data.
    * @param <source>
    *            Specifies the Depth Source used to get the Level2 data.
    * @param <symbols>
    *            Comma separated list of symbols for which to make a Depth
    *            request. Multiple symbols must be enclosed in quotes with no
    *            spaces between symbols. For example: "IBM,GE,WFM"
    * @param <mode>
    *            Specifies the mode of operation. Takes in 'info' or 'stream'
    *
    * @param <timeLimit> 
    *            The duration this sample will run before exiting.
    *            Time duration applies to subscribe requests only.
    *            Syntax for setting the time duration is: 
    *                      
    *               HH:MM:SS
    *                         
    *            Setting the limit to 0 or 00:00:00 indicates to run
    *            continuously without end; sample application must be
    *            forcibly stopped.
    * @param <display>
    *            Specifies the type of output to produce. 
    *            True indicates format for console display; False indicates format for csv file.
    * @throws Exception
    */
   public MarketDepthSample(String host, SockType connType, String username, String password, String depthType, String depthSource, String symbols, String mode, String limit, boolean display) {
      int DepthType = 0;
      int DepthSource = 0;
      
      // This sample doesn't provide a command line argument for depth filters.  
      // You can specify a filter in the properties file for this sample.
      
      // There are 5 depth types available.   
      try {
         DepthType = Integer.parseInt(depthType);
      }
      catch (NumberFormatException e)
      {
         if (depthType.equalsIgnoreCase("ORDER_BOOK")) 
            DepthType = eDEPTH_TYPE.ORDER_BOOK.getCode();
         if (depthType.equalsIgnoreCase("BOOK_BY_PRICE"))
            DepthType = eDEPTH_TYPE.BOOK_BY_PRICE.getCode();
         if (depthType.equalsIgnoreCase("BOOK_BY_MARKET_PARTICIPANT"))
            DepthType = eDEPTH_TYPE.BOOK_BY_MARKET_PARTICIPANT.getCode();
         if (depthType.equalsIgnoreCase("BOOK_BY_LEVEL"))
            DepthType = eDEPTH_TYPE.BOOK_BY_LEVEL.getCode();
         if (depthType.equalsIgnoreCase("BOOK_BY_MARKET_PARTICIPANT_EXTENDED"))
            DepthType = eDEPTH_TYPE.BOOK_BY_MARKET_PARTICIPANT_EXTENDED.getCode();
      }
      
      // There are 135 depth sources available.  This sample can request data from all available sources
      // if the source is specified as an integer (ie 87 for ASX Equities).  This sample also permits users
      // to enter a source description equivalent for a depth source, but it only processes 19 of the available
      // sources in this fashion.
      try {
         DepthSource = Integer.parseInt(depthSource);
      }
      catch (NumberFormatException e)
      {
         if (depthSource.equalsIgnoreCase("NASDAQ"))
            DepthSource = eDEPTH_SOURCE.NASDAQ.getCode();
         if (depthSource.equalsIgnoreCase("NYSE"))
            DepthSource = eDEPTH_SOURCE.NYSE.getCode();
         if (depthSource.equalsIgnoreCase("ARCA"))
            DepthSource = eDEPTH_SOURCE.ARCA.getCode();
         if (depthSource.equalsIgnoreCase("PINKSHEET"))
            DepthSource = eDEPTH_SOURCE.PINKSHEET.getCode();
         if (depthSource.equalsIgnoreCase("BULLETIN_BOARD"))
            DepthSource = eDEPTH_SOURCE.BULLETIN_BOARD.getCode();
         if (depthSource.equalsIgnoreCase("CME"))
            DepthSource = eDEPTH_SOURCE.CME.getCode();
         if (depthSource.equalsIgnoreCase("CBOT"))
            DepthSource = eDEPTH_SOURCE.CBOT.getCode();
         if (depthSource.equalsIgnoreCase("NYMEX"))
            DepthSource = eDEPTH_SOURCE.NYMEX.getCode();
         if (depthSource.equalsIgnoreCase("COMEX"))
            DepthSource = eDEPTH_SOURCE.COMEX.getCode();
         if (depthSource.equalsIgnoreCase("TSX"))
            DepthSource = eDEPTH_SOURCE.TSX.getCode();
         if (depthSource.equalsIgnoreCase("CL2"))
            DepthSource = eDEPTH_SOURCE.CL2.getCode();
         if (depthSource.equalsIgnoreCase("ATS_ALPHA"))
            DepthSource = eDEPTH_SOURCE.ATS_ALPHA.getCode();
         if (depthSource.equalsIgnoreCase("ATS_CHIX"))
            DepthSource = eDEPTH_SOURCE.ATS_CHIX.getCode();
         if (depthSource.equalsIgnoreCase("ATS_PURE"))
            DepthSource = eDEPTH_SOURCE.ATS_PURE.getCode();
         if (depthSource.equalsIgnoreCase("ATS_OMEGA"))
            DepthSource = eDEPTH_SOURCE.ATS_OMEGA.getCode();
         if (depthSource.equalsIgnoreCase("TSX_CONSOLIDATED"))
            DepthSource = eDEPTH_SOURCE.TSX_CONSOLIDATED.getCode();
         if (depthSource.equalsIgnoreCase("ICE_US"))
            DepthSource = eDEPTH_SOURCE.ICE_US.getCode();
         if (depthSource.equalsIgnoreCase("LON"))
            DepthSource = eDEPTH_SOURCE.LON.getCode();
         if (depthSource.equalsIgnoreCase("ASX"))
            DepthSource = eDEPTH_SOURCE.ASX.getCode();
      }
      
      if (DepthType == 0)
         DepthType = -1;
      
      if (DepthSource == 0)
         DepthSource = -1;
      
      sampleProps = SampleProperties.getInstance().getProperties();
      this.host = host;
      this.connType = connType;
      this.username = username;
      this.password = password;
      this.depthType = DepthType;
      this.depthSource = DepthSource;
      this.syms = symbols.split(",");
      this.mode = mode;
      this.limit = limit;
      this.displayOutput = display;
   }
   
   public synchronized void exitSample (boolean disconnect) throws Exception {
      if (disconnect) {
    	  Thread.sleep(2000);
    	  long turnAround = 2001;
    	  for (String symbol : syms) {
    		  marketDepthManager.unsubscribe(symbol,consoleMarketDepthListener, turnAround);
    		  ++turnAround;
    	  }
    	  if (this.displayOutput) {    	 
           System.out.println("");
           System.out.println("Exiting application...");
           System.out.println("(press Enter to exit)");
           System.in.read();  
        }
        
         try {
            this.marketDepthManager.disconnect();
         } catch (ResourceManagerException rme) {
            rme.printStackTrace();
         }
      }
      System.exit(0);
   }

   public boolean displayData() {
   	  return displayOutput;
   }

   /**
    * Creates an instance of a marketDepthManager factory and a connection
    * listener. Connects to the farm using connection settings retrieved from
    * the command line via the getConnectionSettings() function.
    * 
    * @param factory
    * @throws Exception
    */
   public void runMarketDepthSample(ResourceManagerFactory factory) throws Exception {
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

         marketDepthManager = factory.createMarketDepthManager();
         ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
         ConnectionSettings conSet = this.getConnectionSettings();
         conSet.setFlags(CONNECTION_FLAGS.DBCAPI_FLAG_DEPTH_EX.getCode());
         marketDepthManager.connect(conSet, consoleConnectionListener);
      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
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
   
   /**
    * Called from the onConnected event, this function checks if the request is for Depth info and returns true otherwise false
    * 
    * @return boolean 
    */
   public boolean isInfo(){
      return mode.toLowerCase().matches("info");
   }
   
   /**
    * Called from the MyConsoleMarketDepthListener onInfo event.
    * This function provides the number of instruments requested so the onInfo event will know
    * when to exit.
    */
   public int getSymbolCount() {
      return syms.length;
   }
   
   /**
    * Called from the onConnected event, this function requests market depth data for the specified symbol.  
    */
   public void marketDepth()
   {
      try {
         boolean info = isInfo();
         long turnAround = 2001;
         consoleMarketDepthListener = new MyConsoleMarketDepthListener(this);
         
         int filter = Integer.parseInt(this.sampleProps.getProperty("samples.marketdepthsample.filter"));
         boolean useDefault = Boolean.parseBoolean(this.sampleProps.getProperty("samples.marketdepthsample.usedefault"));

         // If "useDefault" is true the Market Depth server will return the "default" depth data for a given instrument.
         // "Default" depth data is the default Depth Type from the default Depth Source for a given instrument.
         if (useDefault) { // Override the other settings
            depthType = -1;
            depthSource = -1;
            filter = -1;
         }
         
         // Check if the defaults (-1) are set for depthType and depthSource.  If they are then ensure useDefault is set to true
         // This can happen if the user doesn't specify a type or source, and also doesn't specify to useDefault
         if (depthType == -1 && depthSource == -1)
            useDefault = true;
         
         for (String symbol : syms) {
            MarketDepthRequest req = new DefaultMarketDepthRequest(symbol, turnAround, depthSource, depthType, filter, useDefault);
            if(info){
               marketDepthManager.getDepthInfo(symbol, consoleMarketDepthListener);
            }
            else {
               marketDepthManager.subscribe(symbol, consoleMarketDepthListener, req);
               // Run continuously?
               if (!this.limit.equalsIgnoreCase("0"))
            	  SampleRunner.runSampleForTime(this.limit, this);
            }
            req.setTurnAround(++turnAround);
         }   
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}