package QuoteManager.Level1QuoteRequest;

import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.DefaultQuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest;
import com.esignal.jstandard.beans.quote.QuoteRequest.DATASET_TYPE;
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
 * Provides methods for requesting and processing current snapshot or streaming data.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *
 */
public class Level1QuoteRequestSample {

   // Variables to collect symbols, credentials, server info and other relevant parameters 
   // from command line for connection to data farm, and to track request completion.
   private String host      = "";
   private SockType connType; 
   private String username  = "";
   private String password  = "";
   private String mode      = "";
   private String[] syms;
   private int requestCount = 0;
   private String limit;
   private boolean displayOutput = true;
   
   // Declare quoteManager 
   private QuoteManager quoteManager;

   // Declare instance of properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();
   private String datasetType;

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
    *            (Required) Please contact your sales or pre-sales representative
    *            to obtain a trial account if you do not have one.
    * @param <password>
    *            (Required) Please contact your sales or pre-sales representative to obtain
    *            a trial account if you do not have one.
    * @param <mode>
    *            Indicates whether the request is for snapshot or streaming data.
    *            Acceptable values are snapshot or subscribe.
    * @param <symbols>
    *            A comma delimited string, to be parsed into an array of symbols.
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
    *            
    * @param <datasetType>
    *           Specifies the type of dataset to be requested.
    *           Acceptable values are PRICING or FUNDAMENTAL(To request FactSet data and other Fundamental data).
    */
   public Level1QuoteRequestSample(String host, SockType connType, String username, String password, String mode, String[] symbols, String timeLimit, boolean display, String datasetType) {
      try {
         this.host          = host;
         this.connType      = connType;
         this.username      = username;
         this.password      = password;
         this.mode          = mode;
         this.syms          = symbols;
         this.limit         = timeLimit;
         this.displayOutput = display;
         this.datasetType    = datasetType;
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
   public void runQuoteBoardSample(ResourceManagerFactory factory) throws Exception {
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
	public void getData() {
		// Create a listener that implements both QuoteListener and QuoteRequestListener
		// interfaces.
		MyConsoleQuoteListener consoleQuoteListener = new MyConsoleQuoteListener(this);
		// When the line above is executed, the constructor for the downloads the qualifiers.tab file and 
		// loads the qualifiers information into memory.   

		// Optional turnaround for tracking      
		long turnaround = 20000L;
		QuoteRequest quoteRequest = new DefaultQuoteRequest(turnaround);

		// Set the dataset type for the request
		if (this.datasetType != null && !this.datasetType.isEmpty()) {
			DATASET_TYPE datasetTypeObject = DATASET_TYPE.getDasetType(this.datasetType);
			if (datasetTypeObject == null) {
				System.out.println("WARNING: Invalid dataset type specified. Defaulting to PRICING.");
				datasetTypeObject = DATASET_TYPE.PRICING;
			}
			quoteRequest.setDatasetType(datasetTypeObject);
		}

		// Determine if request is for snapshot data
		if (this.mode.equals("snapshot")) {
			// Use QuoteRequestListener for snapshots
			// Request each symbol on the list and increment the turnaround
			for (String symbol : syms) {
				quoteManager.request(symbol, consoleQuoteListener, quoteRequest);
				quoteRequest.setTurnAround(++turnaround);
				// Track number of requests made
				this.requestCount++;
			}
		}

		else {
			for (String symbol : syms) {
				quoteManager.subscribe(symbol, consoleQuoteListener);
				this.requestCount++;
			}
			// Run continuously?
			if (!this.limit.equalsIgnoreCase("0"))
				SampleRunner.runSampleForTime(this.limit, this);
		}
	}

   /**
    * Called to disconnect the quoteManager
    * 
    * @throws Exception
    */
   public synchronized void exitSample (boolean disconnect) throws Exception {
      if (disconnect) {   
         if (this.displayOutput) {
            System.out.println("");
            System.out.println("Exiting application...");
            System.out.println("(press Enter to exit)");
            System.in.read();  
         }
         try {
            this.quoteManager.disconnect();
         } catch (ResourceManagerException rme) {
            rme.printStackTrace();
         } 
      }
      System.exit(0);
   }

   public boolean displayData() {
        return displayOutput;
   }   
   
   public void unsubSymbol(String symbol, QuoteListener listener) {
      this.quoteManager.unsubscribe(symbol, listener);
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
