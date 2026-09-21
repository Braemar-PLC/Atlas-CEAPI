package NewsManager.NewsSample;

import java.util.List;

import com.esignal.jstandard.beans.news.Headline;
import com.esignal.jstandard.beans.news.Story;
import com.esignal.jstandard.event.HeadlineEvent;
import com.esignal.jstandard.event.NewsListener;
import com.esignal.jstandard.event.NewsRequestListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.StoryEvent;

public class MyConsoleNewsListener implements NewsListener, NewsRequestListener {
   
   private NewsSample consoleSample;
   private int completeCount = 0;
   private int headlineCount = 0;
   
   public  MyConsoleNewsListener(NewsSample consoleSample) {
      this.consoleSample = consoleSample;
   }
   
   // Required to implement this class, but not handled by this sample
   public void onAdded(StatusEvent statusEvent) {      
   }

   // Required to implement this class, but not handled by this sample
   public void onDeleted(StatusEvent statusEvent) {         
   }

   // This event is sent when mode is set to subscribe 
   public void onUpdate(HeadlineEvent hlEvent) {
	  if (!consoleSample.displayData())
		  return;
	  
      List<Headline> headlinesList = hlEvent.getHeadlines();
      this.displayHeadlines(headlinesList);
   }

   // Required to implement this class, but not handled by this sample
   public void onError(StatusEvent statusEvent) {         
   }

   // Required to implement this class, but not handled by this sample
   public void onRequested(StatusEvent statusEvent) {
   }

   // This event is sent when mode is set to request
   public void onResponse(HeadlineEvent hlEvent) {
      List<Headline> headlinesList = hlEvent.getHeadlines();
      this.displayHeadlines(headlinesList);
   }
   
   // This event is sent when mode is set to story
   public void onResponse(StoryEvent stEvent) {
      List<Story> stList = stEvent.getStories();
      this.displayStories(stList);
   }

   // This event is sent when a request is completed, this event is only sent when mode is set to story or request.
   public void onComplete(StatusEvent statusEvent) {
      this.completeCount++;
      try {
         // Exit the sample if complete count matches with the request count
         if (this.completeCount == this.consoleSample.getRequestCount()) {
            //if (consoleSample.getMode().equals("request"))
            //   System.out.println ("Received " + this.headlineCount + " headlines.");
            System.out.println("************************************************************************************************************************************");
            this.consoleSample.exitSample(true);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   /**
    * This function displays the elements for a list of Headline objects
    * 
    * @param hlList list of Headline objects
    */
   public void displayHeadlines (List<Headline> hlList) {
      
      if (hlList.isEmpty() && this.headlineCount == 0) {
         System.out.println ("No headlines associated.\n");
         return;         
      }         
      
      for (Headline hl : hlList) {
         this.headlineCount++;

         System.out.println("************************************************************************************************************************************");
         System.out.println("HEADLINE " + this.headlineCount + " OF " + this.consoleSample.getHeadlinesRequestedCount() + " REQUESTED\n");
         System.out.println("            STORY ID:  " + hl.getStoryId());
         System.out.println("                TIME:  " + hl.getTime()) ;
         List<String> services = hl.getServices();         
         if (services!=null && !services.isEmpty()) {
            System.out.print ("       NEWS SERVICES:  ");
            for (String service : services)
               System.out.print (service + " ");
            System.out.println("");         
         }
         System.out.println("            HEADLINE:  " + hl.getHeadlineText());
                  
         List<String> categories = hl.getCategories();         
         if (categories != null && !categories.isEmpty()) {
            System.out.print ("          CATEGORIES:  ");
            for (String category : categories)
               System.out.print (category + " ");
            System.out.println("");         
         }
            
         List<String> symbols = hl.getSymbols();         
         if (symbols != null && !symbols.isEmpty()) {
            System.out.print ("             SYMBOLS:  ");
            for (String symbol : symbols)
               System.out.print (symbol + " ");
            System.out.println("");         
         }

         String action = "";
         
         if (hl.getNewsAction() != null) {         
            switch (hl.getNewsAction())
            {
               case ACTION_NONE:
                  action = "No action";
                  break;
               case ACTION_NEW:
                  action = "New headline";
                  break;
               case ACTION_REPLACE:
                  action = "Headline replaced";
                  break;
               case ACTION_DELETE:
                  action = "Headline deleted";
                  break;
               case ACTION_EXPIRE:
                  action = "Headline expired";
                  break;
               case ACTION_REPLACE_HEADLINE:
                  action = "Replaced the headline";
                  break;
               case ACTION_REPLACE_METADATA:
                  action = "Replaced metadata associated with the news item";
                  break;
               case ACTION_REPLACE_BODY:
                  action = "Replace the body of the story";
                  break;
               case ACTION_RETRANSMIT:
                  action = "Headline re-transmitted";
                  break;
               default:
                  break;
            }
   
            if (!action.isEmpty()) 
               System.out.println ("         NEWS ACTION:  " + action);
         }
      }
   }
   
   /**
    * This function displays the elements for a list of Story objects
    * 
    * @param stList list of Story objects
    */
   public void displayStories (List<Story> stList) {
      
      if (stList.isEmpty()) {
         System.out.println ("No story associated with StoryID provided.\n");
         return;         
      }   
      
      for (Story st : stList) {
         
         System.out.println("            STORY ID:  " + st.getStoryId());
         System.out.println("                TIME:  " + st.getTime()) ;         
         List<String> services = st.getServices();         
         if (services!=null && !services.isEmpty()) {
            System.out.print ("       NEWS SERVICES:  ");
            for (String service : services)
               System.out.print (service + " ");
            System.out.println("");         
         }
         System.out.println("            HEADLINE:  " + st.getHeadlineText());      
         
         List<String> categories = st.getCategories();         
         if (categories != null && !categories.isEmpty()) {
            System.out.print ("          CATEGORIES:  ");
            for (String category : categories)
               System.out.print (category + " ");
            System.out.println("");         
         }
            
         List<String> symbols = st.getSymbols();         
         if (symbols != null && !symbols.isEmpty()) {
            System.out.print ("             SYMBOLS:  ");
            for (String symbol : symbols)
               System.out.print (symbol + " ");
            System.out.println("");         
         }
         
         System.out.println();         
         System.out.println("   STORY TEXT FORMAT:  " + st.getStoryType().toString());         
         System.out.println("          STORY TEXT:  " + st.getStoryBody() + "\n");            
      }      
   }
}
