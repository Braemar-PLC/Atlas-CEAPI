package TriggeredQuoteManager.SnapOnTriggerSample;

import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;

public class MyConsoleConnectionListener implements ConnectionListener {

    private SnapOnTriggerSample consoleSample;
    private boolean connectionEstablished;

    public MyConsoleConnectionListener(SnapOnTriggerSample consoleSample) {
       this.consoleSample         = consoleSample;
       this.connectionEstablished = false;
    }

   public void onConnected(ConnectionEvent event) {
	  if (consoleSample.displayData()) {
	     if (!this.connectionEstablished) {
	        System.out.println("Connected to the Level 1 Service.\n");
	        System.out.println("Server Connection: " + event.getConnectedHost());
	        System.out.println("Your IP Address: " + event.getConnectionLocalIp());
	     }
	  }
	  this.connectionEstablished = true;
      
      // Adds all defined triggers to the Active Trigger list.
      // This will send a request for each trigger to the Snap-on-Trigger
      // service.  The SampleTriggeredQuoteEventListener suppresses the 
      // data returned ** if ** the request is a "list" request.
      //
      // This behavior is for demonstration purposes only.
      consoleSample.getData();

      // Check if the request is a "list" request.
      if (consoleSample.isListRequest()) {
         consoleSample.listTriggers();
         
         // Exit the sample.
         try {
            consoleSample.exitSample(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public void onConnecting(ConnectionEvent event) {
      // This sample doesn't implement this event
   }

   public void onDisconnected(ConnectionEvent event) {
	  if (!connectionEstablished) 
		  return;
      connectionEstablished = false;      
      try {
         consoleSample.exitSample(true);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void onDisconnecting(ConnectionEvent event) {
      // This sample doesn't implement this event
   }

   public void onError(ConnectionEvent event) {
      System.out.println(event.getStatusString());
   }

}
