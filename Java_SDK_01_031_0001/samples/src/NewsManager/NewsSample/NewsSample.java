package NewsManager.NewsSample;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.news.DefaultNewsRequest;
import com.esignal.jstandard.beans.news.DefaultStoryRequest;
import com.esignal.jstandard.beans.news.NewsRequest;
import com.esignal.jstandard.beans.news.StoryRequest;
import com.esignal.jstandard.beans.news.NewsRequest.NEWS_SEARCH_FIELDS;
import com.esignal.jstandard.beans.news.NewsRequest.NEWS_SORT_ORDER;
import com.esignal.jstandard.beans.news.StoryRequest.NEWS_STORY_FORMAT;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.NewsListener;
import com.esignal.jstandard.event.NewsRequestListener;
import com.esignal.jstandard.exception.ResourceManagerException;
import com.esignal.jstandard.managers.NewsManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.esignal.jstandard.managers.UsernameInfo;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI;
import com.esignal.jstandard.managers.dbc.DbcCodes.DBCAPI_ERROR;

import Util.SampleProperties;
import Util.SampleRunner;

/**
 * Provides methods for requesting and processing news headlines and stories.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *
 */
public class NewsSample {
   
   // Variables to collect symbols, credentials, server info and other relevant parameters 
   // from command line for connection to data farm, and to track request completion.

   private String host = "";
   private SockType connType;
   private String username = "";
   private String password = "";
   private String requestType = "";
   private String services = "";
   private String storyID = "";
   private String[] symbols = null;
   private String storyFormat = "";
   private String categories = "";
   private String headlineCount = "";
   private String beginTime = "";
   private String endTime = "";
   private String headlineDays = "";
   private String allActions = "";
   private String searchText = "";
   private String searchFields = "";
   private String isPhraseSearch = "";
   private String isAllWordsSearch = "";
   private String isFullTextSearch = "";
   private String isWholeWordSearch = "";
   private String isMatchCaseSearch = "";
   private String sortOrder = "";
   private int requestCount = 0;
   private String limit = "";
   private boolean displayOutput = true;
   
   private long turnAroundValue = 1000;
   
   // Declare newsManager
   private NewsManager newsManager;
   
   // Declare newsListener
   private NewsListener newsListener;
   
   // Declare instance of properties file
   Properties sampleProps = SampleProperties.getInstance().getProperties();
   
