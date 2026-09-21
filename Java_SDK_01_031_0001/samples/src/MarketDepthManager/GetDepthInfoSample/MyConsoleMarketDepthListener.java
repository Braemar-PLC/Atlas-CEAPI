package MarketDepthManager.GetDepthInfoSample;

import org.json.JSONObject;
import org.json.JSONArray;

import com.esignal.jstandard.event.MarketDepthEvent;
import com.esignal.jstandard.event.MarketDepthInfoEvent;
import com.esignal.jstandard.event.MarketDepthListener;
import com.esignal.jstandard.event.NetOrderImbalanceEvent;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with a Level 2
 * request.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class MyConsoleMarketDepthListener implements MarketDepthListener {
   // Holds a reference to the GetDepthInfoSample object so that certain functions may be
   // called from this listener.
   private GetDepthInfoSample consoleSample;
   
   // Counts number of instruments (requests) that returned NO_DATA or resulted in some
   // of type of error that resulted in no records returned.
   private int nothingReturned;
   
   // Members for handling column headings for output to file
   private final String infoHeadings = "SYMBOL,MARKET,CLEAR DOWN TIME,SUPPORTS DEPTH TYPE,NET ORDER IMBALANCE AVAILABLE,ENTITLEMENT REQUIRED,IS ENTITLED,IS DEFAULT";
   private boolean columnsPrinted = false;

   public MyConsoleMarketDepthListener(GetDepthInfoSample getDepthInfoSample) {
      this.consoleSample = getDepthInfoSample;
   }   

   public void onInfo(MarketDepthInfoEvent event) {
	    // Final constants to reduce repetition
	    final String LINE_SEPARATOR = "-------------------------------------------------------------------------------------------------";
	    final String MSG_TYPE_DEPTH_INFO = "DEPTH_INFO_NOTIFICATION";

	    // Check if the event itself is null
	    if (event == null) {
	        handleEmptyResponse(null); 
	        markNoRecordsReturned();
	        consoleSample.setGetDepthInfoComplete(true);
	        return;
	    }

	    // Now safely retrieve JSON string from the event
	    String jsonString = event.getInfo();
	    if (jsonString == null || jsonString.isEmpty()) {
	        handleEmptyResponse(event.getSymbol());
	        markNoRecordsReturned();
	        consoleSample.setGetDepthInfoComplete(true);
	        return;
	    }

	    try {
	        JSONObject jsonObject = new JSONObject(jsonString);

	        // Check msgType
	        String msgType = jsonObject.optString("msgType", "");
	        if (!MSG_TYPE_DEPTH_INFO.equals(msgType)) {
	            // Not the message type we expect; just return
	            consoleSample.setGetDepthInfoComplete(true);
	            return;
	        }

			 if (!jsonObject.has("sources")) {
				System.out.println("No depth information available.");
				return;
			 }
			 
	        // Access the "sources" array
	        JSONArray sources = jsonObject.optJSONArray("sources");
	        if (sources == null) {
	            // No sources; handle gracefully
	            handleEmptyResponse(event.getSymbol());
	            markNoRecordsReturned();
	            consoleSample.setGetDepthInfoComplete(true);
	            return;
	        }

	        // Retrieve symbol
	        String eventSymbol = event.getSymbol();

	        // If displaying to console, print the header
	        if (consoleSample.displayData()) {
	            System.out.println(LINE_SEPARATOR);
	            System.out.printf("SYMBOL: %s%n", eventSymbol);
	            System.out.println(LINE_SEPARATOR);
	        } else {
	        	// Have columns printed?
	        	if (!columnsPrinted) {
	        		System.out.println(infoHeadings);
	        		columnsPrinted = true;
	        	}
	        }

	        // Iterate over sources
	        for (int i = 0; i < sources.length(); i++) {
	            JSONObject source = sources.optJSONObject(i);
	            if (source == null) {
	                continue; // skip invalid/missing source object
	            }

	            String sourceDesc = source.optString("desc", "");
	            String netOrderDesc = source.optString("netorder", "");
	            String cleanupTime = source.optString("cleanup", "N/A");

	            // Output depends on console vs. CSV
	            if (consoleSample.displayData()) {
	                System.out.printf("   %-72sClear Down Time: %s%n%n", sourceDesc, cleanupTime);
	            } 

	            // Retrieve the "types" array safely
	            JSONArray types = source.optJSONArray("types");
	            if (types != null) {
	                for (int j = 0; j < types.length(); j++) {
	                    JSONObject type = types.optJSONObject(j);
	                    if (type == null) {
	                        continue; // skip invalid type object
	                    }

	                    String depthType   = type.optString("desc", "");
	                    String entRequired = type.optString("ent_required", "");
	                    boolean entitled   = type.optBoolean("entitled", false);
	                    boolean isDefault  = type.optBoolean("default", false);

	                    if (consoleSample.displayData()) {
	                        System.out.printf("      Depth Type: %-30s Entitlement: %-7s Entitled: %-6s",
	                                depthType, entRequired, entitled ? "Yes" : "No");
	                        if (isDefault) {
	                            System.out.print(" (default)");
	                        }
	                        System.out.println();
	                    } else {
	                    	// CSV-style
	        	            // "netorder" may or may not be present
	        	            if (source.has("netorder")) {
	        	            	if (netOrderDesc.equalsIgnoreCase(entRequired))
			        	            System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s\n", 
			        	            		eventSymbol, sourceDesc, cleanupTime, depthType, "Yes", entRequired, 
			        	            		entitled ? "Yes" : "No", isDefault ? "default" : "");
	        	            	else
			        	            System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s\n", 
			        	            		eventSymbol, sourceDesc, cleanupTime, depthType, "No", entRequired, 
			        	            		entitled ? "Yes" : "No", isDefault ? "default" : "");
	        	            }
	        	            else {
		        	            System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s\n", 
		        	            		eventSymbol, sourceDesc, cleanupTime, depthType, "No", entRequired, 
		        	            		entitled ? "Yes" : "No", isDefault ? "default" : "");
	    	                }
	        	        }
	                }
	            }

	            // "netorder" may or may not be present
	            if (source.has("netorder")) {
	                if (consoleSample.displayData()) {
	                    System.out.println("\n      Net Order Imbalance: " + netOrderDesc);
	                } 
	            }

	            if (consoleSample.displayData()) {
		            // Separate entries
		            if (i < sources.length() - 1) {
		                System.out.println(); 
		            } else {
		                System.out.println(LINE_SEPARATOR + "\n");
		            }
	            }
	        }

	    } catch (Exception e) {
	        // Catch unexpected errors safely
	        e.printStackTrace();
	    }

	    // Mark completion
	    consoleSample.setGetDepthInfoComplete(true);
	}

	/**
	 * Prints a standardized message if the JSON response is empty
	 * or the symbol might be invalid.
	 */
	private void handleEmptyResponse(String symbol) {
	    System.out.println("ERROR:  The response was empty.");
	    if (symbol != null) {
	        System.out.println("        This can occur for invalid symbols: " + symbol);
	    } else {
	        System.out.println("        The symbol may have been invalid or missing.");
	    }
	    System.out.println("        Please check the symbol and try again.");
	}

	/**
	 * Increments the counter for returned-empty events and updates `consoleSample`.
	 */
	private void markNoRecordsReturned() {
	    ++this.nothingReturned;
	    this.consoleSample.setNumberRequestsWithNoRecords(nothingReturned);
	}  
	
   public void onError(MarketDepthInfoEvent marketDepthEvent) {
      // This sample displays errors regardless of the display setting.
      if (marketDepthEvent.getStatus() == DbcCodes.DBCAPI_ERROR.DEPTH_SYMBOL_NOT_FOUND) {
         System.out.println("Could not process with the error Status: " + marketDepthEvent.getStatus().name());
         System.out.println("Invalid Symbol entered: " + marketDepthEvent.getSymbol());
         System.out.println("Please check the symbol and try again.");
      }
      ++this.nothingReturned;
      this.consoleSample.setNumberRequestsWithNoRecords(nothingReturned);      
   }

   // The following events apply to Market Depth requests, but not to Market Depth Info requests.
   // Market Depth includes: Order Book, Book by Price, Book by Level (aka Aggregate Depth), Book by Market Maker
   // and Book by Market Maker Ext.
   // Depth Info describes the type of depth available for a given instrument, but does not return depth data.

   // Required to implement this class, but not handled by this sample
   public void onAdded(SymbolEvent event) {
   }

   // Required to implement this class, but not handled by this sample
   public void onDeleted(SymbolEvent event) {
   }

   // Required to implement this class, but not handled by this sample
   public void onError(StatusEvent event) {
   }

   // Required to implement this class, but not handled by this sample
   public void onError(MarketDepthEvent event) {
   }

   // Required to implement this class, but not handled by this sample
   public void onUpdate(MarketDepthEvent event) {
   }

   // Required to implement this class, but not handled by this sample
   public void onUpdate(NetOrderImbalanceEvent netOrderImbalanceEvent) {
   }
   
   // Required to implement this class, but not handled by this sample
   public void onError(NetOrderImbalanceEvent netOrderImbalanceEvent) {
   }

}
