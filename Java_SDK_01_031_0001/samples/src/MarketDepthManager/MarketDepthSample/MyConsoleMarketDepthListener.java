package MarketDepthManager.MarketDepthSample;

import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

import com.esignal.jstandard.beans.marketdepth.AggregateDepthRecord;
import com.esignal.jstandard.beans.marketdepth.BookByPriceRecord;
import com.esignal.jstandard.beans.marketdepth.BookByMarketMakerRecord;
import com.esignal.jstandard.beans.marketdepth.BookByMarketMakerExtendedRecord;
import com.esignal.jstandard.beans.marketdepth.MarketDepth;
import com.esignal.jstandard.beans.marketdepth.OrderBookRecord;
import com.esignal.jstandard.event.MarketDepthEvent;
import com.esignal.jstandard.event.MarketDepthInfoEvent;
import com.esignal.jstandard.event.MarketDepthListener;
import com.esignal.jstandard.event.NetOrderImbalanceEvent;
import com.esignal.jstandard.event.StatusEnum.TYPE;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

/**
 * Provides methods for handling various states associated with a Level 2
 * request.
 * 
 * @autor ICE Data Services - Developer Support
 */
public class MyConsoleMarketDepthListener implements MarketDepthListener {
   private MarketDepthSample consoleSample;

   String gInstrument = "";
   double gAsk = 0;
   double gBid = 0;
   double gLastAsk = 0;
   double gLastBid = 0;
   
   int symbolCount = 0;
   
   public MyConsoleMarketDepthListener(MarketDepthSample marketDepthSample) {
      this.consoleSample = marketDepthSample;
   }

   public void onAdded(SymbolEvent event) {
     if (consoleSample.displayData()) {
        if (event.getStatusType() == TYPE.ERROR) {
           if (event.getStatus() == DbcCodes.DBCAPI_ERROR.NOTCONNECTED) {
              System.out.println("onAdded - The client is not connected.");
              System.exit(-1);
           }
        }
     }
   }

   public void onDeleted(SymbolEvent event) {
      if (consoleSample.displayData()) {
         if (event.getStatusType() == TYPE.ERROR) {
            if (event.getStatus() == DbcCodes.DBCAPI_ERROR.NOTCONNECTED) {
               System.out.println("onDeleted - The client is not connected.");
               System.exit(-1);
            }
            else {
               System.out.println("onDeleted - processing for symbol:" + event.getSymbol() + ": with status: " + event.getStatus());
            }
         }
      }
   }

   public void onError(StatusEvent event) {
     if (!consoleSample.displayData())
          return;
      
      System.out.println("onError during processing, returning status: " + event.getStatus());
      System.exit(-1);
   }

   public void onError(MarketDepthEvent event) {
     if (!consoleSample.displayData())
          return;

     if (event.getStatus() == DbcCodes.DBCAPI_ERROR.DEPTH_SYMBOL_NOT_FOUND) {
         System.out.println("Could not process with the error Status: " + event.getStatus().name());
         System.out.println("Invalid Symbol entered. Please check it again.");
      }
   }

