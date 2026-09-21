package QuoteManager.OptionMonthSample;

import java.util.List;

import com.esignal.jstandard.event.OptionMonthsEvent;
import com.esignal.jstandard.event.OptionMonthsListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with requesting option months.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class MyConsoleOptionMonthsListener implements OptionMonthsListener {

   private OptionMonthSample consoleSample;
   private int completeCount = 0;

   public MyConsoleOptionMonthsListener(OptionMonthSample consoleSample) {
      this.consoleSample = consoleSample;
   }

   // Required to implement this class, but not handled by this sample
   public void onRequested(SymbolEvent event) {
	   System.out.println("Requested: " + event.getSymbol());
   }

   public void onComplete(SymbolEvent event) {
      // Increment number of completed requests
      this.completeCount++;

      // Determine if all requests have been processed, and if so, exit the sample
      try {
         if (this.completeCount == this.consoleSample.getRequestCount())
            this.consoleSample.exitSample(true);
      } catch (Exception e) {
         e.printStackTrace();
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
         System.out.println("Your account is not entitled for symbol: " + event.getSymbol());
      }
      System.exit(-1);
   }

   public void onResponse(OptionMonthsEvent event) {

      System.out.println("SYMBOL: " + event.getSymbol());

      // When a response is received, feed the optionMonth object into a
      // List and parse the list to obtain specific months.
      List<String> monthResponse = event.getOptionMonths();

      for (String month : monthResponse) {
         System.out.println("    MONTH: " + month);
      }
   }
}
