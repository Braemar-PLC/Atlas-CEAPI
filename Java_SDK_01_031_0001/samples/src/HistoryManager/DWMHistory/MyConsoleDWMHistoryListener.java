package HistoryManager.DWMHistory;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.esignal.jstandard.beans.flex.FlexData;
import com.esignal.jstandard.beans.flex.FlexData.DATATYPE;
import com.esignal.jstandard.beans.flex.FlexData.TimeMicro;
import com.esignal.jstandard.beans.flex.FlexData.TimeNano;
import com.esignal.jstandard.beans.flex.FlexDataField;
import com.esignal.jstandard.beans.history.HistoricalBar;
import com.esignal.jstandard.beans.history.HistoricalBar.AGGREGATIONS;
import com.esignal.jstandard.beans.history.HistoricalBar.RESPONSE_METADATA_OPTION;
import com.esignal.jstandard.event.DictionaryEvent;
import com.esignal.jstandard.event.HistoricalBarEvent;
import com.esignal.jstandard.event.HistoricalBarFlexEvent;
import com.esignal.jstandard.event.HistoricalBarListener;
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
public class MyConsoleDWMHistoryListener implements HistoricalBarListener {
   // Holds a reference to the DWMHistorySample object so that certain functions may be
   // called from this listener.
   private DWMHistorySample consoleSample;

   // Holds the current count of returned BAR records.
   private int barCount;
   
   // Counts number of instruments (requests) that returned NO_DATA or resulted in some
   // of type of error that resulted in no records returned.
   private int nothingReturned;
   
   // Members for handling column headings for output to file
   private boolean columnsPrepared = false;;
   private int     columnIndex = 0;
   private boolean columnsPrinted = false;
   private String[] columnHeadings = new String[50];
   private String compositeColumnHeading = "";

   public MyConsoleDWMHistoryListener(DWMHistorySample histSample) {
      this.consoleSample   = histSample;
      this.barCount        = 0;
      this.nothingReturned = 0;
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
	  //System.out.println("onComplete for SymbolEvent: " + event.getSymbol());
      if (consoleSample.isDictionaryRequest())
         // note: The "flexible data" features are available for the "desktop offerings" only.
         consoleSample.setDictionaryComplete();
      else
         consoleSample.setHistComplete();
   }

   @Override
   public void onComplete(StatusEvent event) {
	  //System.out.println("onComplete for StatusEvent: " + event.getStatusString());
      if (consoleSample.isDictionaryRequest())
         // note: The "flexible data" features are available for the "desktop offerings" only.
         consoleSample.setDictionaryComplete();
      else
         consoleSample.setHistComplete();
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
          if (consoleSample.displayData() & barCount > 0)
             System.out.println("Total Records:  " + barCount);
          barCount = 0;
       }
       consoleSample.setHistComplete();
   }
   
   
   /* ---------------------------------------------------------------------------------------------
    * ERROR HANDLERS
    * ---------------------------------------------------------------------------------------------
    */
   /**
    * Handles errors that occur during the processing of a `StatusEvent`.
    *
    * <p>This method is invoked when an error occurs during the handling of a 
    * status-related request. It logs the error details to the console, regardless 
    * of the display setting.</p>
    *
    * @param event the `StatusEvent` containing details about the error.
    */
   @Override
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
      // -----------------------------------------------------------------------------
      // NOTE: This sample was designed for display on a console.
      //       The purpose of consoleSample.displayData() is to provide a means for 
      //       formatting the responses for display output or for writing to file.
      //       This sample supports display output only.
      // -----------------------------------------------------------------------------
      // This sample displays errors regardless of the display setting.
//      if (event.getStatus() == DbcCodes.HIST_RESPONSE_ERROR.NO_DATA) {
//        if (consoleSample.displayData()) {
//           System.out.println("SYMBOL:  " + event.getSymbol() + "   -   NO DATA");
//        } else {
//           System.out.println(event.getSymbol() + ",NO DATA");
//        }

//         System.out.println("Could not process with the error status: " + event.getStatus().name());
//         if (event.getSymbol().startsWith("%")) {
//            System.out.printf("The instrument entered ( %" + event.getSymbol() + " ) has a Continuous Contract symbol format.\n");
//            System.out.println("Please verify that you have set the CONTCHART option for this request.");
//         }
//         else
//            System.out.println("The instrument entered ( " + event.getSymbol() + " ) may be valid, but there may be no data for the given date range.");

