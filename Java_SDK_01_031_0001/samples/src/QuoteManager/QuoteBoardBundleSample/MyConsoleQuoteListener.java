package QuoteManager.QuoteBoardBundleSample;

import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.QuoteListener;
import com.esignal.jstandard.event.QuoteRequestListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with a bundle
 * request.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class MyConsoleQuoteListener implements QuoteListener, QuoteRequestListener {

   private QuoteBoardBundleSample consoleSample;
   private int completeCount = 0;

   public MyConsoleQuoteListener(QuoteBoardBundleSample consoleSample) {
      this.consoleSample = consoleSample;
   }

   public void displayData(Quote quote) {
      String symbol      = "---";
      String underlyer   = "---";
      Double strike      = 0.0;
      String expiration  = "---";
      String type        = "---";
      String dayfilter   = "";
      String onnproduct  = "";
      String idcosymbol  = "";
      String osisymbol   = "";

      boolean displayAll = false;
      boolean displayDay = false;

      if (this.consoleSample.getDayFilter() != -1)
         dayfilter = "D" + Integer.toString(this.consoleSample.getDayFilter());
      else
         displayAll = true;

      for (FieldItem item : quote) {
         switch (item.getId()) {
         case Quote.FieldItem.LRT_TYPE_LIST_BUNDLE:
            String[] bundleItems = item.getValueAsStringArray();

            for (String pipeDelimitedBundleItem : bundleItems) {
               String[] bundleItem = pipeDelimitedBundleItem.split("[|]+"); // We tokenize by splitting on "|"

               for (int i = 0; i < bundleItem.length - 1; i = i + 2) {  // We increment by 2 to ensure we test on the fieldID values
                  int fieldId = Integer.valueOf(bundleItem[i]).intValue();

                  switch (fieldId) {
                     case Quote.FieldItem.LRT_TYPE_SYMBOL:
                        symbol = bundleItem[i + 1].trim();
                        if (displayAll == false) {
                           if (symbol.contains(dayfilter))
                              displayDay = true;
                        }
                        break;
                     case Quote.FieldItem.LRT_TYPE_OPTION_TYPE:
                        type = bundleItem[i + 1].trim();
                        break;
                     case Quote.FieldItem.LRT_TYPE_STRIKE:
                        strike = Double.parseDouble(bundleItem[i + 1].trim());
                        break;
                     case Quote.FieldItem.LRT_TYPE_EXPIRATIONDATE:
                        expiration = bundleItem[i + 1].trim();
                        break;
                     case Quote.FieldItem.LRT_TYPE_UNDERLYING:
                        underlyer = bundleItem[i + 1].trim();
                        break;
                     case Quote.FieldItem.LRT_TYPE_ONNPRODUCT:
                    	 onnproduct = bundleItem[i + 1].trim();
                    	 break;
                     case Quote.FieldItem.LRT_TYPE_SYMBOL_IDCO:
                    	 idcosymbol = bundleItem[i + 1].trim();
                    	 break;
                     case Quote.FieldItem.LRT_TYPE_SYMBOL_OSI:
                    	 osisymbol = bundleItem[i + 1].trim();
                    	 break;
                     default:
                        break;
                  }
               }

               if (displayAll || displayDay) {
                  switch (this.consoleSample.getChainType()) {
                     case FUTURE_CHAIN:
                        System.out.printf("SYMBOL: %-15sEXPIRATION: %-10sTYPE: %-10s", symbol, expiration, type);
                        break;
                     default:
                        System.out.printf("SYMBOL: %-22sUNDERLYER: %-10sSTRIKE: %-12f EXPIRATION: %-10sTYPE: %-10sONN PRODUCT: %-24sIDCO SYMBOL: %-24sOSI SYMBOL: %s", symbol, underlyer, strike, expiration, type, onnproduct, idcosymbol, osisymbol);
                        break;
                  }
                  System.out.println("");
                  // We must set the displayDay value back to false because this single update may include
                  // additional records and we don't want to display those if they don't meet the day 
                  // filter requirement.
                  // We don't need to reset the displayAll value because it is either true or false for
                  // the entire update.
                  displayDay = false;
               }
            }
            break;
         }
      }
   }

   // Required to implement this class, but not handled by this sample
   public void onAdded(SymbolEvent event) {
   }

   // Required to implement this class, but not handled by this sample
   public void onDeleted(SymbolEvent event) {
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
      System.exit(-1);
   }

   public void onUpdate(QuoteEvent event) {
      Quote quote = event.getQuote();
      displayData(quote);
   }

   public void onComplete(SymbolEvent event) {
      this.completeCount++;
      try {
         if (this.completeCount == this.consoleSample.getRequestCount()) {
            this.consoleSample.exitSample(true);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // Required to implement this class, but not handled by this sample
   public void onRequested(SymbolEvent event) {
   }

   public void onResponse(QuoteEvent event) {
      Quote quote = event.getQuote();
      displayData(quote);
   }
}