   public void onUpdate(MarketDepthEvent event) {
     if (!consoleSample.displayData())
        return;
     
      String instrument = null;
      String marketMaker = null;
      String orderId = null;
      String askOut = null;
      String bidOut = null;
      String orderCount = null;
      String noValue = "---";
      String operation = null;

      double ask = 0;
      double bid = 0;

      int level = 0;
      int orderSize = 0;
      
      List<MarketDepth> marketDepthList = event.getMarketDepthList();

      if (consoleSample.displayData()) {      
         if (!marketDepthList.isEmpty()) {  
            for (MarketDepth list : marketDepthList) {
               instrument = list.getSymbol();
               gInstrument = instrument;   
               
               switch (event.getNewMarketDepthType()) {
                  case ORDER_BOOK:
                     OrderBookRecord orderBookRecord = (OrderBookRecord)list;            

                     orderId = orderBookRecord.getOrderId();
                     operation = orderBookRecord.getOperation().toString();

                     if (list.getAsk() != null) {
                        ask = orderBookRecord.getAsk().getValue();
                        gAsk = (gAsk != ask) ? ask : gAsk;
                        orderSize = orderBookRecord.getAsk().getSize();
                     }

                     if (list.getBid() != null) {
                        bid = orderBookRecord.getBid().getValue();
                        gBid = (gBid != bid) ? bid : gBid;
                        orderSize = orderBookRecord.getBid().getSize();
                     }

                     if (gAsk == 0 || gAsk == gLastAsk)
                        askOut = noValue;
                     else 
                        askOut = String.format("%.2f", gAsk);
                     
                     if (gBid == 0 || gBid == gLastBid)
                        bidOut = noValue;
                     else
                        bidOut = String.format("%.2f", gBid);
                     
                     if (orderSize != 0 && bidOut != noValue)
                        System.out.printf("Symbol: %-10s OrderId: %-10s BID: %-10s Size: %-10d OrderId: %-10s ASK: %-10s Size: %-10s Operation: %-10s\n", gInstrument, orderId, bidOut, orderSize, noValue, askOut, noValue, operation);
                     else if (orderSize != 0 && askOut != noValue)
                        System.out.printf("Symbol: %-10s OrderId: %-10s BID: %-10s Size: %-10s OrderId: %-10s ASK: %-10s Size: %-10d Operation: %-10s\n", gInstrument, noValue, bidOut, noValue, orderId, askOut, orderSize, operation);
                     gLastAsk = gAsk;
                     gLastBid = gBid;
                     break;
                  case BOOK_BY_PRICE:
                     BookByPriceRecord bookByPriceRecord = (BookByPriceRecord)list;
                     if (bookByPriceRecord.getOrderCount() == 0)
                        orderCount = noValue;
                     else
                        orderCount = String.valueOf(bookByPriceRecord.getOrderCount());
                     
                     operation = bookByPriceRecord.getOperation().toString();
                     
                     if (list.getAsk() != null) {
                        ask = bookByPriceRecord.getAsk().getValue();
                        gAsk = (gAsk != ask) ? ask : gAsk;
                        orderSize = bookByPriceRecord.getAsk().getSize();
                     }

                     if (list.getBid() != null) {
                        bid = bookByPriceRecord.getBid().getValue();
                        gBid = (gBid != bid) ? bid : gBid;
                        orderSize = bookByPriceRecord.getBid().getSize();
                     }

                     if (gAsk == 0 || gAsk == gLastAsk)
                        askOut = noValue;
                     else 
                        askOut = String.format("%.2f", gAsk);
                     
                     if (gBid == 0 || gBid == gLastBid)
                        bidOut = noValue;
                     else
                        bidOut = String.format("%.2f", gBid);
                        
                     if (bidOut != noValue)
                        System.out.printf("Symbol: %-10s BID: %-10s Size: %-10d Order Count: %-10s ASK: %-10s Size: %-10s Order Count: %-10s Operation: %-10s\n", gInstrument, bidOut, orderSize, orderCount, askOut, noValue, noValue, operation);
                     else if (askOut != noValue)
                        System.out.printf("Symbol: %-10s BID: %-10s Size: %-10s Order Count: %-10s ASK: %-10s Size: %-10d Order Count: %-10s Operation: %-10s\n", gInstrument, bidOut, noValue, noValue, askOut, orderSize, orderCount, operation);
                     gLastBid = gBid;
                     gLastAsk = gAsk;
                     break;            
                  case BOOK_BY_MARKET_PARTICIPANT:
                     BookByMarketMakerRecord bookByMarketMaker = (BookByMarketMakerRecord)list;

                     marketMaker = bookByMarketMaker.getMarketMaker();
                     operation = bookByMarketMaker.getOperation().toString();

                     if (list.getAsk() != null) {
                        ask = bookByMarketMaker.getAsk().getValue();
                        gAsk = (gAsk != ask) ? ask : gAsk;
                        orderSize = bookByMarketMaker.getAsk().getSize();
                     }

                     if (list.getBid() != null) {
                        bid = bookByMarketMaker.getBid().getValue();
                        gBid = (gBid != bid) ? bid : gBid;
                        orderSize = bookByMarketMaker.getBid().getSize();
                     }

                     if (gAsk == 0 || gAsk == gLastAsk)
                        askOut = noValue;
                     else 
                        askOut = String.format("%.2f", gAsk);
                     
                     if (gBid == 0 || gBid == gLastBid)
                        bidOut = noValue;
                     else
                        bidOut = String.format("%.2f", gBid);
                        
                     if (bidOut != noValue)
                        System.out.printf("Symbol: %-10s MMID: %-10s BID: %-10s Size: %-10d MMID: %-10s ASK: %-10s Size: %-10s Operation: %-10s\n", gInstrument, marketMaker, bidOut, orderSize, noValue, askOut, noValue, operation);
                     else if (askOut != noValue)
                        System.out.printf("Symbol: %-10s MMID: %-10s BID: %-10s Size: %-10s MMID: %-10s ASK: %-10s Size: %-10d Operation: %-10s\n", gInstrument, bidOut, noValue, noValue, marketMaker, askOut, orderSize, operation);
                     gLastBid = gBid;
                     gLastAsk = gAsk;               
                     break;
                  case BOOK_BY_MARKET_PARTICIPANT_EXTENDED:
                     BookByMarketMakerExtendedRecord bookByMarketMakerExtended = (BookByMarketMakerExtendedRecord)list;
                        
                     marketMaker = bookByMarketMakerExtended.getMarketMaker();
                     operation = bookByMarketMakerExtended.getOperation().toString();

                     if (list.getAsk() != null) {
                        ask = bookByMarketMakerExtended.getAsk().getValue();
                        gAsk = (gAsk != ask) ? ask : gAsk;
                        orderSize = bookByMarketMakerExtended.getAsk().getSize();
                     }

                     if (list.getBid() != null) {
                        bid = bookByMarketMakerExtended.getBid().getValue();
                        gBid = (gBid != bid) ? bid : gBid;
                        orderSize = bookByMarketMakerExtended.getBid().getSize();
                     }

                     if (gAsk == 0 || gAsk == gLastAsk)
                        askOut = noValue;
                     else 
                        askOut = String.format("%.2f", gAsk);
                     
                     if (gBid == 0 || gBid == gLastBid)
                        bidOut = noValue;
                     else
                        bidOut = String.format("%.2f", gBid);
                        
                     if (bidOut != noValue)
                        System.out.printf("Symbol: %-10s MMID: %-10s BID: %-10s Size: %-10d MMID: %-10s ASK: %-10s Size: %-10s Operation: %-10s\n", gInstrument, marketMaker, bidOut, orderSize, noValue, askOut, noValue, operation);
                     else if (askOut != noValue)
                        System.out.printf("Symbol: %-10s MMID: %-10s BID: %-10s Size: %-10s MMID: %-10s ASK: %-10s Size: %-10d Operation: %-10s\n", gInstrument, bidOut, noValue, noValue, marketMaker, askOut, orderSize, operation);
                     gLastBid = gBid;
                     gLastAsk = gAsk;               
                     break;
                  case BOOK_BY_LEVEL:
                     AggregateDepthRecord aggregateDepthRecord = (AggregateDepthRecord)list;
                     level = aggregateDepthRecord.getLevelID();
                     operation = aggregateDepthRecord.getOperation().toString();
                     
                     if (list.getAsk() != null) {
                        ask = aggregateDepthRecord.getAsk().getValue();
                        gAsk = (gAsk != ask) ? ask : gAsk;
                        orderSize = aggregateDepthRecord.getAsk().getSize();
                     }

                     if (list.getBid() != null) {
                        bid = aggregateDepthRecord.getBid().getValue();
                        gBid = (gBid != bid) ? bid : gBid;
                        orderSize = aggregateDepthRecord.getBid().getSize();
                     }

                     if (gAsk == 0 || gAsk == gLastAsk)
                        askOut = noValue;
                     else 
                        askOut = String.format("%.2f", gAsk);
                     
                     if (gBid == 0 || gBid == gLastBid)
                        bidOut = noValue;
                     else
                        bidOut = String.format("%.2f", gBid);
                        
                     if (bidOut != noValue)
                        System.out.printf("Symbol: %-10s LEVEL: %-10d BID: %-10s Size: %-10d LEVEL: %-10s ASK: %-10s Size: %-10s Operation: %-10s\n", gInstrument, level, bidOut, orderSize, noValue, askOut, noValue, operation);
                     else if (askOut != noValue)
                        System.out.printf("Symbol: %-10s LEVEL: %-10s BID: %-10s Size: %-10s LEVEL: %-10d ASK: %-10s Size: %-10d Operation: %-10s\n", gInstrument, bidOut, noValue, noValue, level, askOut, orderSize, operation);
                     gLastBid = gBid;
                     gLastAsk = gAsk;               
                     break;
                  case DEFAULT:
                     break;
               }
            }
         } else {
            System.out.println("No Depth Data available for: " + event.getSymbol());
         }
      }
   }