   /**
    * The constructor for the NewsSample.  Receives command line arguments for performing News search and Story requests.
    * 
    * @param <host>                      Specifies the address to use to connect to the ICE Data Services network.  
    *                                    For example: cm*.dataservices.theice.com
    * @param <connetionType>             Specifies the type of connection to use with the ICE Data Services
    *                                    network.  Valid values include:
    *                             
    *                                       0 - SOCKTYPE_LEGACY    Uses a non TLS connection.
    *                                       1 - SOCKTYPE_TLS       Uses a secure/encrypted socket connection.
    *                                       2 - SOCKTYPE_TLS_PLUS  Not yet implemented; reserved for future support.
    *                             
    * @param <username>                  The username for the account entitled to receive data from ICE Data Services.
    *                                    If you do not have one, please contact your ICE Data Services representative.
    * @param <password>                  The password for the account entitled to receive data from ICE Data Services.
    *                                    If you do not have one, please contact your ICE Data Services representative.
    * @param <requestType>               Specifies what kind of news request to make. 
    *                                    Valid values include:
    *                                    
    *                                       - request
    *                                         This is a non-streaming request for headlines that meet the search criteria. 
    *                                         One of the fields returned is the StoryID. The StoryID can then be specified to request the 
    *                                         StoryBody using the "story" request type in a subsequent request.
    *                                    
    *                                       - subscribe
    *                                         This is a streaming request for headlines that meet the search criteria.
    *                                         One of the fields returned is the StoryID. The StoryID can then be specified to request the 
    *                                         StoryBody using the "story" request type in a subsequent request.
    *                                    
    *                                       - story
    *                                         This is a request for a story associated with a story ID.
    *                                                             
    * @param <services>                  Comma separated list of news services. News services are specified by the ICE
    *                                    Data Services two-letter service code for each news service.  For example:
    *                                    
    *                                       -ns "CT,PR,N1,PC"  
    *                                 
    * @param <symbolsOrStoryID>          Comma separated list of symbols or a single story ID for which to make a request. 
    *                                    Multiple symbols must be separated by commas; if one or more symbols contain a space the 
    *                                    list must be enclosed in quotes. For example:
    *                                                                              
    *                                       a. IBM,GE,WFM
    *                                       b. "ES #F,GE,WFM"
    *                                    
    *                                    note: You cannot mix symbols and story IDs.
    *                                          You cannot submit multiple story IDs in a single request.
    *                                          
    * @param <storyFormat>               The format the News Service should provide for the requested story.  Valid formats include:
    *                         
    *                                       - url
    *                                         The response includes a URL address to the story.
    *                                         
    *                                       - html
    *                                         The response is an HTML document provided as a string.
    *                                      
    * @param <categories>                A single word, or multiple words enclosed in quotes and separated by commas that identify
    *                                    vendor categories, products, topics, language, region, etc.
    *                                    For example: financials
    *                                    
    * @param <headlineStoryCount>        The number of headlines or stories to return for a search request, regardless of the 
    *                                    total number available.
    *                                    
    * @param <beginTime>                 The start of the time range in which to conduct the news search.  The date format is as follows:
    *                      
    *                                       yyyy/MM/dd HH:mm:ss
    *                                    
    *                                    Setting this value to 0 indicates to start the search as far back as possible; forever.
    *                                    
    * @param <endTime>                   The end of the time range in which to conduct the news search.  The date format is as follows:
    *                      
    *                                       yyyy/MM/dd HH:mm:ss
    *                                       
    *                                    Setting this value to 0 indicates to use the current time as the end time for the search.
    *                                    
    * @param <days>                      The number of days to search from the current day moving backward.
    *                  
    * @param <allActions>                Valid values include:
    *                                    - 0 = Send only new headlines (default)
    *                                    - 1 = Send all headline update actions
    *                                    
    * @param <searchText>                A single word, set of words, or phrase to search.  Multiple words should be separated by commas.
    *                                    Multiple words and phrases should be contained in quotes.
    *                                    
    * @param <searchFields>              The fields in which to search for the instruments, words, phrases, etc.
    *                                    Search fields include:
    *                                    
    *                                    - headline
    *                                    - story
    *                                    - category
    *                                    - symbol
    *                                    - all
    *                                    
    * @param <isPhrase>                  Valid values include:
    *                                    - 0 = The search text is not a phrase (default)
    *                                    - 1 = The search text is a phrase
    *                                    
    * @param <allWords>                  Valid values include:
    *                                    - 0 = Not required to match all words (default)
    *                                    - 1 = Require all words specified
    *                                                                
    * @param <fullText>                  Valid values include:
    *                                    - 0 = Do not perform a full-text search (default)
    *                                    - 1 = The search is a full-text search
    *                                                                
    * @param <wholeWord>                 Valid values include:
    *                                    - 0 = Allow partial word matches (default)
    *                                    - 1 = Require whole word matches
    *                                    
    * @param <matchCase>                 Valid values include:
    *                                    - 0 = The search is case-insensitive (default)
    *                                    - 1 = The search is case-sensitive
    *                                    
    *                                    The default is to search the headline field.
    *                                    
    * @param <sortOrder>                 The order in which to return the news search results. Valid values include:
    *                                    - 0 = time descending (default)
    *                                    - 1 = time ascending
    *                                    - 2 = relevance score
    *                        
    * @param <timeLimit>                 The duration this sample will run before exiting.
    *                                    Time duration applies to subscribe requests only.
    *                                    Syntax for setting the time duration is: 
    *                                              
    *                                       HH:MM:SS
    *                                                 
    *                                    Setting the limit to 0 or 00:00:00 indicates to run
    *                                    continuously without end; sample application must be
    *                                    forcibly stopped.
    *
    * @param <display>                   Specifies the type of output to produce. 
    *                                    True indicates format for console display; False indicates format for csv file.
    * @throws Exception 
    */
   public NewsSample(String host, SockType connType, String username, String password, String requestType, String services, 
                 String storyID, String[] symbols, String storyFormat, String categories, String headlineCount, 
                 String beginTime, String endTime, String headlineDays, String allActions, String searchText,
                 String searchFields, String isPhraseSearch, String isAllWordsSearch, String isFullTextSearch,
                 String isWholeWordSearch, String isMatchCaseSearch, String sortOrder, String limit, boolean display)
   {   
      try {
         this.host = host;
         this.connType = connType;
         this.username = username;
         this.password = password;
         this.requestType = requestType;
         this.services = services;
         this.storyID = storyID;
         this.symbols = symbols;
         this.storyFormat = storyFormat;
         this.categories = categories;
         this.headlineCount = headlineCount;
         this.beginTime = beginTime;
         this.endTime = endTime;
         this.headlineDays = headlineDays;
         this.allActions = allActions;
         this.searchText = searchText;
         this.searchFields = searchFields;
         this.isPhraseSearch = isPhraseSearch;
         this.isAllWordsSearch = isAllWordsSearch;
         this.isFullTextSearch = isFullTextSearch;
         this.isWholeWordSearch = isWholeWordSearch;
         this.isMatchCaseSearch = isMatchCaseSearch;
         this.sortOrder = sortOrder;
         this.limit = limit;
         this.displayOutput = display;
      } catch (Exception e) {
          SampleProperties.getInstance().displayHelp(true);
          System.exit(-1);
       }
   }
   