//   }

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
         if (event.getStatus() == DbcCodes.HIST_RESPONSE_ERROR.SERVERBUSY) {
            System.out.println("   The server was unable to honor the request because it was \"busy.\"");
            System.out.println("   Please try again later or contact support if the issue persists.");
         }
         
         if (event.getStatus() == DbcCodes.HIST_RESPONSE_ERROR.NO_DATA) {
           if (event.getSymbol().startsWith("%")) {
              System.out.printf("The instrument entered ( %" + event.getSymbol() + " ) has a Continuous Contract symbol format.\n");
              System.out.println("Please verify that you have set the CONTCHART option for this request.");
           }
           else {
        	  System.out.println("   The instrument entered ( " + event.getSymbol() + " ) may be valid, but there may be no data for the given date range.");
        	  System.out.println("   If the request included \"Flexible Data\" column options, one or more columns may not be applicable to the instrument.");
           }
         }
   
         if (event.getStatus() == DbcCodes.HIST_RESPONSE_ERROR.NOT_ENT) {
            System.out.println("   Could not process with the error Status: " + event.getStatus().name());
            System.out.println("   Your account is not enitled for symbol: " + event.getSymbol());
         }
   
         if (event.getStatus() == DbcCodes.HIST_RESPONSE_ERROR.REQINCORRECT) {
            System.out.println("   Incorrect Request:" + event.getStatus().name());
         }
         System.out.println();
     }
     else {
    	 System.out.println(event.getSymbol() + ",NO DATA");
     }
     
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
    * Processes the response for an interday historical data request.
    *
    * <p>This method handles responses containing bar data for interday history requests. 
    * It formats and outputs the received data either to the console or in CSV format, 
    * depending on the display settings.</p>
    *
    * @param historicalBarEvent the event containing the response data for intraday history.
    */
   public void onResponse(HistoricalBarEvent historicalBarEvent) {   
      // -----------------------------------------------------------------------------
      // NOTE: This sample was designed for display on a console.
      //       The purpose of consoleSample.displayData() is to provide a means for 
      //       formatting the responses for display output or for writing to file.
      //       This sample supports display output only.
      // -----------------------------------------------------------------------------    
      if (consoleSample.displayData()) {
         // START Check for AGGREGATIONS applied to returned data.
         if ((historicalBarEvent.getFlags() & AGGREGATIONS.THIRTY_DAY_YIELD.getBitMask()) > 0)
            // The 30 DAY YIELD flag is set and that is applied to the data.
            System.out.printf("30 DAY YIELD APPLIED TO DATA\n");
         
         if ((historicalBarEvent.getFlags() & AGGREGATIONS.SIMPLE_YIELD.getBitMask()) > 0)
            // The SIMPLE YIELD flag is set and that is applied to the data.
            System.out.printf("SIMPLE YIELD APPLIED TO DATA\n");
         
         if ((historicalBarEvent.getFlags() & AGGREGATIONS.SEVEN_DAY_YIELD.getBitMask()) > 0)
            // The 7-DAY YIELD flag is set and that is applied to the data.
            System.out.printf("7-DAY YIELD APPLIED TO DATA\n");
         // END Check for AGGREGATIONS applied to returned data.
      }
      // When a response is received, feed the historicalBar object into a
      // List and parse the list to obtain specific fields for each bar
      List<HistoricalBar> histResponse = historicalBarEvent.getHistoricalBarList();
      
      for (HistoricalBar bar : histResponse) {
//         SimpleDateFormat d = new SimpleDateFormat("MMM dd, yyyy");
        SimpleDateFormat d = new SimpleDateFormat("MM/dd/yyyy");
        
        String date = "not set";
        String tradeDate = "not set";
        String settlementDate = "not set";
        
        // Some dates may not apply to a bar.  For example, not all bars include a settlement
        // or settlement date.  In these cases, attempts to format the date result in a NullPointerException
        // error.
        try {
           date = d.format(bar.getDate());
        } catch (NullPointerException e) {
           // This should not occur, but the check is here for "best practices" purposes.
           // Every bar has a bar date associated with it.
           date = "** error **";
        }
        
        try {
           tradeDate = d.format(bar.getTradeDate());
        } catch (NullPointerException e) {
           // Trade Date does not necessarily apply to all bars, but its absence does not
           // constitute an error.  The Trade Date was used for legacy products developed by
           // ICE Data Services and is not intended for external use.
           tradeDate = date;
        }    
        
        try {
           // settlement date may not exist in every bar, so check for null before formatting
           settlementDate = d.format(bar.getSettlementDate());
        } catch (NullPointerException e) {
           settlementDate = "----";
           //System.out.println("ERROR: Could not format settlement date ( " + e.toString() + " )");
        }         
        
          String open         = String.format("%.6f", bar.getOpen());
          String high         = String.format("%.6f", bar.getHigh());
          String low          = String.format("%.6f", bar.getLow());
          String close        = String.format("%.6f", bar.getClose());
          String volume       = String.format("%.6f", bar.getVolume());
          String settlement   = String.format("%.6f", bar.getSettlementPrice());
          String adjustments  = String.format("%.6f", bar.getAdjustments());
          String openInterest = String.format("%.6f", bar.getOpenInterest());
        
          if (consoleSample.displayData()) {
           // Handle special continuous contracts symbology that starts with %
           if (bar.getSymbol().startsWith("%"))
              System.out.printf("SYMBOL: %" + bar.getSymbol());
           else
              System.out.printf("SYMBOL: " + bar.getSymbol());
          } else {
           System.out.printf(bar.getSymbol() + ",");
          }
          String adjustmentColumnName = "SPLIT ADJ:";
     
          // Check if the response provides cumulative split adjustment values or % change values
          // for bar.getAdjustments()
          //
          // The "adjustments" field in a bar holds either the cumulative split adjustment factor,
          // or the percentage change from the last bar's close.  The default behavior is to provide
          // the cumulative split adjustment factor.  When the option, ADJEQPCTCHGPRIORBAR, is set
          // to TRUE the default value is overridden and the percentage change is provided.
          //
          // note: The remaining bar data is still subject to the setting of the SPLIT option and will
          //       represent adjusted (default) or unadjusted data depending upon how the SPLIT option 
          //       is set.                     
          if ((historicalBarEvent.getFlags() & 0x04000000) > 0)
             // This release does not provide an enum for checking if the % change flag is set.
             // The value for this flag is 0x04000000, so we will use that value to check.
             adjustmentColumnName = "PCT CHANGE:";
     
          if (consoleSample.displayData()) {
             // Print bar date -- this date should always be available
             System.out.printf("    DATE: %-15s ", date);

             // Print trade date -- not every bar will have a trade date available
             System.out.printf("TRADED: %-15s ", tradeDate);

             // Determine columns to print
             if ((historicalBarEvent.getFlags() & 0x04000000) > 0) {
                // This is an EOD NAV response
            	// close => NAV
            	System.out.printf("NAV: %-9s ", close);
             }
             
			 if ((historicalBarEvent.getFlags() & 0x02000000) > 0) {
			    // This is an EOD ASK response
			    // openInterest => ASK
			    System.out.printf("ASK: %-9s ", openInterest);
			 }

			 if ((historicalBarEvent.getFlags() & 0x01000000) > 0) {
			    // This is an EOD BID response
			    // settlement => BID
			    System.out.printf("BID: %-9s ", settlement);
			 }
			 
			 // Print common fields
             System.out.printf("OPEN: %-9s HIGH: %-9s LOW: %-9s CLOSE: %-9s VOLUME: %-14s OPEN INTEREST: %-9s ", open, high, low, close, volume, openInterest);
           
             // Print settlement date -- not every bar will have a settlement date available
             if (!settlementDate.equalsIgnoreCase("not set"))
                System.out.printf("SETTLEMENT: %-9s SETTLEMENT DATE: %-15s ", settlement, settlementDate);
             else
                System.out.printf("SETTLEMENT: %-9s SETTLEMENT DATE: %-15s ", "not set", settlementDate);
         
             // Print "adjustments" column
             System.out.printf("%s %-9s", adjustmentColumnName, adjustments);
     
             // Check for meta data as part of the response              
             if (bar.getMetaDataResponse(RESPONSE_METADATA_OPTION.BACK_ADJUST) != null) {
                Object backAdjustObject = bar.getMetaDataResponse(RESPONSE_METADATA_OPTION.BACK_ADJUST).getValue();
                if (backAdjustObject != null && backAdjustObject.getClass().equals(RESPONSE_METADATA_OPTION.BACK_ADJUST.getClassName())) {
                   if (backAdjustObject instanceof Long) {
                      Long value = (Long) backAdjustObject;
                      System.out.printf("   BACK ADJUST: " + value + "\n");
                   }
                }
             }
             System.out.printf("\n");
          }
          else {
             System.out.printf(date + "," + tradeDate + "," + open + "," + high + "," + low + "," + close + "," + volume + "," + openInterest + "," + settlement + "," + settlementDate + "\n");
          }
      }
   }

   @Override
   /**
    * Processes the response for a flexible bar record request.
    *
    * <p>This method handles responses containing flexible data fields. It processes 
    * both single-value and composite flexible fields and formats the output for 
    * console display or CSV export.</p>
    *
    * @param historicalFlexEvent the event containing the response data for flexible tick records.
    */
    public void onFlexResponse(HistoricalBarFlexEvent historicalFlexEvent) {
      Iterator<Iterator<FlexDataField>> recordIterator = historicalFlexEvent.iterator();
      
      String output;         
      
      if (consoleSample.displayData()) {
          // Displays the current response's schema.  Commented out to minimize visual clutter.
          //System.out.println("RESPONSE SCHEMA:\n" + historicalFlexEvent.getSchema() + "\n");      
       }
      
      try {
          while (recordIterator.hasNext()) {
             output = "";
             // getDisplayFormat returns true if the output is displayed within the console; false otherwise.
             if (this.consoleSample.displayData())
                 output += "SYMBOL:  " + historicalFlexEvent.getSymbol() + "  \t";
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
                     output += dataField.getName().toUpperCase().replace("_", " ") + ":  ";
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
                    else {
                        output += processFlexibleDataField(dataField) + ",";
                    }
                }
             }
             if (this.consoleSample.displayData()) 
                 System.out.println(output);
              else {
                 if (!columnsPrepared)
                    columnsPrepared = true;
                 
                 if (!columnsPrinted) {
                	 System.out.println(",," + compositeColumnHeading);
                	 for (int i = 0; i <= columnIndex; i++) {
                		 if (!columnHeadings[i].equalsIgnoreCase("SKIP"))
                			 System.out.print(columnHeadings[i] + ",");
                	 }
                	 columnsPrinted = true;
                	 System.out.print("\n");
                 }
                 System.out.println(output);
              }
              barCount++;
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
      // -----------------------------------------------------------------------------
      // NOTE: This sample was designed for display on a console.
      //       The purpose of consoleSample.displayData() is to provide a means for 
      //       formatting the responses for display output or for writing to file.
      //       This sample supports display output only.
      // -----------------------------------------------------------------------------      
      if (consoleSample.displayData()) {
         System.out.println(dictionaryEvent.getDictionary());
         System.out.println("");
      }
      else {
        // Currently, this sample does not support -disp OFF.
      }
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
		}
       
		dataField.iterator().forEachRemaining(field -> {
			String fieldName = field.getName().toLowerCase().replace("_", " ");
			
			if (this.consoleSample.displayData()) {
	            output.append(String.format("%s: %-10s\t", fieldName, 
	                    field.getType() == FlexData.DATATYPE.COMPOSITE
	                            ? processCompositeFlexibleDataField(field)
	                            : processFlexibleDataField(field)));
			} else {
				if (!columnsPrepared) {
					if (!compositeColumnHeading.isEmpty()) {
						columnHeadings[++columnIndex] = fieldName;
					}
				}
				output.append(field.getType() == FlexData.DATATYPE.COMPOSITE 
						? processCompositeFlexibleDataField(field)
						: processFlexibleDataField(field)).append(",");
			}
		});
       
       if (this.consoleSample.displayData()) {
      
          if (output.length() > 1) {
              output.setLength(output.length() - 2);
          }
          output.append("]");
         
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
    * in the UTC time zone, using a predefined date and time pattern.</p>
    *
    * @param dataField the `TIME` type flexible data field to format.
    * @return the formatted string representation of the time field.
    * @throws InvalidDataConversionException if the field cannot be converted to a `TIME`.
    */
   private String formatTimeField(FlexDataField dataField) throws InvalidDataConversionException {
       long value = dataField.getValueAsLong();
       Date date = new Date(value * 1000);
       SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss aa");
       
       // Times are stored and provided as Unix timestamps.
       // Unix timestamps define time as the number of seconds that have elapsed sine the Unix epoch:
       // 
       //    January 1, 1970, 00:00:00 UTC
       //
       // These values *do not* include time zone details, so it is necessary to convert them to the
       // appropriate time zone for display.
       // For example, if the source time zone for a record is New York and the execution took place
       // Monday, February 24, 2025 1:38:37 PM (Eastern Time), the Unix timestamp would be: 1740422317.
       // 
       // Unless the source time zone is known, it is best to convert the time to UTC for display.
       // In this case, the timestamp would be converted to:
       //
       //    Monday, February 24, 2025 6:38:37 PM (UTC)
       //
       // If the source time zone is known, the time can be converted to the appropriate time zone.
       // For example:
       //
       //    formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
       //    - or -
       //    formatter.setTimeZone(TimeZone.getTimeZone("GMT-5:00"));
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
       
       // Times are stored and provided as Unix timestamps.
       // Unix timestamps define time as the number of seconds that have elapsed sine the Unix epoch:
       // 
       //    January 1, 1970, 00:00:00 UTC
       //
       // These values *do not* include time zone details, so it is necessary to convert them to the
       // appropriate time zone for display.
       // For example, if the source time zone for a record is New York and the execution took place
       // Monday, February 24, 2025 1:38:37 PM (Eastern Time), the Unix timestamp would be: 1740422317.
       // 
       // Unless the source time zone is known, it is best to convert the time to UTC for display.
       // In this case, the timestamp would be converted to:
       //
       //    Monday, February 24, 2025 6:38:37 PM (UTC)
       //
       // If the source time zone is known, the time can be converted to the appropriate time zone.
       // For example:
       //
       //    formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
       //    - or -
       //    formatter.setTimeZone(TimeZone.getTimeZone("GMT-5:00"));
       formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
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
