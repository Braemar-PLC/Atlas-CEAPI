package QuoteManager.StockOptionRootSample;


import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.StockOptionRootsEvent;
import com.esignal.jstandard.event.StockOptionRootsListener;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with an option root request.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class MyConsoleStockOptionRootsListener implements StockOptionRootsListener {
   
   private StockOptionRootSample consoleSample;
   private int completeCount = 0;
   
   public MyConsoleStockOptionRootsListener(StockOptionRootSample consoleSample) {
      this.consoleSample = consoleSample;
   }
   
   public void displayData(Quote quote) {

      String[] returnedRootsData = null;   // Contains an array of strings.  Each string contains information about a specific option root.
                                 // Each option root string contains a pipe ( "|" ) delimited set of elements.
                                 // For example:  |493|O:AAPL|119|AAPL|497|OXASPS|,|493|O:AAPL7|119|AAPL|497|OXASPS|
                                 //                       element 0             ,           element 1

      String rootList = "";            // Contains the "|" delimited string of a single element from the returnedRootsData array.
                                 // For example:  |493|O:AAPL|119|AAPL|497|OXASPS|
      
      String[] roots = null;            // Contains an array of strings.  Each string is an element of the rootList string after it has
                                 // been "tokenized" by splitting the string at each occurrence of the pipe ( "|" ) character.
                                 // For example: 493,O:AAPL,119,AAPL,497,OXASPS

      String underlying = "";   
      boolean printedUnderlying = false;
      String root = "";
      String optionType = "";
      String nonstandardDescription = "";
      
      FieldItem item = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_OPTION_EROOTS); 
      returnedRootsData = item.getValueAsStringArray();   

      for (int x = 0; x < returnedRootsData.length; x++)   
      {
         rootList = returnedRootsData[x];

         roots = rootList.split("[|]+"); // We tokenize by splitting on "|"

         for (int y = 1; y < roots.length - 1; y = y + 2) {  // We increment by 2 to ensure we test on the numerical ID values for each field
            int fieldId = Integer.valueOf(roots[y]).intValue();

            switch (fieldId) {
               case Quote.FieldItem.LRT_TYPE_UNDERLYING:
                  underlying = roots[y+1];
                  break;
               case Quote.FieldItem.LRT_TYPE_OPTION_EROOT:
                  root = "ROOT:  " + roots[y+1];
                  break;
               case Quote.FieldItem.LRT_TYPE_OPTION_TYPE:
                  optionType = "OPTION TYPE:  " + roots[y+1];
                  break;
               case Quote.FieldItem.LRT_TYPE_OPTION_NONSTANDARD_DESCRIPTION:
                  nonstandardDescription = "          NONSTANDARD DESCRIPTION:  " + roots[y+1] + "\n";
                  break;
               default:
                  break;
            }
         }
         
         // All option roots for a given instrument have the same underlyer; For the sample we print the underlyer only once.
         if (!printedUnderlying)
         {
            System.out.println("SYMBOL: " + underlying);
            printedUnderlying = true;
         }
         
         System.out.println("   " + root + "   \t" + optionType);
         
         if (!nonstandardDescription.isEmpty())
            System.out.println(nonstandardDescription);
      }
   }
   
      // Required to implement this class, but not handled by this sample
   public void onRequested(SymbolEvent event) {
   }

   public void onComplete(SymbolEvent event) {
      this.completeCount++;
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
         System.out.println("Your account is not enitled for symbol: " + event.getSymbol());      
      }
      System.exit(-1);   
   }

   public void onResponse(StockOptionRootsEvent event) {
         
      // When a response is received get the Quote object from the event
      // then pass that Quote object into a method to parse the data
      Quote quote = event.getStockOptionRoot();
      displayData(quote);
   }
}
