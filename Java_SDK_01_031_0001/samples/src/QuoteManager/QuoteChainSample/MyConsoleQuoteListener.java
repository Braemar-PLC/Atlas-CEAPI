package QuoteManager.QuoteChainSample;

import com.esignal.jstandard.beans.quote.CidData;
import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.QuoteListener;
import com.esignal.jstandard.event.QuoteRequestListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Provides methods for handling various states associated with a chain request.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class MyConsoleQuoteListener implements QuoteListener, QuoteRequestListener {
   
   private QuoteChainSample consoleSample;
   private boolean isSnapshot = false;
   private String prevSymbol = "";
   private long totalSymbols = 0;
   private long totalSymbolsRemoved = 0;
   private ArrayList<String> symbolList = new ArrayList<String>();
      
   public MyConsoleQuoteListener(QuoteChainSample consoleSample) {
      this.consoleSample = consoleSample;
      
      if (!consoleSample.displayData()) {
         // Print field headers
         switch (consoleSample.getChainType()) {
            case FUTURE_CHAIN:
               System.out.println("SYMBOL,EXPIRATION,LAST,LAST SIZE,DATETIME LAST,LAST EXCHANGE,BID,BID SIZE,DATETIME BID,BID EXCHANGE,ASK,ASK SIZE,DATETIME ASK,ASK EXCHANGE,CFI,EXCHANGE CODES");
               break;
            case FUTURE_OPTIONS:
               System.out.println("SYMBOL,UNDERLYER,STRIKE,EXPIRATION,LAST,LAST SIZE,DATETIME LAST,LAST EXCHANGE,BID,BID SIZE,DATETIME BID,BID EXCHANGE,ASK,ASK SIZE,DATETIME ASK,ASK EXCHANGE,CFI,EXCHANGE CODES");
               break;
            case REGIONAL_MONTAGE:
               // REGIONAL_MONTAGE is not supported for this type of request 
               break;
            case STOCK_OPTIONS:
               System.out.println("SYMBOL,OSI21,IDCO22,UNDERLYER,STRIKE,EXPIRATION,LAST,LAST SIZE,DATETIME LAST,LAST EXCHANGE,BID,BID SIZE,DATETIME BID,BID EXCHANGE,ASK,ASK SIZE,DATETIME ASK,ASK EXCHANGE,CFI,EXCHANGE CODES");
               break;
            default:
               break;
         }
      }
   }
   
   public void setSnapshot(boolean isSnapshot) {
      this.isSnapshot = isSnapshot;
   }

   /**
    * Called from either onResponse or onUpdate events, this function processes
    * retrieved data.
    * 
    * @param quote
    */
   public void displayData(Quote quote) {

      String symbol        = "---";
      String underlyer     = "---";
      String strike        = "---";
      String expiration    = "---";
      String bid           = "---";
      String ask           = "---";
      String last          = "---";
      String datetimebid   = "---";
      String datetimeask   = "---";
      String datetimelast  = "---";
      String bidExg        = "---";
      String askExg        = "---";
      String lastExg       = "---";
      String bidSize       = "---";
      String askSize       = "---";
      String lastSize      = "---";
      String osi21         = "---";
      String idco22        = "---";
      String cfi           = "---";
      String exchangeCodes = "---";

      for (FieldItem item : quote) {
         
         switch (item.getId()) {
            case Quote.FieldItem.LRT_TYPE_KEY:
               symbol = formatAndRequest(item).toString();
               
               if (!symbolList.contains(symbol)) {
                  symbolList.add(symbol);
               }
               break;
            case Quote.FieldItem.LRT_TYPE_UNDERLYING:
               underlyer = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_PORC:
               System.out.println("PUT CALL: " + formatAndRequest(item).toString());
               break;
            case Quote.FieldItem.LRT_TYPE_STRIKE:
               strike = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_EXPIRATIONDATE:
               expiration = formatAndRequest(item).toString();
                break;
            case Quote.FieldItem.LRT_TYPE_BID:
               bid = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_ASK:
               ask = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_LAST:
               last = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_DATETIMEBID:
               datetimebid = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_DATETIMEASK:
               datetimeask = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_DATETIMERT:
               datetimelast = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_BIDSIZE:
               bidSize =  formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_ASKSIZE:
               askSize = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_TRADESIZE:
               lastSize = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_BIDEXG:
               bidExg = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_ASKEXG:
               askExg = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_TRADEEXG:
               lastExg = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_SYMBOL_OSI:
               osi21 = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_SYMBOL_IDCO:
               idco22 = formatAndRequest(item).toString();
               break;
            case Quote.FieldItem.LRT_TYPE_CFI:
               cfi = formatAndRequest(item).toString();
               break;         
            case Quote.FieldItem.LRT_TYPE_EXCHANGECODES:
                   exchangeCodes = formatAndRequest(item).toString();
                   break;
         }
      }
      
      switch (this.consoleSample.getChainType()) {
         case FUTURE_CHAIN:
            if (consoleSample.displayData()) {
               System.out.printf("SYMBOL: %-10sEXPIRATION: %-10sLAST: %-10sLASTSIZE: %-10sDATETIMELAST: %-30s"
                     + "LASTEXG: %-5sBID: %-10sBIDSIZE: %-10sDATETIMEBID: %-30sBIDEXG: %-5sASK: %-10sASKSIZE: %-10sDATETIMEASK: %-30sASKEXG: %-5sCFI: %-10sEXCHANGE CODES: %s", 
                     symbol, expiration, last, lastSize, datetimelast, lastExg, bid, bidSize, datetimebid, bidExg, ask, askSize, datetimeask, askExg,cfi,exchangeCodes);
            }
            else {
               System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                 symbol, expiration, last, lastSize, datetimelast, lastExg, 
                                 bid, bidSize, datetimebid, bidExg, ask, askSize, datetimeask, askExg,cfi,exchangeCodes);
            }
            break;
         case FUTURE_OPTIONS:
            if (consoleSample.displayData()) {
               System.out.printf("SYMBOL: %-20sUNDERLYER: %-10s STRIKE: %-10sEXPIRATION: %-10sLAST: %-10sLASTSIZE: %-10sDATETIMELAST: %-30s"
                     + "LASTEXG: %-5sBID: %-10sBIDSIZE: %-10sDATETIMEBID: %-30sBIDEXG: %-5sASK: %-10sASKSIZE: %-10sDATETIMEASK: %-30sASKEXG: %-5sCFI: %-10sEXCHANGE CODES: %s", 
                    symbol, underlyer, strike, expiration, last, lastSize, datetimelast, lastExg, bid, bidSize, datetimebid, bidExg, ask, askSize, datetimeask, askExg,cfi,exchangeCodes);
            }
            else {
               System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                  symbol, underlyer, strike, expiration, last, lastSize, datetimelast, lastExg, 
                                  bid, bidSize, datetimebid, bidExg, ask, askSize, datetimeask, askExg,cfi,exchangeCodes);
            }
            break;
         
         // NOTE:  REGIONAL_MONTAGE is not supported for this type of request
         //case REGIONAL_MONTAGE:
         //   break;
         
         case STOCK_OPTIONS:
            if (consoleSample.displayData()) {
            System.out.printf("SYMBOL: %-35sOSI21: %-25sIDCO22: %-25sUNDERLYER: %-10s STRIKE: %-10sEXPIRATION: %-10sLAST: %-10sLASTSIZE: %-10sDATETIMELAST: %-30s"
                  + "LASTEXG: %-5sBID: %-10sBIDSIZE: %-10sDATETIMEBID: %-30sBIDEXG: %-5sASK: %-10sASKSIZE: %-10sDATETIMEASK: %-30sASKEXG: %-5sCFI: %-10sEXCHANGE CODES: %s", 
                  symbol, osi21, idco22, underlyer, strike, expiration, last, lastSize, datetimelast, lastExg, bid, bidSize, datetimebid, bidExg, ask, askSize, datetimeask, askExg,cfi, exchangeCodes);
            }
            else {
               System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                 symbol, osi21, idco22, underlyer, strike, expiration, last, lastSize, datetimelast, lastExg, 
                                 bid, bidSize, datetimebid, bidExg, ask, askSize, datetimeask, askExg,cfi, exchangeCodes);
            }
            break;
         default:
            break;
      }

      System.out.println ("");
      
      if (this.isSnapshot) {
        this.consoleSample.removeSymbol(symbol);
        symbolList.remove(symbol);        
      }
   }

   Object formatAndRequest(FieldItem item) {
      switch (item.getFormat()){
         case BYTE: 
            byte byteItem = item.getValueAsByte();
            return byteItem;
         case BYTEARRAY:
            byte [] byteArrayItem = item.getValue();      
            return byteArrayItem;
         case CIDDATA: 
            CidData cidDataItem = item.getValue();
            return cidDataItem;
         case CIDFIELDDATA: 
            double cidFieldDataItem = item.getValueAsCidFieldData().getValue();
            return cidFieldDataItem;
         case CIDPRICEFIELDDATA:
            double cidPriceFieldDataItem = item.getValueAsCidPriceFieldData().getValue();            
            return cidPriceFieldDataItem;
         case DATE: 
            long dateItem = item.getValueAsInteger(false) * 1000L;
            Date date = new Date(dateItem);            
            return date.toString();
         case DOUBLE: 
            double doubleItem = item.getValueAsDouble();            
            return doubleItem;
         case INT32: 
            int int32Item = item.getValueAsInteger(false);            
            return int32Item;
         case SHORT: 
            short shortItem = item.getValueAsShort(true);            
            return shortItem;
         case STRING: 
            String stringItem = item.getValueAsString();            
            return stringItem;
         case STRINGARRAY: 
            byte [] stringArrayItem = item.getValue();            
            return stringArrayItem;
         case UINT32:
            int uint32Item = item.getValueAsInteger(false);            
            return uint32Item;
         case UINT64: 
            int uint64Item = item.getValueAsInteger(false);            
            return uint64Item;
         case UNKNOWN:
            // REVIEW           
            Object unknownItem = item.getValue();            
            return unknownItem;
         default:
            break;
      }
      return null;
   }// end formatAndRequest

   // Required to implement this class, but not handled by this sample
   public void onAdded(SymbolEvent event) {
      // Each time the sample adds an instrument for streaming data, the onAdded event
      // fires.  The first instrument in a Quote Chain subscription request is a specially
      // formatted instrument that begins with LIST_.  This special instrument requests the
      // service to provide all the instruments available to the "chain."  The JStandard API
      // then subscribes to each of these instruments.  Again, each subsequent subscribe
      // request results in the onAdded event firing.
      //
      // We track the total number of symbols to the sample has subscribed so that we
      // may unsubscribe (delete) those instruments from the watch list if we want to 
      // emulate a snapshot request.
      totalSymbols++;
   }

   
   public void onDeleted(SymbolEvent event) {
      totalSymbolsRemoved++;
      // totalSymbols - 1, because the first instrument is LIST_ 
      // and is no longer being tracked.
      if (totalSymbolsRemoved == totalSymbols-1) {
         try {
			this.consoleSample.exitSample(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
      }          
   }

   // Required to implement this class, but not handled by this sample
   public void onError(StatusEvent event) {
   }

   public void onError(SymbolEvent event) {
      if (event.getStatus() == DbcCodes.eSTATUS_NO_DATA.NOSERVERDATA || event.getStatus() == DbcCodes.eSTATUS_NO_DATA.NODATA) {
         System.out.println("Could not process with the error Status: " + event.getStatus().name());
         System.out.println("Invalid Symbol entered. Please check it again.");      
      }
      
      if (event.getStatus() == DbcCodes.eSTATUS_NO_DATA.NOTENT) {
         System.out.println("Could not process with the error Status: " + event.getStatus().name());
         System.out.println("Your account is not enitled for symbol: " + event.getSymbol());      
      }
      // The following exit is commented out to ensure the application doesn't exit prematurely.
      // The application can exit prematurely if you are not entitled for all the options data 
      // available to a chain.  For example, if the chain contains 100 options contracts, but 
      // you are not entitled for the exchange of the 50th option the application will exit and 
      // you will not see the remaining 50 options.
      //System.exit(-1);   
   }

   public void onUpdate(QuoteEvent event) {
      Quote quote = event.getQuote();
      displayData(quote);
   }

   // Required to implement this class, but not handled by this sample
   public void onComplete(SymbolEvent event) {
      
   }

   // Required to implement this class, but not handled by this sample
   public void onRequested(SymbolEvent event) {
   }

   public void onResponse(QuoteEvent event) {
      Quote quote = event.getQuote();
      displayData(quote);
   }
}