   public void onInfo(MarketDepthInfoEvent event) {
      if (!consoleSample.displayData())
         return;
      
      String line = "-------------------------------------------------------------------------------------------------";
      try {
          String jsonString = event.getInfo();
          
          //System.out.println(jsonString);
          
          JSONObject jsonObject = new JSONObject(jsonString);

          if (!jsonObject.getString("msgType").equals("DEPTH_INFO_NOTIFICATION")) {
              return;
          }
          
          if (consoleSample.displayData()) {
             System.out.println(line);
             System.out.printf("SYMBOL: %s\n", event.getSymbol());
             System.out.println(line);

			 if (!jsonObject.has("sources")) {
				System.out.println("No depth information available.");
				return;
			 }
			 
             JSONArray sources = jsonObject.getJSONArray("sources");
             for (int i = 0; i < sources.length(); i++) {
                 JSONObject source = sources.getJSONObject(i);
                 String sourceDesc = source.getString("desc");
                 String cleanupTime = source.has("cleanup") ? source.getString("cleanup") : "N/A";
                 System.out.printf("   %-72sClear Down Time: %s\n\n", sourceDesc, cleanupTime);

                 JSONArray types = source.getJSONArray("types");
                 for (int j = 0; j < types.length(); j++) {
                     JSONObject type = types.getJSONObject(j);
                     String depthType = type.getString("desc");
                     String entRequired = type.getString("ent_required");
                     boolean entitled = type.getBoolean("entitled");
                     boolean isDefault = type.has("default") && type.getBoolean("default");

                     System.out.printf("      Depth Type: %-30s Entitlement: %-7s Entitled: %-6s",
                             depthType, entRequired, entitled ? "Yes" : "No");

                     if (isDefault) {
                         System.out.print(" (default)");
                     }
                     System.out.println();
                 }

                 if (source.has("netorder")) {
                     System.out.println("\n      Net Order Imbalance: " + source.getString("netorder"));
                 }

                 if (i < sources.length() - 1)
                     System.out.println();
                 else
                     System.out.println(line + "\n");
             }
          }
      } catch (Exception e) {
          e.printStackTrace();
      }      
      symbolCount++;
      
      if (symbolCount == consoleSample.getSymbolCount()) {
         try {
            consoleSample.exitSample(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
   
   public void onError(MarketDepthInfoEvent marketDepthEvent) {
      System.out.println("onError during processing, returning symbol: " + marketDepthEvent.getSymbol());
      System.out.println(marketDepthEvent.getInfo());
      System.exit(-1);
   }

   public void onUpdate(NetOrderImbalanceEvent netOrderImbalanceEvent) {
   }

   public void onError(NetOrderImbalanceEvent netOrderImbalanceEvent) {
   }
}
