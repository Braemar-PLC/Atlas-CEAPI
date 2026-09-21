package TickHistoryManager.TickHistorySample;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.esignal.jstandard.beans.flex.FlexData;
import com.esignal.jstandard.beans.flex.FlexData.DATATYPE;
import com.esignal.jstandard.beans.flex.FlexData.TimeMicro;
import com.esignal.jstandard.beans.flex.FlexData.TimeNano;
import com.esignal.jstandard.beans.flex.FlexDataField;
import com.esignal.jstandard.beans.history.HistoricalBar;
import com.esignal.jstandard.beans.tick.HistoricalTickRecord;
import com.esignal.jstandard.beans.tick.QuoteTickRecord;
import com.esignal.jstandard.beans.tick.TradeTickRecord;
import com.esignal.jstandard.beans.tick.HistoricalTickRecordCID;
import com.esignal.jstandard.beans.tick.QuoteTickRecordCID;
import com.esignal.jstandard.beans.tick.TradeTickRecordCID;
import com.esignal.jstandard.event.DictionaryEvent;
import com.esignal.jstandard.event.HistoricalTickEvent;
import com.esignal.jstandard.event.HistoricalTickFlexEvent;
import com.esignal.jstandard.event.HistoricalTickListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.exception.InvalidDataConversionException;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with a history
 * request.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class MyConsoleTickHistoryListener implements HistoricalTickListener {
   // Holds a reference to the TickHistorySample object so that certain functions may be
   // called from this listener.
   private TickHistorySample consoleSample;
   
   // Holds the current count of returned TIC records or BAR records.
   private int ticBarCount;
   
   // Counts number of instruments (requests) that returned NO_DATA or resulted in some
   // of type of error that resulted in no records returned.
   private int nothingReturned;
   
   // Members for handling column headings for output to file
   private boolean columnsPrepared;
   private int     columnIndex = 0;
   private boolean columnsPrinted;
   private String[] columnHeadings = new String[50];
   private String compositeColumnHeading = "";
 
   private static final int EXCHANGE_THRESHOLD = 191;
   // The following members are used to load and parse the "qualifiers.tab" file.
   // We need to load this file into an XML parser to retrieve the data associated with any qualifier information we receive.
   // This information tells us more details about the data.
   Document gDoc;
   NodeList gQualifierList;
   NodeList gSecurityStatusList;
   
   short[] gQualifiers;
   String  gExchange = "";

   public MyConsoleTickHistoryListener(TickHistorySample consoleSample) {
      this.consoleSample   = consoleSample;
      this.ticBarCount     = 0;
      this.nothingReturned = 0;
      
      //if (this.consoleSample.displayData())
      //   System.out.print("Loading qualifiers information...");
      
      try {
         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
         gDoc = dBuilder.parse(new URL("http://fs2.esignal.com/qualifiers.tab").openStream());
         gDoc.getDocumentElement().normalize();
         gQualifierList = gDoc.getElementsByTagName("q");
         gSecurityStatusList = gDoc.getElementsByTagName("securityStatus");         
      }
      catch (Exception e) {
         System.out.println("could not load qualifiers information.");
         System.out.println(e + "\n");
      }

      //if (this.consoleSample.displayData())     
      //   System.out.println("loaded.\n");
   }
   
   /* ---------------------------------------------------------------------------------------------
    * COMPLETION HANDLERS
    * ---------------------------------------------------------------------------------------------
    */
   @Override
   /**
    * Handles the completion of a `SymbolEvent`.
    *
    * <p>This method is triggered when a symbol-specific request has been completed. 
    * It delegates the processing to the `handleCompletion` method, which finalizes
    * the state and updates relevant counters or logs.</p>
    *
    * @param event the `SymbolEvent` signaling the completion of a symbol-specific request.
    */
   public void onComplete(SymbolEvent event) {
      handleCompletion();
   }

   @Override
   /**
    * Handles the completion of a `StatusEvent`.
    *
    * <p>This method is triggered when a status-related request has been completed. 
    * It delegates the processing to the `handleCompletion` method, which finalizes
    * the state and updates relevant counters or logs.</p>
    *
    * @param event the `StatusEvent` signaling the completion of a status-related request.
    */
   public void onComplete(StatusEvent event) {
      handleCompletion();
   }

   /**
    * Finalizes the processing of completed events.
    *
    * <p>This private method is invoked by `onComplete` methods for both `SymbolEvent` 
    * and `StatusEvent`. It determines whether the event is a dictionary request and 
    * performs the appropriate finalization tasks, such as updating counters or 
    * triggering cleanup actions.</p>
    */
   private void handleCompletion() {
      if (consoleSample.isDictionaryRequest())
          consoleSample.setDictionaryComplete();
       else {
          if (consoleSample.displayData() & ticBarCount > 0)
             System.out.println("Total Records:  " + ticBarCount);
          ticBarCount = 0;
       }
       consoleSample.setHistComplete();
   }
   
   /* ---------------------------------------------------------------------------------------------
    * ERROR HANDLERS
    * ---------------------------------------------------------------------------------------------
    */
   @Override
   /**
    * Handles errors that occur during the processing of a `StatusEvent`.
    *
    * <p>This method is invoked when an error occurs during the handling of a 
    * status-related request. It logs the error details to the console, regardless 
    * of the display setting.</p>
    *
    * @param event the `StatusEvent` containing details about the error.
    */
   public void onError(StatusEvent event) {
      // This sample displays errors regardless of the display setting.
      System.out.println("onError during processing, returning status: " + event.getStatus());
   }
   
   @Override
   /**
    * Handles errors that occur during the processing of a `SymbolEvent`.
    *
    * <p>This method is invoked when an error occurs during the handling of a 
    * symbol-specific request. It logs detailed information about the error, 
    * including the symbol, turnaround, and status details, to the console. 
    * If applicable, it increments the count of requests with no records.</p>
    *
    * @param event the `SymbolEvent` containing details about the error.
    */
   public void onError(SymbolEvent event) {
      // This sample displays errors regardless of the display setting.
     if (consoleSample.displayData()) {
         System.out.println();
         System.out.println("ERROR:");
         System.out.println("       Symbol: " + event.getSymbol());
         System.out.println("   Turnaround: " + event.getTurnAround());
         System.out.println("       Source: " + event.getSource());
         System.out.println("  Status Code: " + event.getStatusCode());
         System.out.println("  Status Type: " + event.getStatusType());
         System.out.println("Status String: " + event.getStatusString() + "\n");
         
         System.out.println("DETAILS:");
         if (event.getStatus() == DbcCodes.TICSRV_RESPONSE_ERROR.SERVERBUSY || event.getStatus() == DbcCodes.TICSRV_RESPONSE_ERROR.NO_DATA) {
            System.out.println("   Could not process with the error status: " + event.getStatus().name());
            System.out.println("   The instrument entered ( " + event.getSymbol() + " ) may be valid, but there may be no data for the given date range.");
            System.out.println("   If the request included \"Flexible Data\" column options, one or more columns may not be applicable to the instrument.");
         }
   
         if (event.getStatus() == DbcCodes.TICSRV_RESPONSE_ERROR.NOT_ENTITLED) {
            System.out.println("   Could not process with the error Status: " + event.getStatus().name());
            System.out.println("   Your account is not enitled for symbol: " + event.getSymbol());
         }
   
         if (event.getStatus() == DbcCodes.TICSRV_RESPONSE_ERROR.REQINCORRECT) {
            System.out.println("   Incorrect Request:" + event.getStatus().name());
         }
         System.out.println();
     }
     System.out.println(event.getSymbol() + ",NO DATA");
     ++this.nothingReturned;
     this.consoleSample.setNumberRequestsWithNoRecords(nothingReturned);
   }
   
   /* ---------------------------------------------------------------------------------------------
    * REQUEST HANDLERS
    * ---------------------------------------------------------------------------------------------
    */
   @Override
   /**
    * Invoked when a symbol-specific request is initiated.
    *
    * <p>This method is called to indicate that a `SymbolEvent` request has been 
    * initiated. Currently, this implementation is a placeholder and does not 
    * perform any specific actions.</p>
    *
    * @param event the `SymbolEvent` representing the initiated request.
    */
   public void onRequested(SymbolEvent event) {
      //System.out.println("REQUESTED," + event.getSymbol());
   }
   
   @Override
   /**
    * Invoked when a status-related request is initiated.
    *
    * <p>This method is called to indicate that a `StatusEvent` request has been 
    * initiated. Currently, this implementation is a placeholder and does not 
    * perform any specific actions.</p>
    *
    * @param event the `StatusEvent` representing the initiated request.
    */
   public void onRequested(StatusEvent event) {
      //System.out.println("REQUESTED," + event.getStatusString());
   }
   
   /* ---------------------------------------------------------------------------------------------
    * RESPONSE HANDLERS
    * ---------------------------------------------------------------------------------------------
    */
   @Override
   /**
    * Processes the response for an intraday historical data request.
    *
    * <p>This method handles responses containing bar data for intraday history requests. 
    * It formats and outputs the received data either to the console or in CSV format, 
    * depending on the display settings.</p>
    *
    * @param historicalTickEvent the event containing the response data for intraday history.
    */
   public void onIntradayResponse(HistoricalTickEvent<HistoricalBar> historicalTickEvent) {
      String symbol = historicalTickEvent.getSymbol();

      List<HistoricalBar> tickBarResponse = historicalTickEvent.getHistoricalTickList();
      
      ticBarCount += tickBarResponse.size();

      for (HistoricalBar bar : tickBarResponse) {
         SimpleDateFormat myFormat = new SimpleDateFormat("yyyyMMddHHmmss");
         String timestamp = myFormat.format(bar.getDate());
         if (consoleSample.displayData()) {
            System.out.printf("SYMBOL: %-15sTIMESTAMP: %-25sBARLENGTH: %-10sOPEN: %-10sHIGH: %-10sLOW: %-10sCLOSE: %-10sVOLUME: %.5f", symbol, timestamp, bar.getBarLength(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
            System.out.printf("\n");
         }
         else {
          System.out.printf("%s,%s,%s,%s,%s,%s,%s,%.5f", symbol, timestamp, bar.getBarLength(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
         }
      }
   }
   
   @Override
   @SuppressWarnings("rawtypes")
   /**
    * Processes the response for a tick record request.
    *
    * <p>This method handles responses containing tick data for quotes and trades. 
    * It processes bid/ask prices and sizes, as well as trade details, and formats 
    * the output based on the display settings.</p>
    *
    * @param historicalTickEvent the event containing the response data for tick records.
    */
   public void onTickRecordResponse(HistoricalTickEvent<HistoricalTickRecord> historicalTickEvent) {
	   SimpleDateFormat myFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	   
       String symbol = historicalTickEvent.getSymbol();
       List<HistoricalTickRecord> tickResponse = historicalTickEvent.getHistoricalTickList();
       ticBarCount += tickResponse.size();

       for (HistoricalTickRecord<?> tick : tickResponse) {
		   String timestamp = myFormat.format(tick.getTime());
           String tickType = "";
           double tradePrice = 0.0, tradeSize = 0.0, tradeTotalVolume = 0.0;
           String tradeTime = "", tradeExchange = "";
           double askPrice = 0.0, askSize = 0.0, bidPrice = 0.0, bidSize = 0.0;
           String askExchange = "";
           String bidExchange = "";
           
           // Flags, qualifiers and VWAP are not available to regular quotes and trades.
           // Use the request option, CID=T, to receive flags, qualifiers and VWAP data.
           // note: Qualifiers can be used to determine if a trade price represents a type of close value,
           //       such as Official Close Price.
           String flagsMsg = "N/A", tradeQuals = "N/A", bidQuals = "N/A", askQuals = "N/A", closeType = "N/A";
           double tradeVWAP = 0.0;

           // Process each tick based on its type
           if (tick.getType().equals(HistoricalTickRecord.TYPE.QUOTE)) {
               QuoteTickRecord quote = (QuoteTickRecord) tick.getValue();
               tickType = "QUOTE";

               askExchange = safeString(quote.getAskExchange());
               askPrice = safeDouble(quote.getAskPrice());
               askSize = safeDouble(quote.getAskSizeAsDouble());
               askQuals = "N/A"; // No qualifiers for regular quotes

               bidExchange = safeString(quote.getBidExchange());
               bidPrice = safeDouble(quote.getBidPrice());
               bidSize = safeDouble(quote.getBidSizeAsDouble());
               bidQuals = "N/A"; // No qualifiers for regular quotes

           } else if (tick.getType().equals(HistoricalTickRecord.TYPE.TRADE)) {
               TradeTickRecord trade = (TradeTickRecord) tick.getValue();
               tickType = "TRADE";

               tradeTime = safeString(tick.getTime().toString());
               tradeExchange = safeString(trade.getExchange());
               tradePrice = safeDouble(trade.getPrice());
               tradeSize = safeDouble(trade.getVolumeAsDouble());
               tradeTotalVolume = safeDouble(trade.getTotalVolumeAsDouble());
               tradeQuals = "N/A"; // No qualifiers for regular trades
           }

           // Output the record
           outputTickRecord(tickType,symbol,timestamp,flagsMsg,
   			         tradeTime,tradeExchange,tradePrice,tradeSize,tradeQuals,tradeTotalVolume,tradeVWAP,
   			         bidExchange,bidPrice,bidSize,bidQuals,
   			         askExchange,askPrice,askSize,askQuals,
   			         closeType);
       }
   }
   
   @Override
   @SuppressWarnings("rawtypes")
   /**
    * Processes the response for a tick record request with CID (Correction, Insertion, Deletion) data.
    *
    * <p>This method handles responses containing tick data with CID information. 
    * It processes bid/ask prices, sizes, qualifiers, and flags, and formats the 
    * output based on the display settings.</p>
    *
    * @param historicalTickEvent the event containing the response data for tick records with CID.
    */
   public void onTickRecordCIDResponse(HistoricalTickEvent<HistoricalTickRecordCID> historicalTickEvent) {
      SimpleDateFormat myFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

      String symbol = historicalTickEvent.getSymbol();

      List<HistoricalTickRecordCID> tickCIDResponse = historicalTickEvent.getHistoricalTickList();
      ticBarCount += tickCIDResponse.size();  
      
      for (HistoricalTickRecordCID<?> tick : tickCIDResponse) {
		    String timestamp = myFormat.format(tick.getTime());
		    String flagsMsg = processFlags(tick.getFlags());

		    String tickType = "", tickTime = "";		    
	        double tradePrice = 0.0, tradeSize = 0.0, tradeTotalVolume = 0.0, tradeVWAP = 0.0;
	        String tradeTime = "", tradeExchange = "", tradeQuals = "", closeType = "";
	        double askPrice = 0.0, askSize = 0.0, bidPrice = 0.0, bidSize = 0.0;
	        String askExchange = "", askQuals = "";
	        String bidExchange = "", bidQuals = "";
	        short[] quals = null;
	
		    if (tick.getType().equals(HistoricalTickRecordCID.TYPE.QUOTE)) {
		        QuoteTickRecordCID quote = (QuoteTickRecordCID) tick.getValue();
		        tickType = "QUOTE";

		        askExchange = safeString(quote.getAskExchange());
		        askPrice = safeDouble(quote.getAskPrice());
		        askSize = safeDouble(quote.getAskSizeAsDouble());
		        askQuals = sanitizeQualifiers(quote.getAskQualifiers());
		        bidExchange = safeString(quote.getBidExchange());
		        bidPrice = safeDouble(quote.getBidPrice());		        
		        bidSize = safeDouble(quote.getBidSizeAsDouble());
		        bidQuals = sanitizeQualifiers(quote.getBidQualifiers());
	
		    } else if (tick.getType().equals(HistoricalTickRecordCID.TYPE.TRADE)) {
		        TradeTickRecordCID trade = (TradeTickRecordCID) tick.getValue();
		        tickType = "TRADE";
		        
		        tradeTime = myFormat.format(tick.getExchangeTime());
		        tradeExchange = safeString(trade.getExchange());
		        tradePrice = safeDouble(trade.getPrice());
		        tradeSize = safeDouble(trade.getVolumeAsDouble());
		        tradeQuals = sanitizeQualifiers(trade.getQualifiers());
		        tradeVWAP = safeDouble(trade.getVolumeWeightedAveragePrice());
		        tradeTotalVolume = safeDouble(trade.getTotalVolumeAsDouble());
		        
		        quals = trade.getQualifiers();
		        
		        if (quals != null) {
			        for (short qualValue : quals) {
			        	switch (qualValue) {
			        	   case 47:
			        		   closeType = "Closing";
			        		   break;
			        	   case 69:
			        		   closeType = "Final Settle";
			        		   break;
			        	   case 88:
			        		   closeType = "Official Close";
			        		   break;
			        	   case 89:
			        		   closeType = "Official Close Price";
			        		   break;
			        	}
			        }
		        }
		    }
		    
		    if (passesQualifierFilter(quals)) {
		    	outputTickRecord(tickType,symbol,timestamp,flagsMsg,
		    			         tradeTime,tradeExchange,tradePrice,tradeSize,tradeQuals,tradeTotalVolume,tradeVWAP,
		    			         bidExchange,bidPrice,bidSize,bidQuals,
		    			         askExchange,askPrice,askSize,askQuals,
		    			         closeType);
		    }
      }
   }
   
   @Override
   /**
    * Processes the response for a flexible tick record request.
    *
    * <p>This method handles responses containing flexible data fields. It processes 
    * both single-value and composite flexible fields and formats the output for 
    * console display or CSV export.</p>
    *
    * @param historicalFlexEvent the event containing the response data for flexible tick records.
    */
   public void onTickRecordFlexResponse(HistoricalTickFlexEvent historicalFlexEvent) {
      Iterator<Iterator<FlexDataField>> recordIterator = historicalFlexEvent.iterator();
      
      String output;         
      String symbol = historicalFlexEvent.getSymbol();
      
      // Display the schema for the current response; toggle from comment to activate.
//      if (consoleSample.displayData())
//        System.out.println("RESPONSE SCHEMA:\n" + historicalFlexEvent.getSchema() + "\n");      
      
      try {
         while (recordIterator.hasNext()) {
            output = "";
            // getDisplayFormat returns true if the output is displayed within the console; false otherwise.
            if (this.consoleSample.displayData())
               output += "SYMBOL:  ";
            output += symbol;            
         
            // Get the next "Flexible Data Field" from the package
            // A FlexibleDataField represents a single data field or a composite data field that is composed of one or more FlexDataFields. 
            Iterator<FlexDataField> fieldIterator = recordIterator.next();            
            
            while (fieldIterator.hasNext()) {
               FlexDataField dataField = fieldIterator.next();               
               
               // FORMAT FIELD NAMES FOR OUTPUT
               // -----------------------------
               // If the output is intended for the console we will display the field name in addition
               // to the field value.
               if (this.consoleSample.displayData()) {
                  // The "TIME" field represents the beacon time; the time the server received the update.
                  // Beacon time is provided as a whole second time field.
                  // This sample displays the beacon time as a UTC-formatted time, therefore it adds UTC to the display description. 
                  if (dataField.getName().equalsIgnoreCase("TIME"))
                    output += "     UTC " + dataField.getName().toUpperCase().replace("_", " ") + ":  ";
                  else
                    output += "     " + dataField.getName().toUpperCase().replace("_", " ") + ":  ";
               } else {
                  output += ","; 
               }
                                             
               // DETERMINE IF THE FIELD IS A COMPOSITE VALUE FIELD (contains more than one value/field) 
               // OR A SINGLE VALUE FIELD
               if (dataField.getType() == DATATYPE.COMPOSITE) {
                  output += processCompositeFlexibleDataField(dataField);
               }
               else {
                  // Verify the field is not null
                  if (!dataField.isNull()) {
                     String tmp = processFlexibleDataField(dataField);
                     if (this.consoleSample.displayData())
                      output += String.format("%-13s", tmp);
                     else
                      output += tmp + ",";
                  }
                  else {
                    if (this.consoleSample.displayData())
                       output += String.format("%-13s","----");
                    else
                       output += ",";
                  }
               }
            }
            
            System.out.println(output);
            ticBarCount++;
         }
      } catch (Exception e) {
         System.out.println("ERROR:  Issue iterating through the Flexible Field Data response.");
         System.out.println("--> " + e.getLocalizedMessage());
      }
   }
   
   @Override
   /**
    * Processes the response for an intraday flexible data request.
    *
    * <p>This method handles responses containing flexible data fields for intraday 
    * bar requests. It organizes and outputs the data in either console or CSV format, 
    * depending on the display settings.</p>
    *
    * @param historicalFlexEvent the event containing the response data for intraday flexible data.
    */
   public void onIntradayFlexResponse(HistoricalTickFlexEvent historicalFlexEvent) {
      Iterator<Iterator<FlexDataField>> recordIterator = historicalFlexEvent.iterator();
       
      String output;     
      columnIndex = 0;
      
      if (consoleSample.displayData()) {
         // Displays the current response's schema.  Commented out to minimize visual clutter.
         //System.out.println("RESPONSE SCHEMA:\n" + historicalFlexEvent.getSchema() + "\n");      
      }

      try {
         while (recordIterator.hasNext()) {
            output = "";
             // getDisplayFormat returns true if the output is displayed within the console; false otherwise.
            if (this.consoleSample.displayData())
               output += "SYMBOL:  " + historicalFlexEvent.getSymbol() + "  \t  ";
            else {
               if (!columnsPrepared) {
                  columnHeadings[columnIndex] = "INSTRUMENT";
               }
               output += historicalFlexEvent.getSymbol() + ",";  
            }
            
            // Get the next "Flexible Data Field" from the package
            // A FlexibleDataField represents a single data field or a composite data field that is composed of one or more FlexDataFields. 
            Iterator<FlexDataField> fieldIterator = recordIterator.next();
               
            while (fieldIterator.hasNext()) {
               FlexDataField dataField = fieldIterator.next();               
               
               // FORMAT FIELD NAMES FOR OUTPUT
               // -----------------------------
               // If the output is intended for the console we will display the field name in addition
               // to the field value.
               if (this.consoleSample.displayData()) {            
                  output += "  \t" + dataField.getName().toUpperCase().replace("_", " ") + ":  ";
               } else {
                  if (!columnsPrepared) {
                     columnHeadings[++columnIndex] = dataField.getName().toUpperCase().replace("_", " ");
                  }
               }
               
               // DETERMINE IF THE FIELD IS A COMPOSITE VALUE FIELD (contains more than one value/field) 
               // OR A SINGLE VALUE FIELD
               if (dataField.getType() == DATATYPE.COMPOSITE) {
                  if (!this.consoleSample.displayData()) {
                     compositeColumnHeading = columnHeadings[columnIndex]; 
                     columnHeadings[columnIndex] = "SKIP";
                  }
                  output += processCompositeFlexibleDataField(dataField);
               }
               else {
                  if (this.consoleSample.displayData())
                     output += processFlexibleDataField(dataField) + "\t";
                  else
                     output += processFlexibleDataField(dataField) + ",";
               }
            }
            if (this.consoleSample.displayData()) 
               System.out.println(output);
            else {
               if (!columnsPrepared)
                  columnsPrepared = true;
               
               if (!columnsPrinted) {
                  for (int i = 0; i <= columnIndex; i++) {
                     if (!columnHeadings[i].equalsIgnoreCase("SKIP"))
                        System.out.print(columnHeadings[i] + ",");
                  }
                  columnsPrinted = true;
                  System.out.print("\n");
               }              
               System.out.println(output);
            }
            ticBarCount++;
         }
      } catch (Exception e) {
         System.out.println("ERROR:  Issue iterating through the Flexible Field Data response.");
         System.out.println("--> " + e.getLocalizedMessage());
      }
   }
   
   @Override
   /**
    * Processes the response for a dictionary request.
    *
    * <p>This method handles responses containing dictionary data for flexible data fields. 
    * It outputs the dictionary information, typically for debugging or validation purposes.</p>
    *
    * @param dictionaryEvent the event containing the response data for the dictionary request.
    */
   public void onDictionaryResponse(DictionaryEvent dictionaryEvent) {
      System.out.println(dictionaryEvent.getDictionary());
      System.out.println("");
   }
   
   /* ---------------------------------------------------------------------------------------------
    * HELPER METHODS - FLAGS
    * ---------------------------------------------------------------------------------------------
    */
   /**
    * Processes a flags value and returns a human-readable description.
    *
    * <p>This method interprets the flags associated with a tick record, identifying 
    * specific conditions or states (e.g., CID operations, general flags). The result 
    * is formatted as a string describing all applicable flags.</p>
    *
    * @param flags the flags value to process.
    * @return a string representation of the processed flags.
    */
   public String processFlags(long flags) {
       StringBuilder flagMsg = new StringBuilder();
   
       // Check and append CID-specific flags
       if (checkAndAppendFlag(flagMsg, flags, 0x00040000, "CID [", true)) {
           appendCIDFlags(flagMsg, flags);
           flagMsg.append("]");
       }
   
       // General flags
       appendGeneralFlag(flagMsg, flags, 0x00000001, "eISTRADE");
       appendGeneralFlag(flagMsg, flags, 0x00000002, "eFORMT");       
       appendGeneralFlag(flagMsg, flags, 0x01000000, "eBIDUNPRICED");
       appendGeneralFlag(flagMsg, flags, 0x02000000, "eASKUNPRICED");
       
       if ((flags & 0x01000000) != 0 && (flags & 0x00000001) != 0) {
           appendSeparator(flagMsg);
           flagMsg.append("eTRADEUNPRICED");
       }
       if ((flags & 0x02000000) != 0 && (flags & 0x00000001) != 0) {
           appendSeparator(flagMsg);
           flagMsg.append("eTRADEUNSIZED");
       }       
       
       appendGeneralFlag(flagMsg, flags, 0x00000100, "eLOCKED");
       appendGeneralFlag(flagMsg, flags, 0x00000200, "eCROSSED");
       appendGeneralFlag(flagMsg, flags, 0x00000400, "eINSIDE");
       appendGeneralFlag(flagMsg, flags, 0x00000800, "eINDICATIVE");
       appendGeneralFlag(flagMsg, flags, 0x00001000, "eBIDWANTED");
       appendGeneralFlag(flagMsg, flags, 0x00002000, "eASKWANTED");
       appendGeneralFlag(flagMsg, flags, 0x00000008, "eISBBO");
       appendGeneralFlag(flagMsg, flags, 0x04000000, "eISSPECIAL");
       
       // Default to "----" if no matches
       if (flagMsg.length() == 0) {
           flagMsg.append("----");
       }
   
       return flagMsg.toString();
   }
   
   /**
    * Checks and appends a specific flag to the flag message.
    *
    * <p>This method evaluates whether a given flag is set in the `flags` value. 
    * If the flag is set, the corresponding message is appended to the `flagMsg` 
    * with an optional separator.</p>
    *
    * @param flagMsg the `StringBuilder` to append the flag description to.
    * @param flags the flags value to evaluate.
    * @param mask the mask representing the specific flag to check.
    * @param message the message to append if the flag is set.
    * @param addSeparator whether to append a separator before the message.
    * @return `true` if the flag is set and the message was appended; `false` otherwise.
    */
   private boolean checkAndAppendFlag(StringBuilder flagMsg, long flags, long mask, String message, boolean addSeparator) {
       if ((flags & mask) != 0) {
           if (addSeparator) appendSeparator(flagMsg);
           flagMsg.append(message);
           return true;
       }
       return false;
   }
   
   /**
    * Appends CID-specific flags to the flag message.
    *
    * <p>This method evaluates the CID-related flags (e.g., Correction, Insertion, Deletion) 
    * and appends their descriptions to the provided `flagMsg`.</p>
    *
    * @param flagMsg the `StringBuilder` to append the CID flag descriptions to.
    * @param flags the flags value containing CID-related information.
    */
   private void appendCIDFlags(StringBuilder flagMsg, long flags) {
       checkAndAppendFlag(flagMsg, flags, 0x00004000, "I", false);
       checkAndAppendFlag(flagMsg, flags, 0x00008000, "C", false);
       checkAndAppendFlag(flagMsg, flags, 0x00010000, "D", false);
       checkAndAppendFlag(flagMsg, flags, 0x00020000, "M", false);
   }
   
   /**
    * Appends a general flag to the flag message.
    *
    * <p>This method evaluates whether a given general flag is set in the `flags` value 
    * and appends its description to the provided `flagMsg`.</p>
    *
    * @param flagMsg the `StringBuilder` to append the flag description to.
    * @param flags the flags value to evaluate.
    * @param mask the mask representing the specific flag to check.
    * @param message the message to append if the flag is set.
    */
   private void appendGeneralFlag(StringBuilder flagMsg, long flags, long mask, String message) {
       checkAndAppendFlag(flagMsg, flags, mask, message, true);
   }
   
   /**
    * Appends a separator to the flag message if necessary.
    *
    * <p>This method appends a separator (e.g., `" | "`) to the `flagMsg` 
    * if it already contains content, ensuring proper formatting for multiple flags.</p>
    *
    * @param flagMsg the `StringBuilder` to append the separator to.
    */
   private void appendSeparator(StringBuilder flagMsg) {
       if (flagMsg.length() > 0) {
           flagMsg.append(" | ");
       }
   }
   
   /* ---------------------------------------------------------------------------------------------
    * HELPER METHODS - QUALIFIERS
    * ---------------------------------------------------------------------------------------------
    */
   /**
    * Retrieves human-readable data for a given qualifier ID and exchange.
    *
    * <p>This method searches the loaded qualifiers for a match with the specified 
    * ID and exchange. If found, it returns a sanitized description of the qualifier; 
    * otherwise, it returns a default message indicating no data is available.</p>
    *
    * @param id the qualifier ID to search for.
    * @param exg the exchange associated with the qualifier (required for IDs above 191).
    * @return a string containing the human-readable qualifier description or a default message.
    */
   public String getQualifierData(short id, String exg) {
       if (gQualifierList == null || gQualifierList.getLength() == 0) {
           return "No qualifier data";
       }
   
       for (int i = 0; i < gQualifierList.getLength(); i++) {
           Node node = gQualifierList.item(i);
           if (node.getNodeType() == Node.ELEMENT_NODE) {
               Element element = (Element) node;
   
               // Qualifier Values above 191 require an exchange value as well
               // note: Some qualifier descriptions include commas. This sample removes commas
               // to facilitate the output as a csv file for importing into applications like Excel.
               if (isMatchingQualifier(element, id, exg)) {
                   return sanitizeQualifierDescription(element.getAttribute("lname"));
               }
           }
       }
       return "No qualifier data";
   }
   
   /**
    * Checks whether a given XML element matches the specified qualifier ID and exchange.
    *
    * <p>This method evaluates whether the `id` and `exg` parameters match the 
    * attributes of the provided XML `Element`. It accounts for special handling of 
    * IDs greater than 191, which require an exchange match.</p>
    *
    * @param element the XML `Element` representing a qualifier entry.
    * @param id the qualifier ID to match.
    * @param exg the exchange to match (required for IDs above 191).
    * @return `true` if the element matches the ID and exchange; `false` otherwise.
    */
   private boolean isMatchingQualifier(Element element, short id, String exg) {
       String elementId = element.getAttribute("id");
       if (!elementId.equals(String.valueOf(id))) {
           return false;
       }
       if (id > EXCHANGE_THRESHOLD) {
           String elementExchange = element.getAttribute("exch");
           return elementExchange.contains(exg);
       }
       return true;
   }
   
   /**
    * Sanitizes a qualifier description for CSV compatibility.
    *
    * <p>This method removes commas from the given description to ensure 
    * compatibility with CSV formatting, making it suitable for exporting.</p>
    *
    * @param description the raw description to sanitize.
    * @return the sanitized description.
    */
   private String sanitizeQualifierDescription(String description) {
       return description.replace(",", "");
   }
 
   /**
    * Sanitizes an array of qualifier IDs for CSV compatibility.
    *
    * <p>This method replaces commas with pipes for a given set of qualifiers to  
    * ensure compatibility with CSV formatting, making it suitable for exporting.</p>
    *
    * @param qualifiers the set of qualifier IDs to sanitize.
    * @return the sanitized qualifier IDs as a <code>String</code>.
    */
   private String sanitizeQualifiers(short[] qualifiers) {
	    if (qualifiers == null) return "";
	    return Arrays.toString(qualifiers).replace(",", "|");
	}
   
   /**
    * Determines whether a tick record passes the qualifier filter.
    *
    * <p>This method checks if the given qualifiers match any of the qualifier filters 
    * defined in `consoleSample`. If no filters are defined, it allows all records to pass.</p>
    *
    * @param qualifiers the array of qualifier values to check.
    * @return `true` if the record passes the filter; `false` otherwise.
    */
   private boolean passesQualifierFilter(short[] qualifiers) {
       // Get the qualifier filters from consoleSample
       List<Integer> qualifierFilters = consoleSample.getQualifierFilters();

       // If no filters are defined, allow all records
       if (qualifierFilters.isEmpty()) {
           return true;
       }

       // Check if any qualifier matches the filters
       for (short qualifier : qualifiers) {
           if (qualifierFilters.contains((int) qualifier)) {
               return true;
           }
       }

       // No matching qualifiers found
       return false;
   }

   /* ---------------------------------------------------------------------------------------------
    * HELPER METHODS - DATA PROCESSING
    * ---------------------------------------------------------------------------------------------
    */
   /**
    * Safely retrieves a valid double value or defaults to 0.0.
    *
    * <p>This method ensures that a `Double` value is non-null and greater than 0. 
    * If the value is null or less than or equal to 0, it returns 0.0.</p>
    *
    * @param value the `Double` value to validate.
    * @return the original value if valid; otherwise, 0.0.
    */
    private double safeDouble(Double value) {
	    return value != null && value > 0 ? value : 0.0;
	}

   /**
    * Safely retrieves a non-empty string or defaults to an empty string.
    *
    * <p>This method ensures that a string is non-null and not empty. 
    * If the value is null or empty, it returns an empty string.</p>
    *
    * @param value the string to validate.
    * @return the original string if valid; otherwise, an empty string.
    */
	private String safeString(String value) {
	    return value != null && !value.isEmpty() ? value : "";
	}

	/**
	 * Outputs a tick record with fixed column widths for console display and CSV output.
	 *
	 * <p>This method ensures consistent formatting for console display by using 
	 * fixed column widths and placeholders for missing data. For CSV output, 
	 * it generates standard CSV format without descriptions.</p>
	 *
	 * @param tickType the type of tick record (e.g., "TRADE", "QUOTE").
	 * @param symbol the symbol associated with the tick record.
	 * @param timestamp the timestamp for the tick record.
	 * @param tradeTime the trade timestamp (if available).
	 * @param tradeExchange the exchange associated with the trade (if available).
	 * @param tradePrice the trade price (if available).
	 * @param tradeSize the trade size (if available).
	 * @param tradeQuals the trade qualifiers (if available).
	 * @param tradeTotalVolume the total volume for the trade (if available).
	 * @param tradeVWAP the volume-weighted average price for the trade (if available).
	 * @param bidTime the timestamp for the bid (if available).
	 * @param bidExchange the exchange associated with the bid (if available).
	 * @param bidPrice the bid price (if available).
	 * @param bidSize the bid size (if available).
	 * @param bidQuals the bid qualifiers (if available).
	 * @param askTime the timestamp for the ask (if available).
	 * @param askExchange the exchange associated with the ask (if available).
	 * @param askPrice the ask price (if available).
	 * @param askSize the ask size (if available).
	 * @param askQuals the ask qualifiers (if available).
	 * @param closeType the closing type or additional details (if available).
	 */
	private void outputTickRecord(
	        String tickType, String symbol, String timestamp, String flagsMsg,
	        String tradeTime, String tradeExchange, double tradePrice, double tradeSize, String tradeQuals, double tradeTotalVolume, double tradeVWAP,
	        String bidExchange, double bidPrice, double bidSize, String bidQuals,
	        String askExchange, double askPrice, double askSize, String askQuals,
	        String closeType
	) {
	    boolean isDisplay = consoleSample.displayData();
	    boolean isTradesOnly = consoleSample.isTradesOnly();
	    boolean isQuotesOnly = consoleSample.isQuotesOnly();

	    // Set placeholders for missing data
	    String placeholderString = "----";

	    // Replace empty or missing fields with placeholders
	    tradeTime = (tradeTime == null || tradeTime.isEmpty()) ? placeholderString : tradeTime;
	    tradeExchange = (tradeExchange == null || tradeExchange.isEmpty()) ? placeholderString : tradeExchange;
	    tradeQuals = (tradeQuals == null || tradeQuals.isEmpty()) ? placeholderString : tradeQuals;
	    bidExchange = (bidExchange == null || bidExchange.isEmpty()) ? placeholderString : bidExchange;
	    bidQuals = (bidQuals == null || bidQuals.isEmpty()) ? placeholderString : bidQuals;
	    askExchange = (askExchange == null || askExchange.isEmpty()) ? placeholderString : askExchange;
	    askQuals = (askQuals == null || askQuals.isEmpty()) ? placeholderString : askQuals;
	    closeType = (closeType == null || closeType.isEmpty()) ? placeholderString : closeType;

	    // Case 1: Trades only
	    if (isTradesOnly) {
	        if (isDisplay) {
	            System.out.printf(
	                "TYPE: %-10s SYMBOL: %-10s TIME: %-25s FLAGS: %-25s TIME: %-25s EXCHANGE: %-10s PRICE: %-10.5f SIZE: %-10.5f QUALIFIERS: %-25s TOTAL VOLUME: %-20.5f VWAP: %-10.5f CLOSE TYPE: %-15s\n",
	                tickType, symbol, timestamp, flagsMsg,tradeTime, tradeExchange, tradePrice, tradeSize, tradeQuals, tradeTotalVolume, tradeVWAP, closeType
	            );
	        } else {
	            // CSV Output
	        	if (!columnsPrinted) {
	        		System.out.println("RECORD TYPE,SYMBOL,TIME,FLAGS," +
		                               "TRADE TIME,TRADE EXCHANGE,TRADE PRICE,TRADE SIZE,TRADE QUALIFIERS,TRADE TOTAL VOLUME,TRADE VWAP,CLOSE TYPE");
	        		columnsPrinted = true;
	        	}
	            System.out.printf(
	                "TRADE,%s,'%s,%s,'%s,%s,%.5f,%.5f,%s,%.5f,%.5f,%s\n",
	                symbol, timestamp, flagsMsg, tradeTime, tradeExchange, tradePrice, tradeSize, tradeQuals, tradeTotalVolume, tradeVWAP, closeType
	            );
	        }
	    }

	    // Case 2: Quotes only
	    else if (isQuotesOnly) {
	        if (isDisplay) {
	            System.out.printf(
	                "TYPE: %-10s SYMBOL: %-10s TIME: %-25s FLAGS: %-25s BID PRICE: %-10.5f BID EXCHANGE: %-10s BID SIZE: %-10.5f BID QUALIFIERS: %-25s " +
	                "ASK PRICE: %-10.5f ASK EXCHANGE: %-10s ASK SIZE: %-10.5f ASK QUALIFIERS: %-25s\n",
	                tickType, symbol, timestamp, flagsMsg, bidPrice, bidExchange, bidSize, bidQuals, askPrice, askExchange, askSize, askQuals
	            );
	        } else {
	            // CSV Output
	        	if (!columnsPrinted) {
	        		System.out.println("RECORD TYPE,SYMBOL,TIME,FLAGS," +
					                   "BID PRICE,BID EXCHANGE,BID SIZE,BID QUALIFIERS," +
		                               "ASK PRICE,ASK EXCHANGE,ASK SIZE,ASK QUALIFIERS,CLOSE TYPE");
	        		columnsPrinted = true;
	        	}
	            System.out.printf(
	                "QUOTE,%s,'%s,%s,%.5f,%s,%.5f,%s,%.5f,%s,%.5f,%s\n",
	                symbol, timestamp, flagsMsg, bidPrice, bidExchange, bidSize, bidQuals, askPrice, askExchange, askSize, askQuals
	            );
	        }
	    }

	    // Case 3: Both trades and quotes
	    else if (!isTradesOnly && !isQuotesOnly) {
	        if (isDisplay) {
	            // Display both trades and quotes with descriptions
	            System.out.printf(
	                "TYPE: %-10s SYMBOL: %-10s TIME: %-25s FLAGS: %-25s TRADE TIME: %-25s TRADE PRICE: %-10.5f TRADE EXCHANGE: %-10s TRADE SIZE: %-10.5f TRADE QUALIFIERS: %-25s TRADE TOTAL VOLUME: %-20.5f TRADE VWAP: %-10.5f " +
	                "BID PRICE: %-10.5f BID EXCHANGE: %-10s BID SIZE: %-10.5f BID QUALIFIERS: %-25s ASK PRICE: %-10.5f ASK EXCHANGE: %-10s ASK SIZE: %-10.5f ASK QUALIFIERS: %-25s CLOSE TYPE: %-15s\n",
	                tickType, symbol, timestamp, flagsMsg, tradeTime, tradePrice, tradeExchange, tradeSize, tradeQuals, tradeTotalVolume, tradeVWAP,
	                bidPrice, bidExchange, bidSize, bidQuals, askPrice, askExchange, askSize, askQuals, closeType
	            );
	        } else {
	            // CSV Output for both
	        	if (!columnsPrinted) {
	        		System.out.println("RECORD TYPE,SYMBOL,TIME,FLAGS," +
		                               "TRADE TIME,TRADE PRICE,TRADE EXCHANGE,TRADE SIZE,TRADE QUALIFIERS,TRADE TOTAL VOLUME,TRADE VWAP," +
					                   "BID PRICE,BID EXCHANGE,BID SIZE,BID QUALIFIERS," +
		                               "ASK PRICE,ASK EXCHANGE,ASK SIZE,ASK QUALIFIERS,CLOSE TYPE");
	        		columnsPrinted = true;
	        	}
	            System.out.printf(
	                "%s,%s,'%s,%s,'%s,%.5f,%s,%.5f,%s,%.5f,%.5f,%.5f,%s,%.5f,%s,%.5f,%s,%.5f,%s,%s\n",
	                tickType, symbol, timestamp, flagsMsg, tradeTime, tradePrice, tradeExchange, tradeSize, tradeQuals, tradeTotalVolume, tradeVWAP,
	                bidPrice, bidExchange, bidSize, bidQuals, askPrice, askExchange, askSize, askQuals, closeType
	            );
	        }
	    }
	}

   /* ---------------------------------------------------------------------------------------------
    * HELPER METHODS - FLEX DATA PROCESSING
    * ---------------------------------------------------------------------------------------------
    */
   /**
    * Processes a single flexible data field and returns its formatted value.
    *
    * <p>This method evaluates the type of the given `FlexDataField` and converts it 
    * to a human-readable string. It supports various data types, including numeric, 
    * string, date, and time fields, as well as sub-second precision.</p>
    *
    * @param dataField the `FlexDataField` to process.
    * @return a formatted string representing the value of the field or an error message if processing fails.
    */
   public String processFlexibleDataField(FlexDataField dataField) {
       if (dataField.isNull()) {
           return "----";
       }
   
       try {
           switch (dataField.getType()) {
               case BOOLEAN:
                   return Boolean.toString(dataField.getValueAsBoolean());
               case BYTEARRAY:
                   return formatByteArray(dataField.getValueAsByteArray());
               case CHAR:
                   return formatCharField(dataField);
               case DATE:
                   return String.valueOf(dataField.getValueAsLong());
               case DATETIME:
                   return String.valueOf(dataField.getValueAsBigInteger());
               case DOUBLE:
                   return formatDoubleField(dataField);
               case INT16:
                   return String.valueOf(dataField.getValueAsShort());
               case INT32:
                   return String.valueOf(dataField.getValueAsInteger());
               case INT64:
                   return String.valueOf(dataField.getValueAsLong());
               case INT8:
                   // The INT8 data type aligns with the Java byte primitive
                   return String.valueOf(dataField.getValueAsByte());
               case STRING:
                   return dataField.getValueAsString(null);
               case TIME:
                   return formatTimeField(dataField);
               case TIMEMICRO:
               case TIMENANO:
                   return formatSubSecondTimeField(dataField);
               case UINT16:
                   return String.valueOf(dataField.getValueAsInteger());
               case UINT32:
                   return String.valueOf(dataField.getValueAsLong());
               case UINT64:
                   return String.valueOf(dataField.getValueAsBigInteger());
               case UINT8:
                   return formatQualifierArray(dataField.getValueAsByteArray());
               default:
                   return "Unsupported Type";
           }
       } catch (InvalidDataConversionException e) {
           e.printStackTrace();
           return "Error";
       }
   }
   
   /**
    * Processes a composite flexible data field and returns its formatted value.
    *
    * <p>This method recursively processes composite fields, which contain nested 
    * flexible data fields. It combines their processed values into a single, 
    * formatted string for display or output.</p>
    *
    * @param dataField the composite `FlexDataField` to process.
    * @return a formatted string representing the values of the composite field.
    */
   public String processCompositeFlexibleDataField(FlexDataField dataField) {
       StringBuilder output = new StringBuilder();
       if (dataField.isNull()) {
           return "no data returned";
       }
       
       if (this.consoleSample.displayData()) {
          output.append("[");
          dataField.iterator().forEachRemaining(field -> {
              String fieldName = field.getName().toLowerCase().replace("_", " ") + ": ";
              output.append(fieldName).append(
                      field.getType() == FlexData.DATATYPE.COMPOSITE
                              ? processCompositeFlexibleDataField(field)
                              : processFlexibleDataField(field)
              ).append(", ");
          });
      
          if (output.length() > 1) {
              output.setLength(output.length() - 2);
          }
          output.append("]");
         
       } else {
          dataField.iterator().forEachRemaining(field -> {
              output.append(
                      field.getType() == FlexData.DATATYPE.COMPOSITE
                              ? processCompositeFlexibleDataField(field)
                              : processFlexibleDataField(field)
              ).append(",");
          });
       } 
       return output.toString();
   }
   
   /* ---------------------------------------------------------------------------------------------
    * HELPER METHODS - FLEX DATA FORMATTING
    * ---------------------------------------------------------------------------------------------
    */
   /**
    * Formats a byte array into a string.
    *
    * <p>This method converts a byte array to a UTF-8 string if the array is not 
    * null. If the array is null, it returns a placeholder indicating no data.</p>
    *
    * @param byteArray the byte array to format.
    * @return a string representation of the byte array or a placeholder for null arrays.
    */
   private String formatByteArray(byte[] byteArray) {
       return byteArray != null ? new String(byteArray, StandardCharsets.UTF_8) : "----";
   }
   
   /**
    * Formats a `CHAR` type flexible data field into a string.
    *
    * <p>This method converts the byte array representing a `CHAR` field into a 
    * trimmed string. It also updates the global `gExchange` variable if the field 
    * name matches `"exchange"`.</p>
    *
    * @param dataField the `CHAR` type flexible data field to format.
    * @return the formatted string representation of the field or a placeholder for null values.
    */
   private String formatCharField(FlexDataField dataField) {
       // The CHAR data type aligns with the Java byte primitive
       // However, it's possible to send back more than one char/byte
       byte[] byteArray = dataField.getValueAsByteArray();
       if (byteArray != null && byteArray[0] > 0) {
           String value = new String(byteArray, StandardCharsets.UTF_8).trim();
           if ("exchange".equalsIgnoreCase(dataField.getName())) {
               gExchange = value;
           }
           return value;
       }
       return "----";
   }
   
   /**
    * Formats a `DOUBLE` type flexible data field into a string.
    *
    * <p>This method converts a `DOUBLE` field to a string, with specific formatting 
    * for fields like `"size"` and `"cumvolume"`. It ensures consistent precision 
    * and presentation for numeric values.</p>
    *
    * @param dataField the `DOUBLE` type flexible data field to format.
    * @return the formatted string representation of the field.
    * @throws InvalidDataConversionException if the field cannot be converted to a `DOUBLE`.
    */
   private String formatDoubleField(FlexDataField dataField) throws InvalidDataConversionException {
       double value = dataField.getValueAsDouble();
       if ("size".equalsIgnoreCase(dataField.getName())) {
           return String.valueOf((int) value);
       } else if ("cumvolume".equalsIgnoreCase(dataField.getName())) {
           return String.format("%.0f", value);
       }
       return String.valueOf(value);
   }
   
   /**
    * Formats a `TIME` type flexible data field into a UTC-formatted string.
    *
    * <p>This method converts a whole-second `TIME` field to a formatted string 
    * in the UTC timezone, using a predefined date and time pattern.</p>
    *
    * @param dataField the `TIME` type flexible data field to format.
    * @return the formatted string representation of the time field.
    * @throws InvalidDataConversionException if the field cannot be converted to a `TIME`.
    */
   private String formatTimeField(FlexDataField dataField) throws InvalidDataConversionException {
       long value = dataField.getValueAsLong();
       Date date = new Date(value * 1000);
       SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss aa");
       formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
       return formatter.format(date);
   }
   
   /**
    * Formats a sub-second precision flexible data field into a string.
    *
    * <p>This method handles `TIMEMICRO` and `TIMENANO` data types, extracting 
    * and formatting both whole seconds and fractional seconds for high-precision 
    * time representation.</p>
    *
    * @param dataField the sub-second precision flexible data field to format.
    * @return the formatted string representation of the time field with sub-second precision.
    * @throws InvalidDataConversionException if the field cannot be converted to the appropriate type.
    */
   private String formatSubSecondTimeField(FlexDataField dataField) throws InvalidDataConversionException {
       long wholeSeconds;
       String fractional;
       
       if (dataField.getType() == DATATYPE.TIMEMICRO) {
           TimeMicro timeMicro = dataField.getValueAsTimeMicro();
           wholeSeconds = timeMicro.getTime() * 1000;
           fractional = String.format("%05d", timeMicro.getMicroseconds());
       } else {
           TimeNano timeNano = dataField.getValueAsTimeNano();
           wholeSeconds = timeNano.getTime() * 1000;
           fractional = String.format("%05d", timeNano.getNanoseconds());
       }
       
       return formatTimeWithFractional(wholeSeconds, fractional);
   }
   
   /**
    * Combines whole seconds and fractional seconds into a formatted timestamp.
    *
    * <p>This method takes a whole-second value and a fractional part, combining 
    * them into a formatted string representation of the timestamp.</p>
    *
    * @param wholeSeconds the whole-second value as a timestamp.
    * @param fractional the fractional seconds as a string.
    * @return the formatted timestamp with fractional seconds.
    */
   private String formatTimeWithFractional(long wholeSeconds, String fractional) {
       SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss aa");
       Date date = new Date(wholeSeconds);
       String timestamp = formatter.format(date);
       int amPmIndex = timestamp.lastIndexOf(" ");
       return timestamp.substring(0, amPmIndex) + "." + fractional + timestamp.substring(amPmIndex);
   }
   
   /**
    * Formats an array of qualifier values into a string.
    *
    * <p>This method converts a byte array of qualifier values into a string 
    * representation, with each value separated by a delimiter (e.g., `" | "`). 
    * It handles unsigned byte-to-integer conversion to ensure proper display.</p>
    *
    * @param byteArray the byte array of qualifier values to format.
    * @return a formatted string representation of the qualifier values.
    */
   private String formatQualifierArray(byte[] byteArray) {
       StringBuilder output = new StringBuilder();
       for (byte value : byteArray) {
           int unsignedValue = Byte.toUnsignedInt(value);
           output.append(unsignedValue).append(" | ");
       }
       return output.toString();
   }
}