   /**
    * Creates an instance of a NewsManager factory and a connection
    * listener. Connects to the farm using connection settings retrieved from
    * the command line via the getConnectionSettings() function.
    * 
    * @param factory
    */
   public void runNewsSample(ResourceManagerFactory factory) throws Exception {
      
      try {
         // Check if the client will use Amazon Web Services
         if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useAWS"))) {                
            UsernameInfo info = factory.acquireUsername(this.getConnectionSettings());
             
            if (info.getStatusEnum() == DBCAPI.SUCCESS ) {
               this.username = info.getUsername();
               this.password = info.getPassword();                            
               System.out.println("account acquired . . . ");                              
            }
            else {
               DBCAPI_ERROR errorCode = DBCAPI_ERROR.valueOf(info.getStatusEnum().get().name());
               
               switch (errorCode) {
                  case INVALIDNAME:
                     System.out.println("\nThe host specified is not valid.  Verify the host value and try again.");
                     exitSample(false);
                     break;
                  case NOT_ENTITLED:
                     System.out.println("\nThe account specified is not entitled for the AWS service.");
                     exitSample(false);
                     break;
                  case WRONG_USERNAMEPASSWORD:
                     System.out.println("\nThe account username and/or password is not recognized.");
                     exitSample(false);
                     break;
                  default:
                     System.out.println("\nAn error occurred when attempting to acquire the username:  " + info.getStatusEnum());
                     exitSample(false);
                     break;
               }
            }
         }
         
         ConnectionListener consoleConnectionListener = new MyConsoleConnectionListener(this);
         this.newsManager = factory.createNewsManager();         
         this.newsManager.connect(getConnectionSettings(), consoleConnectionListener);
      } catch (Exception e) {
          System.err.println("An error occurred while running the News Sample: " + e.getMessage());
          e.printStackTrace();
      }
   }
   
   /**
    * This function creates a StoryRequest object based upon the values received from the user and/or properties file.
    * The StoryRequest object is then returned.
    * 
    * @return req StoryRequest object
    */
   private StoryRequest buildStoryRequest(){
      ++this.turnAroundValue;
      
      StoryRequest req = new DefaultStoryRequest(turnAroundValue);
      req.setStoryID (this.storyID);
      
      if (this.storyFormat != null )
         req.setStoryFormat(NEWS_STORY_FORMAT.getNewsStoryFormat(Integer.parseInt(this.storyFormat)));
      return req;      
   }
   
   /**
    * This function creates a DefaultNewsRequest object based upon the values received from the user and/or properties file.
    * The DefaultNewsRequest object is then returned.
    * 
    * @return req NewsRequest object
    */
   private NewsRequest buildNewsRequest(){
      ++this.turnAroundValue;
      
      NewsRequest req = new DefaultNewsRequest(turnAroundValue);
      
      req.setSubscribeAllActions(this.allActions != null && this.allActions.equalsIgnoreCase("true") ? true :false);            
      req.setHeadlineCount (this.headlineCount != null && Integer.parseInt(this.headlineCount) > 0 ? Integer.parseInt(this.headlineCount):0);
      
      if (this.sortOrder != null)
         req.setSortOrder(NEWS_SORT_ORDER.getNewsSortOrder(Integer.parseInt(this.sortOrder)));

      // convert string list with spaces
      if (this.symbols != null)
    	  req.setSymbols(new ArrayList<String>(Arrays.asList(this.symbols)));
         //req.setSymbols(new ArrayList<String>(Arrays.asList(this.symbols.split("\\,"))));
      
      if (this.categories != null)
         req.setCategories(new ArrayList<String>(Arrays.asList(this.categories.split("\\,"))));
      
      if (this.services != null)
         req.setServices(new ArrayList<String>(Arrays.asList(this.services.split("\\,"))));

      req.setSearch(this.searchText);
      
      if (this.searchFields != null)
         req.setSearchFields(NEWS_SEARCH_FIELDS.getSearchField(Integer.parseInt(this.searchFields)));
      
      req.setMatchAllWords(this.isAllWordsSearch   != null && this.isAllWordsSearch.equalsIgnoreCase("true")  ? true :false);
      req.setPhraseQuery(this.isPhraseSearch       != null && this.isPhraseSearch.equalsIgnoreCase("true")    ? true :false);
      req.setFullTextQuery(this.isFullTextSearch   != null && this.isFullTextSearch.equalsIgnoreCase("true")  ? true :false);
      req.setMatchWholeWord(this.isWholeWordSearch != null && this.isWholeWordSearch.equalsIgnoreCase("true") ? true :false);
      req.setMatchCase(this.isMatchCaseSearch      != null && this.isMatchCaseSearch.equalsIgnoreCase("true") ? true :false);      
            
      SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      
      if (this.beginTime != null && this.beginTime.length() > 0) {
         if (beginTime.equals("0"))
            req.setBeginTime(0);
         else {
            try {
               req.setBeginTime(format.parse(beginTime).getTime());
            } 
            catch (ParseException e1) { 
               e1.printStackTrace();
            }
         }
      }
      
      if (this.endTime != null && this.endTime.length() > 0 ) {
         if (endTime.equals("0"))
            req.setEndTime(0);
         else {
            try {
               req.setEndTime(format.parse(endTime).getTime());
            }
            catch (ParseException e2) {
               e2.printStackTrace();
            }
         }
      }
      
      req.setHeadlineDays(this.headlineDays != null && Integer.parseInt(this.headlineDays) > 0 ?   Integer.parseInt(this.headlineDays):0 );
       
      return req;   
   }
   
   /**
    * This function determines the mode that the NewsSample is running in and calls
    * the appropriate method of the NewsManager
    * 
    * @throws ResourceManagerException
    */
   public void getData () throws ResourceManagerException {
      this.newsListener = new MyConsoleNewsListener(this);         

      NewsRequest newsREQ = buildNewsRequest();
      StoryRequest storyREQ = buildStoryRequest();
      
      if (this.requestType != null && this.requestType.equalsIgnoreCase("request")){    
         this.newsManager.request(newsREQ, (NewsRequestListener) this.newsListener);
         this.requestCount++;
      } else if (this.requestType!= null && this.requestType.equalsIgnoreCase("subscribe")) {
         this.newsManager.subscribe(newsREQ, this.newsListener);
         this.requestCount++;
         // Run continuously?
         if (!this.limit.equalsIgnoreCase("0"))
           SampleRunner.runSampleForTime(this.limit, this);
      } else if (this.requestType!= null && this.requestType.equalsIgnoreCase("story")) {
         this.newsManager.getStory(storyREQ, (NewsRequestListener) this.newsListener);
         this.requestCount++;
      }
   }
   
   /**
    * This function calls the disconnect method of the NewsManager class
    * 
    */
   public void exitSample (boolean disconnect) throws Exception {
      if (disconnect) {    	  
    	 this.newsManager.unsubscribe(this.turnAroundValue, this.newsListener);
    	 Thread.sleep(2000);    	 
    	 
    	 if (this.displayOutput) {
	         System.out.println("");
	         System.out.println("Exiting application...");
	         System.out.println("(press Enter to exit)");
	         System.in.read();  
    	 }
         try {
        	this.newsManager.disconnect();
         } catch (ResourceManagerException rme) {
        	  rme.printStackTrace();
         }
      }
      System.exit(0);
   }   

   public boolean displayData() {
   	  return displayOutput;
   }

   /**
    * This function returns the requestCount member of the NewsSample class
    * 
    * @return this.requestCount
    */
   public int getRequestCount () {
      return this.requestCount;
   }
   
   /**
    * This function returns the int value of the headlineCount member in the NewsSample class
    * 
    * @return Integer.parseInt(this.headlineCount)
    */
   public int getHeadlinesRequestedCount () {
      return Integer.parseInt(this.headlineCount);
   }

   /**
    * This function returns the mode member of the NewsSample class
    * 
    * @return this.mode
    */
   public String getMode () {
      return this.requestType;
   }
   
   /**
    * Retrieves connection settings from either the command line or properties
    * file, and returns to the calling function.
    * 
    * @return connectionSettings
    */
   private ConnectionSettings getConnectionSettings() {
      ConnectionSettings connectionSettings = new ConnectionSettings(host, username, password, connType);
   
      // If samples.connection.useproxy in NewsSample.properties is set
      // to true, collect proxy settings
      if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useproxy"))) {
         String proxyusername="";
         String proxypassword="";
         connectionSettings.setProxyInfo(sampleProps.getProperty("samples.connection.proxyhost"),
                        Integer.parseInt(sampleProps.getProperty("samples.connection.proxyport")),
                        proxyusername, proxypassword);
        }
        
        // Connection Timeout Override
        // ===========================
        // The API allows up to 30 seconds to establish a connection before timing out
        // and making another attempt.  This default can be overridden using a property
        // setting from the jstandard.properties file, or by using the setConnectionTimeout
        // method.
        //
        // If the jstandard.DBC.timeouts.connection property is set in the jstandard.properties
        // file, the API will automatically retrieve the value and apply it to all connections.
        //
        // If the setConnectionTimeout method is used, it will take precedence over any value
        // provided in the jstandard.properties file.
        // 
        // The following code demonstrates how to set a timeout value using the setConnectionTimeout
        // method:
        
        //connectionSettings.setConnectionTimeout(30000);
        
        // note:  The above value of 30000 is the same as the default value.  This value can be
        //        changed.  The minimum value is 1000.
        
        // IMPORTANT: Use of the setConnectionTimeout method affects ALL connections (new and existing)
        //            including existing connections that experience a reconnection.  
        //            This is a *global* setting and affects all connections regardless of
        //            manager type.

      return connectionSettings;
   }
}
