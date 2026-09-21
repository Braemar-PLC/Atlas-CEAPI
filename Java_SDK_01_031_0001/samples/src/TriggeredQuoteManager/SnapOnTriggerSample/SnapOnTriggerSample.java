package TriggeredQuoteManager.SnapOnTriggerSample;

import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.exception.ResourceManagerException;

import java.util.Arrays;
import java.util.Properties;

import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.managers.UsernameInfo;

import Util.SampleProperties;
import Util.SampleRunner;

import com.esignal.jstandard.managers.TriggeredQuoteEventManager;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

/**
 * Provides methods for requesting and processing current snapshot or streaming data.
 *
 * This sample is designed to provide basic market data output to screen or detailed
 * market data for redirection to a file.
 * 
 * When the display setting is ON, the sample will provide basic market data out
 * to the console window.
 * When the display setting is OFF, the sample will provide detailed market data in
 * csv format; you will need to specify the redirection and filename.
 * 
 * For example:
 *   java -cp .;JStandardSamples.jar;lib/* TriggeredQuoteManager.SnapOnTriggerSample.Main -h host -u user -p pwd -m addTrigger -limit 00:05:10 -disp off > detailedoutput.csv
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 */
public class SnapOnTriggerSample {

   // Variables to collect symbols, credentials, server info and other relevant parameters 
   // from command line for connection to data farm, and to track request completion.
   private String   host         = "";
   private SockType connType     = SockType.SOCKTYPE_LEGACY;
   private String   username     = "";
   private String   password     = "";
   private String   mode         = "";
   private String[] syms;
   private String[] triggers     = null;
   private String   listRequest  = "";
   private int      requestCount = 0;
   private String   limit        = "";
   private boolean  isList       = false;
   private String[] triggerNames;
   
   private boolean displayOutput = true;
   
   // Declare triggeredQuoteManager
   private TriggeredQuoteEventManager triggeredQuoteManager;
   
   private SampleTriggeredQuoteEventListener triggeredQuoteListener;

   // Declare instance of properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();

   /**
    * @param <host>           Specifies the address to use to connect to the ICE Data Services 
    *                         network. For example: cm*.dataservices.theice.com
    *
    * @param <connType>       Specifies the type of connection to use with the ICE Data Services
    *                         network.  Valid values include:
    *                         
    *                            0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
    *                            1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
    *                            2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
    *                         
    *                         note: Currently, secured TLS connections are not suppoorted for the
    *                               Snap-on-Trigger service.
    *                         
    * @param <username>       The username for the account entitled to receive data from ICE Data Services.
    *                         If you do not have one, please contact your ICE Data Services representative.
    * @param <password>       The password for the account entitled to receive data from ICE Data Services.
    *                         If you do not have one, please contact your ICE Data Services representative.
    * @param <triggers>       An array of JSON Strings.  Each JSON String represents a defined
    *                         Snap-on-Trigger request.    
    *                                
    * @param <limit>          The duration this sample will run before exiting.
    *                         Time duration applies to subscribe requests only.
    *                         Syntax for setting the time duration is:
    *                         
    *                            HH:MM:SS
    *                         
    *                         Setting the limit to 0 or 00:00:00 indicates to run
    *                         continuously without end; sample application must be
    *                         forcibly stopped.
    *
    * @param <listRequest>    A JSON String formatted to request the active triggers from the API.
    *                         The active triggers are stored in API memory.
    *
    * @param <isList>         <code>true</code> if the <code>listRequest</code> argument should be used;
    *                         <code>false</code> otherwise.
    * 
    * @param <display>        Specifies the type of output to produce. 
    *                         True indicates format for console display; False indicates format for csv file.
    */
   public SnapOnTriggerSample(String host, SockType connType, String username, String password, 
                            String[] triggers, String limit, String listRequest, boolean isList, boolean display) {
      try {
          this.host          = host;
          this.connType      = connType;
          this.username      = username;
          this.password      = password;
          this.triggers      = Arrays.copyOf(triggers, triggers.length);
          this.limit         = limit;
          this.listRequest   = listRequest;
          this.isList        = isList;
          this.triggerNames  = new String[triggers.length];
          this.displayOutput = display;
       } catch (Exception e) {
          SampleProperties.getInstance().displayHelp(true);
          System.exit(-1);
       }    
   }

   public void runSnapOnTriggerSample(ResourceManagerFactory factory) throws Exception {
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
        
        this.triggeredQuoteManager = factory.createTriggeredQuoteEventManager();    
        ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
        triggeredQuoteManager.connect(this.getConnectionSettings(), consoleConnectionListener);      
      } catch (ResourceManagerException rme) {
         rme.printStackTrace();
      }
   }
   
   public void addTriggerName(String name, int index) {
      triggerNames[index] = name;
   }
   
   public boolean isListRequest() {
      return this.isList;
   }

    public void listTriggers() {
      // Request the API to return the active list of triggers
      String triggerList = triggeredQuoteManager.listAllTriggers(listRequest);
      
      if (displayData()) {
         System.out.println("\nActive List of Triggers:  [JSON document]");
         System.out.println("   " + triggerList + "\n");
      }
   }

   /**
     * Called from the onConnected event, this function requests snapshot or streaming
     * data based on the mode parameter.
     */
    public void getData() {
       // Create a listener that implements both QuoteListener and QuoteRequestListener
       // interfaces.
       triggeredQuoteListener = new SampleTriggeredQuoteEventListener(this);
       // Executing the line above will call the constructor, which downloads the qualifiers.tab file
       // and loads the qualifiers information into memory.  

       for (String triggeredRequest : triggers) {
          triggeredQuoteManager.addTrigger(triggeredRequest, triggeredQuoteListener);
       }
       
       // Run continuously?
       if (!this.limit.equalsIgnoreCase("0"))
        SampleRunner.runSampleForTime(this.limit, this);
    }
   

   /**
     * Called to disconnect the TriggeredQuoteManager
     * 
     * @throws Exception
     */
    public synchronized void exitSample (boolean disconnect) throws Exception {
       if (disconnect) {
          for (String trigger : triggerNames) {
              triggeredQuoteManager.removeTrigger(trigger, this.triggeredQuoteListener);
          }
           
          try {           
             this.triggeredQuoteManager.disconnect();
          } catch (ResourceManagerException rme) {
             rme.printStackTrace();
          }

          if (this.displayOutput) {         
             System.out.println("");
             System.out.println("Exiting application...");
             System.out.println("(press Enter to exit)");
             System.in.read();
          }
          System.exit(0);
       }
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
       //
       //connectionSettings.setConnectionTimeout(30000);
       //
       // note:  The above value of 30000 is the same as the default value.  This value can be
       //        changed.  The minimum value is 1000.
       //
       // IMPORTANT: Use of the setConnectionTimeout method affects ALL connections (new and existing)
       //            including existing connections that experience a reconnection.  
       //            This is a *global* setting and affects all connections regardless of
       //            manager type.

       return connectionSettings;
   }
}
