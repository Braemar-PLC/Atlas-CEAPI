package QuoteManager.SymbolCheckSample;

import java.util.Arrays;

import com.esignal.jstandard.beans.quote.Status;
import com.esignal.jstandard.event.KeyLookupEvent;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.QuoteKeyListener;
import com.esignal.jstandard.event.QuoteRequestListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with a streaming or
 * snapshot request.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *  
 * 
 */
@SuppressWarnings("unused")
public class MyConsoleSymbolCheckListener implements QuoteKeyListener {

   private SymbolCheckSample consoleSample;
   private int completeCount = 0;
   private String key;

   public MyConsoleSymbolCheckListener(SymbolCheckSample consoleSample) {
      this.consoleSample = consoleSample;
   }


   /**
    * Called from either onResponse or onUpdate events, this function processes
    * retrieved data.
    *  
    * Note: This sample only outputs when there is an update, and only handles 
    *       instrument key, company name, last, bid and ask.
    * 
    * @param quote
    */
   
    public void onComplete(KeyLookupEvent event) {
        this.completeCount++;
        try {
            if (this.completeCount == this.consoleSample.getRequestCount())
                this.consoleSample.exitSample(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onError(KeyLookupEvent event) {
       System.out.println(key);  // Recorded as part of the onRequested event
                                 // You can also get the key from event.getkey, but we have already formatted the display
                                 // within onRequested based upon the conversionType of the request.
        System.out.println("    *** NO MATCH -- ERROR CODE: " + event.getStatusString());
        this.completeCount++;
        try {
            if (this.completeCount >= this.consoleSample.getRequestCount())
                this.consoleSample.exitSample(true);
            else {
                this.consoleSample.setReadyToRequest(true);
                this.consoleSample.getData();                       
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onRequested(KeyLookupEvent event) {
        switch (consoleSample.getConversionType()) {
           case SEDOL:
               key = "SEDOL: " + event.getKey();
               break;
           case ISIN:
               key = "ISIN: " + event.getKey();
               break;
           case CUSIP:
               key = "CUSIP: " + event.getKey();
               break;
           case WKN:
               key = "WKN: " + event.getKey();
               break;
           case SYM_TO_CUSIP:
              key = "SYMBOL: " + event.getKey();
              break;
           case SYM_TO_MIC:
              key = "SYMBOL: " + event.getKey();
              break;
           case OSI21:
              key = "OSI21: " + event.getKey();
              break;
           case IDCO22:
              key = "IDCO22: " + event.getKey();
              break;
           default:
               key = "KEY: " + event.getKey();
               break;
        }
    }

    public void onResponse(SymbolEvent event) {
       String output = "";
        if (consoleSample.printFailedOnly() == false) {
            switch (consoleSample.getConversionType()) {
              case SEDOL:
              case ISIN:
              case CUSIP:
              case WKN:
              case OSI21:
              case IDCO22:
                  output = "    SYMBOLS: " + Arrays.toString(event.getSymbols());    
                  break;
              case SYM_TO_CUSIP:
                  output = "    CUSIPS: " + Arrays.toString(event.getSymbols());    
                 break;
              case SYM_TO_MIC:
                  output = "    MICS: " + Arrays.toString(event.getSymbols());    
                 break;
              default:
                  output = "    SYMBOLS: " + Arrays.toString(event.getSymbols());    
                  break;
            }
            System.out.println(key + "\n" + output);
        }
        this.consoleSample.setReadyToRequest(true);
        this.consoleSample.getData();
    }
}
