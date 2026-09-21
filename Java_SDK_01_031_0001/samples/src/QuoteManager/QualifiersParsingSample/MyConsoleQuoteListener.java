package QuoteManager.QualifiersParsingSample;

import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.esignal.jstandard.beans.quote.CidData;
import com.esignal.jstandard.beans.quote.CidFieldData;
import com.esignal.jstandard.beans.quote.FIELDFORMAT;
import com.esignal.jstandard.beans.quote.MSTATUS;
import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.beans.quote.Status;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.QuoteListener;
import com.esignal.jstandard.event.QuoteRequestListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.dbc.DbcCodes;

import Util.SampleUtilities;

public class MyConsoleQuoteListener implements QuoteRequestListener, QuoteListener {

    // Reference to the QualifiersParsingSample object for accessing specific functions
    private final QualifiersParsingSample consoleSample;

    // SampleUtilities instance
    private final SampleUtilities utilities = SampleUtilities.getInstance();
    
    // Instrument & update details
    private String instrument;
    private String displayKey;
    private String companyName;
    private String listedExchange;
    private String categoryName;
    private String subcategoryName;
    private char[] updateStatus;
    private Long marketStatus;
    String[] marketUpdateInfo;

    // Common pricing details
    private static class PricingDetails {
        double open, high, low, close, ask, bid, last, settlement, mid;
        double accumVolume, bidSize, askSize, lastSize;
        int bidLineID, askLineID, lastLineID;
        short bidSeries, askSeries, lastSeries;
        BigInteger bidSeqNum, askSeqNum;
		BigInteger lastSeqNum;
        Instant bidExgTime, askExgTime;
		Instant lastExgTime;
		Instant highTime;
        String bidExg, askExg, lastExg;
        String bidQuals, askQuals, lastQuals, mphaseQuals, securityStats;
        short[] lastQualifiers, bidQualifiers, askQualifiers, mphaseQualifiers, securityStatus;

        /**
         * Resets all pricing details to their default values.
         */
        void reset() {
            open = high = low = close = ask = bid = last = settlement = mid = 0;
            accumVolume = bidSize = askSize = lastSize = 0;
            bidLineID = askLineID = lastLineID = 0;
            bidSeries = askSeries = lastSeries = 0;
            bidSeqNum = askSeqNum = lastSeqNum = BigInteger.ZERO;
            bidExgTime = askExgTime = lastExgTime = highTime = Instant.EPOCH;
            bidExg = askExg = lastExg = "----";
            bidQuals = askQuals = lastQuals = mphaseQuals = securityStats = "----";
            lastQualifiers = new short[4];
            bidQualifiers = new short[4];
            askQualifiers = new short[4];
            mphaseQualifiers = new short[4];
            securityStatus = new short[4];
        }

        /**
         * Initializes the qualifiers arrays with the given array.
         *
         * @param array The array to copy values from.
         */
        void initializeQualifiers(short[] array) {
            lastQualifiers = Arrays.copyOf(array, 4);
            bidQualifiers = Arrays.copyOf(array, 4);
            askQualifiers = Arrays.copyOf(array, 4);
            mphaseQualifiers = Arrays.copyOf(array, 4);
            securityStatus = Arrays.copyOf(array, 4);
        }
    }

    private final PricingDetails currentPricing = new PricingDetails();
    private final PricingDetails pastPricing = new PricingDetails();

    // CID update details
    private static class CIDDetails {
        short[] lastQualifiers, bidQualifiers, askQualifiers, securityStatus;

        /**
         * Resets all CID details to their default values.
         */
        void reset() {
            lastQualifiers = new short[4];
            bidQualifiers = new short[4];
            askQualifiers = new short[4];
            securityStatus = new short[4];
        }

        /**
         * Initializes the qualifiers arrays with the given array.
         *
         * @param array The array to copy values from.
         */
        void initializeQualifiers(short[] array) {
            lastQualifiers = Arrays.copyOf(array, 4);
            bidQualifiers = Arrays.copyOf(array, 4);
            askQualifiers = Arrays.copyOf(array, 4);
            securityStatus = Arrays.copyOf(array, 4);
        }
    }

    private final CIDDetails cidDetails = new CIDDetails();

	// ///////////////////////////////////////////////////////////////////////////////
	// YES Energy details
    private static class yesEnergyDetails {
        double volume;
        double dayAheadLMP, dayAheadCongestion, dayAheadLoss;
        double rtPricesLMP, rtPricesCongestion, rtPricesLoss;
        double rtLoad, dayAheadLoad, rtFinalLoad;
        double rtLMP, rtCongestion, rtLoss;
        double hourAheadLMP, hourAheadCongestion, hourAheadLoss;
        double load;
        double rtSysPrice;
        double rt15mPricesLMP, rt15mPricesCongestion, rt15mPricesLoss;
        BigInteger intervalStart, intervalEnd, intervalStartDARTPrice, intervalEndDARTPrice;
        BigInteger intervalStartRT15mPrices, intervalEndRT15mPrices;
        BigInteger intervalStartDARTLoads, intervalEndDARTLoads;
        BigInteger intervalStartRTLoads, intervalEndRTLoads;
        int intervalTypeRT15mPrices, intervalTypeDARTPrices, intervalTypeDARTLoads, intervalTypeRTLoads;
        String rtFinalDARTPrice;
        String utcOffset;

        /**
         * Resets all LRT type details to their default values.
         */
        void reset() {
            volume = 0;
            dayAheadLMP = dayAheadCongestion = dayAheadLoss = 0;
            rtPricesLMP = rtPricesCongestion = rtPricesLoss = 0;
            rtLoad = dayAheadLoad = rtFinalLoad = 0;
            rtLMP = rtCongestion = rtLoss = 0;
            hourAheadLMP = hourAheadCongestion = hourAheadLoss = 0;
            load = 0;
            rtSysPrice = 0;
            rt15mPricesLMP = rt15mPricesCongestion = rt15mPricesLoss = 0;
            intervalStart = intervalEnd = intervalStartDARTPrice = intervalEndDARTPrice = BigInteger.ZERO;
            intervalStartRT15mPrices = intervalEndRT15mPrices = BigInteger.ZERO;
            intervalStartDARTLoads = intervalEndDARTLoads = BigInteger.ZERO;
            intervalStartRTLoads = intervalEndRTLoads = BigInteger.ZERO;
            intervalTypeRT15mPrices = intervalTypeDARTPrices = intervalTypeDARTLoads = intervalTypeRTLoads = 0;
            rtFinalDARTPrice = "----";
            utcOffset = "----";
        }
    }

    private final yesEnergyDetails yesEnergy = new yesEnergyDetails();
    
    // Unknown Fields Details
	private static class unknownFieldsDetails {
		String fieldName = "----";
		int fieldID = -1;
		String fieldFormat = "----";
		Object fieldValues;
		String categoryName = "----";
		String subcategoryName = "----";
		
		public unknownFieldsDetails(String fieldName, short id, String fieldFormat, Object value, String category, String subcategory) {
			this.fieldName = fieldName;
			this.fieldID = id;
			this.fieldFormat = fieldFormat;
			this.fieldValues = value;
			this.categoryName = category;
			this.subcategoryName = subcategory;			
		}
	}
	
	private final Vector<unknownFieldsDetails> unknownFields = new Vector<>();
    
    private boolean isCIDUpdate;
    private String cidType; // Correction, Insertion, Deletion, Unknown

    // Additional fields
    private String fieldName;
    private String fieldFormat;
    private String currency;

    // Counts number of instruments (requests) that returned NO_DATA or resulted in some
    // type of error that resulted in no records returned.
    private int nothingReturned;

    // Output formatting
    private boolean columnsPrepared;
    private boolean columnsPrinted;
    private String[] columnHeadings = new String[50];
    
    private boolean unknownColumnsPrepared;
    private boolean unknownColumnsPrinted;
    private String[] unknownColumnHeadings = new String[4];

    // Qualifiers parsing
    // The following members are used to load and parse the "qualifiers.tab" file.
    // We need to load this file into an XML parser to retrieve the data associated with any
    // qualifier information we receive.
    // This information provides more details about the data.
    private static final int EXCHANGE_THRESHOLD = 191;
    private Document gDoc;
    private NodeList gQualifierList;
    private NodeList gSecurityStatusList;
    
    private ArrayList<Integer> qualifierFilters;
   
    /**
     * Constructor for MyConsoleQuoteListener.
     *
     * @param consoleSample The QualifiersParsingSample object for accessing specific functions.
     */
    public MyConsoleQuoteListener(QualifiersParsingSample consoleSample) {
        this.consoleSample = consoleSample;
        loadQualifiersAndSecurityStatus();
        this.qualifierFilters = consoleSample.getQualifierFilters();

        if (!consoleSample.displayData()) {
            createColumnHeadings();
        }
    }

    /**
     * Triggered when a symbol is added to the subscription list.
     * 
     * <p>This method is required to implement the interface but is not
     * handled in this sample.</p>
     *
     * @param event The SymbolEvent object containing details of the added symbol.
     */
    @Override
    public void onAdded(SymbolEvent event) {
        // Method intentionally left unimplemented
    }

    /**
     * Triggered when a symbol is deleted from the subscription list.
     *
     * <p>This method is required to implement the interface but is not
     * handled in this sample.</p>
     *
     * @param event The SymbolEvent object containing details of the deleted symbol.
     */
    @Override
    public void onDeleted(SymbolEvent event) {
        System.out.println("UNSUBSCRIBED: " + event.getSymbol());
    }

    /**
     * Triggered when an error occurs related to a subscription.
     *
     * @param event The StatusEvent object containing details of the error.
     */
    @Override
    public void onError(StatusEvent event) {
        System.out.println(event.getStatusString());
    }

    /**
     * Triggered when an error occurs for a specific symbol.
     *
     * <p>This method is triggered when an error occurs while handling a symbol-specific request.
     * It logs detailed information about the error, including the symbol, turnaround, and
     * status details, to the console. If applicable, it increments the count of requests
     * with no records and updates the associated metrics in `consoleSample`.</p>
     *
     * @param event The `SymbolEvent` containing details about the error.
     */
    @Override
    public void onError(SymbolEvent event) {
        // Display error details regardless of the display setting
        System.out.println("\nERROR:");
        System.out.println("       Symbol: " + event.getSymbol());
        System.out.println("   Turnaround: " + event.getTurnAround());
        System.out.println("       Source: " + event.getSource());
        System.out.println("  Status Code: " + event.getStatusCode());
        System.out.println("  Status Type: " + event.getStatusType());
        System.out.println("Status String: " + event.getStatusString() + "\n");

        // Handle specific error statuses
        System.out.println("DETAILS:");
        if (event.getStatus() == DbcCodes.eSTATUS_NO_DATA.NOSERVERDATA || 
            event.getStatus() == DbcCodes.eSTATUS_NO_DATA.NODATA) {
            System.out.println("   Could not process with the error Status: " + event.getStatus().name());
            System.out.println("   The instrument entered ( " + event.getSymbol() + " ) may be valid, but no data is available.");
        }

        if (event.getStatus() == DbcCodes.eSTATUS_NO_DATA.NOTENT) {
            System.out.println("   Could not process with the error Status: " + event.getStatus().name());
            System.out.println("   Your account is not entitled for the symbol: " + event.getSymbol());
        }

        System.out.println();

        // Log "NO DATA" and increment the count of failed requests
        System.out.println(event.getSymbol() + ",NO DATA");
        ++nothingReturned;
        consoleSample.setNumberRequestsWithNoRecords(nothingReturned);
    }

    /**
     * Triggered when a quote update event occurs.
     *
     * <p>This method processes the updated quote data by invoking the
     * {@code processData} method.</p>
     *
     * @param event The QuoteEvent object containing the updated quote information.
     */
    @Override
    public void onUpdate(QuoteEvent event) {
        Quote quote = event.getQuote();
        processData(quote);
    }

    /**
     * Triggered when a symbol subscription is complete.
     *
     * @param event The SymbolEvent object indicating the subscription completion.
     */
    @Override
    public void onComplete(SymbolEvent event) {
        consoleSample.setDataComplete();
    }

    /**
     * Triggered when a symbol subscription is requested.
     *
     * <p>This method is required to implement the interface but is not
     * handled in this sample.</p>
     *
     * @param event The SymbolEvent object containing details of the requested symbol.
     */
    @Override
    public void onRequested(SymbolEvent event) {
        // Method intentionally left unimplemented
    }

    /**
     * Triggered when a quote response event occurs.
     *
     * <p>This method processes the response data by invoking the {@code processData} method.</p>
     *
     * @param event The QuoteEvent object containing the quote response information.
     */
    @Override
    public void onResponse(QuoteEvent event) {
        Quote quote = event.getQuote();
        processData(quote);
    }


    /**
     * Loads and parses the "qualifiers.tab" file to retrieve qualifier and security status data.
     */
    private void loadQualifiersAndSecurityStatus() {
        if (consoleSample.displayData()) {
            System.out.printf("Loading qualifiers information...");
        }

        // Loading the qualifiers.tab file
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            gDoc = dBuilder.parse(new URL("http://fs2.esignal.com/qualifiers.tab").openStream());
            gDoc.getDocumentElement().normalize();
            gQualifierList = gDoc.getElementsByTagName("q");
            gSecurityStatusList = gDoc.getElementsByTagName("securityStatus");

            if (consoleSample.displayData()) {
                System.out.println("loaded.\n");
            }
        } catch (Exception e) {
            System.out.println("Could not load qualifiers information.");
            System.out.println(e + "\n");
        }
    }

    /**
     * Retrieves human-readable data for a given qualifier ID and exchange.
     *
     * @param id  The qualifier ID to search for.
     * @param exg The exchange associated with the qualifier (required for IDs above 191).
     * @return A string formatted as "[id:description]" or "[id:No qualifier data]" if no match is found.
     */
    public String getQualifierData(short id, String exg) {
        if (gQualifierList == null || gQualifierList.getLength() == 0) {
            return "[" + id + ":No qualifier data]";
        }

        for (int i = 0; i < gQualifierList.getLength(); i++) {
            Node node = gQualifierList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (isMatchingQualifier(element, id, exg)) {
                    String description = sanitizeQualifierDescription(element.getAttribute("lname"));
                    return "[" + id + ":" + description + "]";
                }
            }
        }
        return "[" + id + ":No qualifier data]";
    }

    /**
     * Retrieves human-readable data for an array of qualifier IDs and an exchange.
     *
     * @param ids An array of qualifier IDs to search for.
     * @param exg The exchange associated with the qualifiers (required for IDs above 191).
     * @return A string formatted as "[id:description] | [id:description]" for all IDs.
     */
    public String getQualifierData(short[] ids, String exg) {
        if (gQualifierList == null || gQualifierList.getLength() == 0) {
            return IntStream.range(0, ids.length)
                            .mapToObj(i -> "[" + ids[i] + ":No qualifier data]")
                            .collect(Collectors.joining(" | "));
        }

        StringBuilder results = new StringBuilder();
        for (short id : ids) {
            if (results.length() > 0) {
                results.append(" | ");
            }
            results.append(getQualifierData(id, exg));
        }
        return results.toString();
    }

    /**
     * Checks whether a given XML element matches the specified qualifier ID and exchange.
     *
     * @param element the XML `Element` representing a qualifier entry.
     * @param id      the qualifier ID to match.
     * @param exg     the exchange to match (required for IDs above 191).
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
     * @param description the raw description to sanitize.
     * @return the sanitized description.
     */
    private String sanitizeQualifierDescription(String description) {
        return description.replace(",", "");
    }

    /**
     * Sanitizes an array of qualifier IDs for CSV compatibility.
     *
     * @param qualifiers the set of qualifier IDs to sanitize.
     * @return the sanitized qualifier IDs as a String.
     */
    private String sanitizeQualifiers(short[] qualifiers) {
        if (qualifiers == null) return "";
        return Arrays.toString(qualifiers).replace(",", "|");
    }

    /**
     * Determines whether a tick record passes the qualifier filter.
     *
     * @param qualifiers the array of qualifier values to check.
     * @return `true` if the record passes the filter; `false` otherwise.
     */
    private boolean passesQualifierFilter(short[] qualifiers) {
        List<Integer> qualifierFilters = consoleSample.getQualifierFilters();

        if (qualifierFilters.isEmpty()) {
            return true;
        }

        for (short qualifier : qualifiers) {
            if (qualifierFilters.contains((int) qualifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates and prints the column headings for output based on defined fields.
     */
    private void createColumnHeadings() {
        // Construct column headings based on all defined fields; these are the fields handled by this sample.
        String[] headings = {
            "CURRENT TIME (EPOCH IN MS)",
            "CURRENT LOCAL TIME",
            "UTC OFFSET (MINUTES)",
            "MARKET STATE",
            "MARKET PHASE",
            "STATUS",
            "FIELD NAME",
            "FIELD FORMAT",
            "CATEGORY",
            "SUBCATEGORY",
            "INSTRUMENT",
            "DISPLAY KEY or FIELD VALUES",
            "CID",
            "CID TYPE",
            "SECURITY QUALIFIERS",
            "COMPANY NAME",
            "OPEN",
            "HIGH",
            "LOW",
            "CLOSE",
            "TRADE EXCHANGE TIME (UTC)",
            "TRADE TIME (LOCAL)",
            "TRADE QUALIFIERS",
            "CURRENCY",
            "ACCUMULATED VOLUME",
            "TRADE PRICE",
            "TRADE SIZE",
            "TRADE EXCHANGE",
            "TRADE SERIES",
            "TRADE SEQ NUM",
            "TRADE LINE ID",
            "BID EXCHANGE TIME (UTC)",
            "BID TIME (LOCAL)",
            "BID QUALIFIERS",
            "BID PRICE",
            "BID SIZE",
            "BID EXCHANGE",
            "BID SERIES",
            "BID SEQ NUM",
            "BID LINE ID",
            "ASK EXCHANGE TIME (UTC)",
            "ASK TIME (LOCAL)",
            "ASK QUALIFIERS",
            "ASK PRICE",
            "ASK SIZE",
            "ASK EXCHANGE",
            "ASK SERIES",
            "ASK SEQ NUM",
            "ASK LINE ID",
        	"SETTLE",
        	"MID",
        	"VOLUME",
        	"DAY AHEAD LMP",
        	"DAY AHEAD CONGESTION",
        	"DAY AHEAD LOSS",
        	"RT PRICES LMP",
        	"RT PRICES CONGESTION",
        	"RT PRICES LOSS",
        	"RT SYS PRICE",
        	"RT LOAD",
        	"DAY AHEAD LOAD",
        	"RT FINAL LOAD",
        	"RT LMP",
        	"RT CONGESTION",
        	"RT LOSS",
        	"HOUR AHEAD LMP",
        	"HOUR AHEAD CONGESTION",
        	"HOUR AHEAD LOSS",
        	"LOAD",
        	"RT 15M PRICES LMP",
        	"RT 15M PRICES CONGESTION",
        	"RT 15M PRICES LOSS",
        	"INTERVAL START",
        	"INTERVAL END",
        	"INTERVAL START DART PRICE",
        	"INTERVAL END DART PRICE",
        	"INTERVAL TYPE DART PRICE",
        	"RT FINAL DART PRICE",
        	"INTERVAL START RT 15M PRICES",
        	"INTERVAL END RT 15M PRICES",
        	"INTERVAL TYPE RT 15M PRICES",
        	"INTERVAL START DART LOADS",
        	"INTERVAL END DART LOADS",
        	"INTERVAL TYPE DART LOADS",
        	"INTERVAL START RT LOADS",
        	"INTERVAL END RT LOADS",
        	"INTERVAL TYPE RT LOADS"
        };        
        
        columnHeadings = headings;
        columnsPrepared = true;
        
        // Prepare unknown column headings; Unknown fields are fields not specifically handled in this sample.
        String[] unknownHeadings = { "FIELD NAME",
        		                     "FIELD ID",
        							 "FIELD FORMAT",
        							 "CATEGORY",
        							 "SUBCATEGORY"
        						   };
        
        unknownColumnHeadings = unknownHeadings;
        unknownColumnsPrepared = true;
    }

    /**
     * Resets all instance variables to their default values.
     */
    private void resetInstanceVariables() {
        instrument = "----";
        companyName = "----";
        listedExchange = "----";
        fieldName = "----";
        fieldFormat = "----";
        currency = "----";
        isCIDUpdate = false;
        cidType = "";

        currentPricing.reset();
        pastPricing.reset();
        cidDetails.reset();
        yesEnergy.reset();
        
        marketUpdateInfo = new String[2];
    }

    /**
     * Processes the security status for the provided FieldItem.
     *
     * @param item The FieldItem containing security status data.
     */
    private void processSecurityStatus(FieldItem item) {
    	// Default value for security status is CIDDATA
        this.fieldFormat = "CIDDATA";
        
        CidData cidDataItem = item.getValueAsCidData();

        // Copy security qualifiers to the global array
        this.currentPricing.securityStatus = Arrays.copyOf(cidDataItem.getSecurityQualifiers(), 4);

        // Build a readable string representation of the security status
        StringBuilder securityStatsBuilder = new StringBuilder();
        for (short qualifier : this.currentPricing.securityStatus) {
            securityStatsBuilder.append(getSecurityStatusDescription(qualifier));
        }
        this.currentPricing.securityStats = securityStatsBuilder.toString();
    }

    /**
     * Retrieves human-readable data for a given security status ID.
     *
     * @param id The security status ID to search for.
     * @return A string formatted as "[id:description]" or "[id:No Data]" if no match is found.
     */
    public String getSecurityStatusDescription(short id) {
        if (gSecurityStatusList == null || gSecurityStatusList.getLength() == 0) {
            return "[" + id + ":No Data]";
        }

        for (int i = 0; i < gSecurityStatusList.getLength(); i++) {
            Node node = gSecurityStatusList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getAttribute("id").equals(String.valueOf(id))) {
                    String description = sanitizeQualifierDescription(element.getAttribute("lname"));
                    return "[" + id + ":" + description + "]";
                }
            }
        }
        return "[" + id + ":No Data]";
    }

    /**
     * Retrieves the market phase information based on the given phaseInfo.
     *
     * @param phaseInfo The short value representing the market phase.
     * @return A string describing the market phase.
     */
    private String getPhaseInfo(short phaseInfo) {
        switch (phaseInfo) {
            case 0:  // MARKET_PHASE_NO_UPDATE
                return "NoUpdate";
            case 1:  // MARKET_PHASE_CLOSED
                return "Closed";
            case 2:  // MARKET_PHASE_PREMARKET
                return "Pre-Market";
            case 3:  // MARKET_PHASE_OPENING_AUCTION
                return "Opening Auction";
            case 4:  // MARKET_PHASE_TRADING
                return "Trading";
            case 5:  // MARKET_PHASE_MANDATORY_QUOTE
                return "Mandatory Quote";
            case 6:  // MARKET_PHASE_VOLATILITY_INTERRUPT
                return "Volatility Interrupt";
            case 7:  // MARKET_PHASE_INTRADAY_AUCTION
                return "Intraday Auction";
            case 8:  // MARKET_PHASE_CLOSING_AUCTION
                return "Closing Auction";
            case 9:  // MARKET_PHASE_ALT_PRICING
                return "Alt Pricing";
            case 10: // MARKET_PHASE_POST_MARKET
                return "Post Market";
            case 11: // MARKET_PHASE_INDICATIVE_QUOTE
                return "Indicative Quote";
            default: // UNKNOWN
                return "UNKNOWN";
        }
    }

    /**
     * Retrieves human-readable data for an array of market phase IDs.
     *
     * @param phaseInfo An array of short values representing market phases.
     * @return A string describing the market phases, formatted as "[phase:description] | ...".
     */
    private String getPhaseInfo(short[] phaseInfo) {
        if (phaseInfo == null || phaseInfo.length == 0) {
            return "No market phase data";
        }

        StringBuilder phaseDescriptions = new StringBuilder();
        for (short phase : phaseInfo) {
            if (phaseDescriptions.length() > 0) {
                phaseDescriptions.append(" | ");
            }
            phaseDescriptions.append("[").append(phase).append(":").append(getPhaseInfo(phase)).append("]");
        }
        return phaseDescriptions.toString();
    }

    /**
     * Creates and configures a date formatter with a specific format and time zone.
     *
     * @return A SimpleDateFormat object configured with the desired format and time zone.
     */
    private SimpleDateFormat createDateFormatter() {
        String format = "MM/dd/yyyy hh:mm:ss.SSS a"; // e.g., 07/28/2015 11:09:41.918 PM
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf;
    }

    /**
     * Processes last trade data from the provided FieldItem.
     *
     * <p>This method extracts both past and current last trade details based on the
     * type of data (e.g., CIDR or standard last trade). It updates the respective fields
     * in `pastPricing` or `currentPricing` and constructs a readable qualifier string.</p>
     *
     * @param item The FieldItem containing last trade data.
     */
    private void processLastTrade(FieldItem item) {
        CidFieldData cidFieldDataItem = item.getValueAsCidFieldData();

        if (item.getId() == Quote.FieldItem.LRT_TYPE_CIDR_LAST) {
            // Populate pastPricing with CIDR last trade values
            pastPricing.lastLineID = cidFieldDataItem.getLineId();
            pastPricing.lastSeqNum = cidFieldDataItem.getSequenceNumberAsBigInteger();
            pastPricing.lastSeries = cidFieldDataItem.getSeries();
            pastPricing.lastExgTime = cidFieldDataItem.getExchangeTimeAsInstant();
            pastPricing.lastExg = cidFieldDataItem.getExchange();
            pastPricing.lastSize = cidFieldDataItem.getSize();
            pastPricing.last = updatePriceField(item);
            pastPricing.lastQualifiers = Arrays.copyOf(cidFieldDataItem.getQualifiers(), 4);
            pastPricing.lastQuals = getQualifierData(pastPricing.lastQualifiers, listedExchange);
        } else {
            // Populate currentPricing with standard last trade values
            currentPricing.lastLineID = cidFieldDataItem.getLineId();
            currentPricing.lastSeqNum = cidFieldDataItem.getSequenceNumberAsBigInteger();
            currentPricing.lastSeries = cidFieldDataItem.getSeries();
            currentPricing.lastExgTime = cidFieldDataItem.getExchangeTimeAsInstant();
            currentPricing.lastExg = cidFieldDataItem.getExchange();
            currentPricing.lastSize = cidFieldDataItem.getSize();
            currentPricing.last = updatePriceField(item);
            currentPricing.lastQualifiers = Arrays.copyOf(cidFieldDataItem.getQualifiers(), 4);
            currentPricing.lastQuals = getQualifierData(currentPricing.lastQualifiers, listedExchange);
        }
    }

    /**
     * Processes bid data from the provided FieldItem.
     *
     * <p>This method extracts both past and current bid details based on the type of
     * data (e.g., CIDR or standard bid). It updates the respective fields in `pastPricing`
     * or `currentPricing` and constructs a readable qualifier string.</p>
     *
     * @param item The FieldItem containing bid data.
     */
    private void processBidData(FieldItem item) {
        CidFieldData cidFieldDataItem = item.getValueAsCidFieldData();

        if (item.getId() == Quote.FieldItem.LRT_TYPE_CIDR_BID) {
            // Populate pastPricing with CIDR bid values
            pastPricing.bidLineID = cidFieldDataItem.getLineId();
            pastPricing.bidSeqNum = cidFieldDataItem.getSequenceNumberAsBigInteger();
            pastPricing.bidSeries = cidFieldDataItem.getSeries();
            pastPricing.bidExgTime = cidFieldDataItem.getExchangeTimeAsInstant();
            pastPricing.bidExg = cidFieldDataItem.getExchange();
            pastPricing.bidSize = cidFieldDataItem.getSize();
            pastPricing.bid = updatePriceField(item);
            pastPricing.bidQualifiers = Arrays.copyOf(cidFieldDataItem.getQualifiers(), 4);
            pastPricing.bidQuals = getQualifierData(pastPricing.bidQualifiers, listedExchange);
        } else {
            // Populate currentPricing with standard bid values
            currentPricing.bidLineID = cidFieldDataItem.getLineId();
            currentPricing.bidSeqNum = cidFieldDataItem.getSequenceNumberAsBigInteger();
            currentPricing.bidSeries = cidFieldDataItem.getSeries();
            currentPricing.bidExgTime = cidFieldDataItem.getExchangeTimeAsInstant();
            currentPricing.bidExg = cidFieldDataItem.getExchange();
            currentPricing.bidSize = cidFieldDataItem.getSize();
            currentPricing.bid = updatePriceField(item);
            currentPricing.bidQualifiers = Arrays.copyOf(cidFieldDataItem.getQualifiers(), 4);
            currentPricing.bidQuals = getQualifierData(currentPricing.bidQualifiers, listedExchange);
        }
    }

    /**
    * Processes ask data from the provided FieldItem.
    *
    * <p>This method extracts both past and current ask details based on the type of
    * data (e.g., CIDR or standard ask). It updates the respective fields in `pastPricing`
    * or `currentPricing` and constructs a readable qualifier string.</p>
    *
    * @param item The FieldItem containing ask data.
    */
    private void processAskData(FieldItem item) {
        CidFieldData cidFieldDataItem = item.getValueAsCidFieldData();

        if (item.getId() == Quote.FieldItem.LRT_TYPE_CIDR_ASK) {
            // Populate pastPricing with CIDR ask values
            pastPricing.askLineID = cidFieldDataItem.getLineId();
            pastPricing.askSeqNum = cidFieldDataItem.getSequenceNumberAsBigInteger();
            pastPricing.askSeries = cidFieldDataItem.getSeries();
            pastPricing.askExgTime = cidFieldDataItem.getExchangeTimeAsInstant();
            pastPricing.askExg = cidFieldDataItem.getExchange();
            pastPricing.askSize = cidFieldDataItem.getSize();
            pastPricing.ask = updatePriceField(item);
            pastPricing.askQualifiers = Arrays.copyOf(cidFieldDataItem.getQualifiers(), 4);
            pastPricing.askQuals = getQualifierData(pastPricing.askQualifiers, listedExchange);
        } else {
            // Populate currentPricing with standard ask values
            currentPricing.askLineID = cidFieldDataItem.getLineId();
            currentPricing.askSeqNum = cidFieldDataItem.getSequenceNumberAsBigInteger();
            currentPricing.askSeries = cidFieldDataItem.getSeries();
            currentPricing.askExgTime = cidFieldDataItem.getExchangeTimeAsInstant();
            currentPricing.askExg = cidFieldDataItem.getExchange();
            currentPricing.askSize = cidFieldDataItem.getSize();
            currentPricing.ask = updatePriceField(item);
            currentPricing.askQualifiers = Arrays.copyOf(cidFieldDataItem.getQualifiers(), 4);
            currentPricing.askQuals = getQualifierData(currentPricing.askQualifiers, listedExchange);
        }
    }
    
    /**
     * Infers the listed exchange from the instrument if it is not already set.
     *
     * <p>If the listed exchange is set to the default placeholder ("----"), this method attempts to
     * infer the exchange from the instrument string by splitting it on hyphens and using the second part.</p>
     */
    private void processListedExchange() {
        // Check if the listed exchange is set to the default placeholder
        if ("----".equals(listedExchange)) {
            // Attempt to infer the exchange from the instrument
            String[] parts = instrument.split("-");

            // Ensure the instrument has more than one part after the split
            if (parts.length > 1) {
                // Assign the second part of the instrument to the listed exchange
                listedExchange = parts[1];
            }
        }
    }

    /**
     * Processes and displays retrieved data from either onResponse or onUpdate events.
     *
     * <p>This method resets instance variables, initializes default arrays, determines whether
     * the update is a CIDR update, processes the listed exchange, and iterates through each
     * field item in the quote to handle the data. It also formats and displays the exchange times
     * for both CIDR and standard updates.</p>
     *
     * @param quote The Quote object containing the data to process and display.
     */
    private void processData(Quote quote) {
        short[] defaultArray = {0, 0, 0, 0};

        // Reset instance variables and global arrays with default values
        resetInstanceVariables();
        currentPricing.initializeQualifiers(defaultArray);
        pastPricing.initializeQualifiers(defaultArray);
        cidDetails.initializeQualifiers(defaultArray);
        
        // Check the CATEGORY type for this update
        FieldItem category = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_CATEGORY);
        
        /* This sample divides the tasks for processing CID messages from other types of messages.
         *
         * CATEGORY_INDICE         = 'A'
         * CATEGORY_STOCK          = 'B'
         * CATEGORY_STOCKOPTION    = 'G'
         * CATEGORY_FUTURE         = 'H'
         * CATEGORY_FUTUREOPTION   = 'I'
         * CATEGORY_CURRENCYOPTION = 'J'
         * CATEGORY_MUTUALFUND     = 'K'
         * CATEGORY_MONEYFUND      = 'L'
         * CATEGORY_GAINER         = 'U'
         * CATEGORY_HEADLINE       = 'V'
         * CATEGORY_FORWARD        = 'W'
         * CATEGORY_CIDR           = 'X'  <-- Corrections, Insertions, Deletions
         * CATEGORY_SPREAD         = 'Y'
         * CATEGORY_INFO           = 'Z'
         */
        
        isCIDUpdate = (category != null && category.getValueAsChar() == 'X');
        
        categoryName = category != null ? utilities.getCategoryName(category.getValueAsChar()) : "----";
        
        // Check the SUBCATEGORY type for this update
        FieldItem subcategory = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_SUBCATEGORY);
        
        subcategoryName = subcategory != null ? utilities.getSubcategoryName(subcategory.getValueAsChar()) : "----";
        
        // Determine the listed exchange and process it if not already set
        FieldItem exchg = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_LISTEXG);
        listedExchange = (exchg != null) ? exchg.getValueAsString() : "----";

        // Determine the instrument to which this update applies
        FieldItem sym = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_KEY);
        instrument = (sym != null) ? sym.getValueAsString() : "----";
        
        // Determine if this update includes LRT_TYPE_DISPLAY_KEY; if so, update the instrument
        // or display both the instrument and the display key
        FieldItem sym2 = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_DISPLAY_KEY);
        displayKey = (sym2 != null) ? sym2.getValueAsString() : "----";
        
        // Final check of the listed exchange
        // It could be "----" and if it is we will try to infer the exchange from the instrument.
        processListedExchange();
        
        // Process each field item in the quote
        for (FieldItem item : quote) {
            if (!quote.isFieldItemNull(item.getId())) {
                handleFieldItem(item);
            }
        }	        
        
        // Update the status and market status fields
        marketUpdateInfo = this.utilities.getMarketStatus(updateStatus, marketStatus);
                
        // Display the formatted data
        if (!isCIDUpdate) {
            Date lastExgTime = (currentPricing.lastExgTime != null) ? Date.from(currentPricing.lastExgTime) : Date.from(Instant.EPOCH);
            Date bidExgTime = (currentPricing.bidExgTime != null) ? Date.from(currentPricing.bidExgTime) : Date.from(Instant.EPOCH);
            Date askExgTime = (currentPricing.askExgTime != null) ? Date.from(currentPricing.askExgTime) : Date.from(Instant.EPOCH);

            displayFormattedData(lastExgTime, bidExgTime, askExgTime);
        } else {
            Date pastLastExgTime = (pastPricing.lastExgTime != null) ? Date.from(pastPricing.lastExgTime) : Date.from(Instant.EPOCH);
            Date pastBidExgTime = (pastPricing.bidExgTime != null) ? Date.from(pastPricing.bidExgTime) : Date.from(Instant.EPOCH);
            Date pastAskExgTime = (pastPricing.askExgTime != null) ? Date.from(pastPricing.askExgTime) : Date.from(Instant.EPOCH);

            displayFormattedData(pastLastExgTime, pastBidExgTime, pastAskExgTime);
        }
        
	    // Reset unknown fields for the next update
	    resetUnknownFields();			        
    }

    /**
     * Retrieves and returns the price value from the given FieldItem based on its format.
     *
     * <p>This method handles multiple formats, including DOUBLE, CIDFIELDDATA, and CIDPRICEFIELDDATA,
     * to extract the appropriate value.</p>
     *
     * @param item The FieldItem containing the data to extract.
     * @return The extracted price value as a double, or 0 if the format is unsupported.
     */
    private double updatePriceField(FieldItem item) {
        double value = 0.0;

        switch (item.getFormat()) {
            case DOUBLE:
                // Handle DOUBLE format
                value = item.getValueAsDouble();
                break;

            case CIDFIELDDATA:
                // Handle CIDFIELDDATA format
                value = item.getValueAsCidFieldData().getValue();
                break;

            case CIDPRICEFIELDDATA:
                // Handle CIDPRICEFIELDDATA format
                value = item.getValueAsCidPriceFieldData().getValue();
                break;

            default:
                // Log or handle unexpected formats if necessary
                System.out.println("Unsupported FieldItem format: " + item.getFormat());
                break;
        }

        return value;
    }

    /**
     * Handles individual field items from a quote object.
     *
     * <p>This method processes various field items, such as instrument details, market status,
     * pricing data, and qualifiers, and updates the respective fields in `currentPricing`,
     * `pastPricing`, or `cidDetails` as needed. It also delegates specific field types to
     * dedicated methods like `processLastTrade`, `processBidData`, and `processAskData`.</p>
     *
     * @param item The FieldItem to process.
     */
    private void handleFieldItem(FieldItem item) {
        switch (item.getId()) {
            case Quote.FieldItem.LRT_TYPE_KEY:
                instrument = item.getValueAsString();
                break;

            case Quote.FieldItem.LRT_TYPE_STATUS:
                updateStatus = item.getValueAsCharArray();
                break;
                
            case Quote.FieldItem.LRT_TYPE_MSTATUS:
            	marketStatus = item.getValueAsLong(true);
            	break;
            	
            case Quote.FieldItem.LRT_TYPE_LISTEXG:
                listedExchange = item.getValueAsString();
                break;

            case Quote.FieldItem.LRT_TYPE_COMPANY:
                companyName = item.getValueAsString();
                companyName = companyName.replace(",", " "); // Remove commas for CSV compatibility
                break;

            case Quote.FieldItem.LRT_TYPE_OPEN:
                currentPricing.open = updatePriceField(item);
                break;

            case Quote.FieldItem.LRT_TYPE_HIGH:
                currentPricing.high = updatePriceField(item);
                currentPricing.highTime = item.getValueAsCidPriceFieldData().getExchangeTimeAsInstant();
                System.out.println("HIGH TIME:  " + utilities.convertTime(safeInstant(currentPricing.highTime)));
                break;

            case Quote.FieldItem.LRT_TYPE_LOW:
                currentPricing.low = updatePriceField(item);
                break;

            case Quote.FieldItem.LRT_TYPE_CLOSE:
                currentPricing.close = updatePriceField(item);
                break;

            case Quote.FieldItem.LRT_TYPE_CURRENCYCODEASCII:
                currency = item.getValueAsString();
                break;

            case Quote.FieldItem.LRT_TYPE_ACCVOL:
                currentPricing.accumVolume = item.getValueAsDouble();
                yesEnergy.volume = item.getValueAsDouble();                
                break;

            case Quote.FieldItem.LRT_TYPE_CIDR_TYPE:
                switch (item.getValueAsByte()) {
                    case 2:  // LRT_CIDR_CORRECTION
                        cidType = "CORRECTION";
                        break;
                    case 4:  // LRT_CIDR_INSERT
                        cidType = "INSERTION";
                        break;
                    case 3:  // LRT_CIDR_CANCEL
                        cidType = "DELETION";
                        break;
                    case 0:  // LRT_CIDR_UNKNOWN
                        cidType = "UNKNOWN";
                        break;
                }
                break;
            	
            case Quote.FieldItem.LRT_TYPE_LAST:
            case Quote.FieldItem.LRT_TYPE_QUALIFIEDTRADE:
            case Quote.FieldItem.LRT_TYPE_CIDR_LAST:
            	fieldName = Quote.FieldItem.getFieldName(item);
            	fieldFormat = item.getFormat().name();
            	
                processLastTrade(item);
                break;

            case Quote.FieldItem.LRT_TYPE_MPHASE:
                currentPricing.mphaseQualifiers = item.getValueAsShortArray();
                currentPricing.mphaseQuals = getPhaseInfo(currentPricing.mphaseQualifiers);
                break;

            case Quote.FieldItem.LRT_TYPE_CIDDATA_MRRT:
            	fieldName = Quote.FieldItem.getFieldName(item);
            	fieldFormat = item.getFormat().name();
            	
                processSecurityStatus(item);
                break;

            case Quote.FieldItem.LRT_TYPE_BID:
            case Quote.FieldItem.LRT_TYPE_CIDR_BID:
            	fieldName = Quote.FieldItem.getFieldName(item);
            	fieldFormat = item.getFormat().name();
            	
                processBidData(item);
                break;

            case Quote.FieldItem.LRT_TYPE_ASK:
            case Quote.FieldItem.LRT_TYPE_CIDR_ASK:
            	fieldName = Quote.FieldItem.getFieldName(item);
            	fieldFormat = item.getFormat().name();
            	
                processAskData(item);
                break;
            case Quote.FieldItem.LRT_TYPE_SETTLEMENT:
            	currentPricing.settlement = updatePriceField(item);           	
            	break;
            case Quote.FieldItem.LRT_TYPE_MIDPRICE:
            	currentPricing.mid = updatePriceField(item);   
            	break;
            case Quote.FieldItem.LRT_TYPE_LOAD:
            	yesEnergy.load = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_DAY_AHEAD_LOAD:
            	yesEnergy.dayAheadLoad = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_DAY_AHEAD_LMP:
            	yesEnergy.dayAheadLMP = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_DAY_AHEAD_CONGESTION:
            	yesEnergy.dayAheadCongestion = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_DAY_AHEAD_LOSS:
            	yesEnergy.dayAheadLoss = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_LOAD:
            	System.out.println("LRT_TYPE_RT_LOAD: " + item.getFormat().name());
            	yesEnergy.rtLoad = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_LMP:
            	yesEnergy.rtLMP = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_CONGESTION:
            	yesEnergy.rtCongestion = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_LOSS:
            	yesEnergy.rtLoss = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_FINAL_LOAD:
            	System.out.println("LRT_TYPE_RT_FINAL_LOAD: " + item.getFormat().name());
            	yesEnergy.rtFinalLoad = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_PRICES_LMP:
            	yesEnergy.rtPricesLMP = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_PRICES_CONGESTION:
                yesEnergy.rtPricesCongestion = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_PRICES_LOSS:
            	yesEnergy.rtPricesLoss = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_HOUR_AHEAD_LMP:
            	yesEnergy.hourAheadLMP = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_HOUR_AHEAD_CONGESTION:
            	yesEnergy.hourAheadCongestion = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_HOUR_AHEAD_LOSS:
            	yesEnergy.hourAheadLoss = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_15M_PRICES_LMP:
            	yesEnergy.rt15mPricesLMP = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_15M_PRICES_CONGESTION:
            	yesEnergy.rt15mPricesCongestion = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_RT_15M_PRICES_LOSS:            
            	yesEnergy.rt15mPricesLoss = item.getValueAsDouble();
            	break;
            case Quote.FieldItem.LRT_TYPE_INTERVAL_START:
            	yesEnergy.intervalStart = item.getValueAsBigInteger(false);
				break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_END:
		    	yesEnergy.intervalEnd = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_START_DART_PRICES:
		    	yesEnergy.intervalStartDARTPrice = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_END_DART_PRICES:
		    	yesEnergy.intervalEndDARTPrice = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_TYPE_DART_PRICES:
		    	yesEnergy.intervalTypeDARTPrices = item.getValueAsByte();
		    	break;
		    case Quote.FieldItem.LRT_TYPE_RT_FINAL_DART_PRICE:
		    	yesEnergy.rtFinalDARTPrice = item.getValueAsString();
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_START_RT15_PRICES:
		    	yesEnergy.intervalStartRT15mPrices = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_END_RT15_PRICES:
		    	yesEnergy.intervalEndRT15mPrices = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_TYPE_RT15_PRICES:
		    	yesEnergy.intervalTypeRT15mPrices = item.getValueAsByte();
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_START_DART_LOADS:
                yesEnergy.intervalStartDARTLoads = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_END_DART_LOADS:
		    	yesEnergy.intervalEndDARTLoads = item.getValueAsBigInteger(false);
		    	break;
			case Quote.FieldItem.LRT_TYPE_UTCOFFSET:
				yesEnergy.utcOffset = item.getValueAsString();
				break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_TYPE_DART_LOADS:
		    	yesEnergy.intervalTypeDARTLoads = item.getValueAsByte();
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_START_RT_LOADS:
		    	yesEnergy.intervalStartRTLoads = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_END_RT_LOADS:
		    	yesEnergy.intervalEndRTLoads = item.getValueAsBigInteger(false);
		    	break;
		    case Quote.FieldItem.LRT_TYPE_INTERVAL_TYPE_RT_LOADS:
                yesEnergy.intervalTypeRTLoads = item.getValueAsByte();
		    	break;
		    case Quote.FieldItem.LRT_TYPE_RTSYSPRICE:
		    	yesEnergy.rtSysPrice = item.getValueAsDouble();
		    	break;
            default:   
            	// "Unknown" fields are fields that are not explicitly handled by this sample.
           		processUnknownField(item);
                break;
        }
    }

    /**
     * Handles unknown field items from a quote object.
     *
     * <p>This method processes field items that are not explicitly handled by other methods.
     * It logs the field name and ID to the console if the displayData flag is set in the consoleSample object.</p>
     *
     * @param item The FieldItem to process.
     */
    private void processUnknownField(FieldItem item) {   	
		if (consoleSample.displayUnknownFields() == 1 || consoleSample.displayUnknownFields() == 2) {
			unknownFields.add(new unknownFieldsDetails(Quote.FieldItem.getFieldName(item), item.getId(), item.getFormat().name(), item.getValue(), categoryName, subcategoryName));
		}
	}
    
	/**
	 * Resets instance variables and clears the unknown fields list.
	 */
    private void resetUnknownFields() {
    	unknownFields.clear();
    }

	/**
	 * Formats and displays data for both standard and CIDR updates.
	 *
	 * <p>This method outputs formatted data to the console, distinguishing between
	 * CIDR updates and standard updates. It handles exchange times, instrument details,
	 * and pricing information.</p>
	 *
	 * @param lastExgTime  The last exchange time for the last trade.
	 * @param bidExgTime   The last exchange time for the bid.
	 * @param askExgTime   The last exchange time for the ask.
	 */
	private void displayFormattedData(Date lastExgTime, Date bidExgTime, Date askExgTime) {
	    if (qualifierFilters != null && !qualifierFilters.isEmpty()) {
	        boolean isQualifierPresent = qualifierFilters.stream()
	            .anyMatch(filter -> currentPricing.lastQuals.contains("[" + filter + ":"));
	        if (!isQualifierPresent) {
	            return;
	        }
	    }
	    
	    // Print unknown fields ONLY if displayUnknownFields is set to 2
        if (consoleSample.displayUnknownFields() == 2 && !unknownFields.isEmpty()) {
        	if (!consoleSample.displayData()) {
	            for (unknownFieldsDetails field : unknownFields) {
	                System.out.println(String.format(
	                    "----,----,----,----,----,UNKNOWN FIELD,%s [ %d ],%s,%s,%s,%s,%s",
	                    safeString(field.fieldName),
	                    field.fieldID,
	                    safeString(field.fieldFormat),
	                    safeString(field.categoryName),
	                    safeString(field.subcategoryName),
	                    safeString(instrument),
	                    field.fieldValues
	                ));
	            }
        	}
        	else {
                for (unknownFieldsDetails field : unknownFields) {
                    System.out.println(String.format(
                        "SYMBOL: %s UNKNOWN FIELD: %s [ %d ] \tFORMAT: %s \tVALUE: %s \tCATEGORY: %s \tSUBCATEGORY: %s",
                        safeString(instrument),
                        safeString(field.fieldName),
                        field.fieldID,
                        safeString(field.fieldFormat),
                        field.fieldValues,
                        safeString(field.categoryName),
                        safeString(field.subcategoryName)
                    ));
                }
        	}
        	return;
        }	    
	
	    try {
	        List<Object> formattedData;
	
	        SimpleDateFormat sdf = createDateFormatter();
	        Instant now = Instant.now();
	        Date currTime = Date.from(now);
	
	        String lastExgTimeStr = formatTime(lastExgTime, sdf);
	        String bidExgTimeStr = formatTime(bidExgTime, sdf);
	        String askExgTimeStr = formatTime(askExgTime, sdf);
	
	        if (!consoleSample.displayData()) {
	            if (!columnsPrepared) {
	                createColumnHeadings();
	            }
	
	            if (!columnsPrinted) {
	                System.out.println(String.join(",", columnHeadings));
	                columnsPrinted = true;
	            }
	
	            formattedData = new ArrayList<>(Arrays.asList(
	                utilities.convertTime(System.currentTimeMillis()),
	                sdf.format(currTime),
	                safeString(yesEnergy.utcOffset),
	                safeString(marketUpdateInfo[1]),
	                safeString(currentPricing.mphaseQuals),
	                safeString(marketUpdateInfo[0]),
	                safeString(fieldName),
	                safeString(fieldFormat),
	                safeString(categoryName),
	                safeString(subcategoryName),
	                safeString(instrument),
	                safeString(displayKey),
	                isCIDUpdate ? "*" : "",
	                safeString(cidType),
	                safeString(currentPricing.securityStats),
	                safeString(companyName),
	
	                currentPricing.open,
	                currentPricing.high,
	                currentPricing.low,
	                currentPricing.close,
	
	                utilities.convertTime(safeInstant(currentPricing.lastExgTime)),
	                lastExgTimeStr,
	                safeString(currentPricing.lastQuals),
	                safeString(currency),
	                currentPricing.accumVolume,
	                currentPricing.last,
	                currentPricing.lastSize,
	                safeString(currentPricing.lastExg),
	                currentPricing.lastSeries,
	                safeBigInt(currentPricing.lastSeqNum),
	                currentPricing.lastLineID,
	
	                utilities.convertTime(safeInstant(currentPricing.bidExgTime)),
	                bidExgTimeStr,
	                safeString(currentPricing.bidQuals),
	                currentPricing.bid,
	                currentPricing.bidSize,
	                safeString(currentPricing.bidExg),
	                currentPricing.bidSeries,
	                safeBigInt(currentPricing.bidSeqNum),
	                currentPricing.bidLineID,
	
	                utilities.convertTime(safeInstant(currentPricing.askExgTime)),
	                askExgTimeStr,
	                safeString(currentPricing.askQuals),
	                currentPricing.ask,
	                currentPricing.askSize,
	                safeString(currentPricing.askExg),
	                currentPricing.askSeries,
	                safeBigInt(currentPricing.askSeqNum),
	                currentPricing.askLineID,
	
	                currentPricing.settlement,
	                currentPricing.mid,
	                yesEnergy.volume,
	
	                yesEnergy.dayAheadLMP,
	                yesEnergy.dayAheadCongestion,
	                yesEnergy.dayAheadLoss,
	
	                yesEnergy.rtPricesLMP,
	                yesEnergy.rtPricesCongestion,
	                yesEnergy.rtPricesLoss,
	                yesEnergy.rtSysPrice,
	
	                yesEnergy.rtLoad,
	                yesEnergy.dayAheadLoad,
	                yesEnergy.rtFinalLoad,
	
	                yesEnergy.rtLMP,
	                yesEnergy.rtCongestion,
	                yesEnergy.rtLoss,
	
	                yesEnergy.hourAheadLMP,
	                yesEnergy.hourAheadCongestion,
	                yesEnergy.hourAheadLoss,
	
	                yesEnergy.load,
	                yesEnergy.rt15mPricesLMP,
	                yesEnergy.rt15mPricesCongestion,
	                yesEnergy.rt15mPricesLoss,
	
	                safeBigInt(yesEnergy.intervalStart),
	                safeBigInt(yesEnergy.intervalEnd),
	
	                safeBigInt(yesEnergy.intervalStartDARTPrice),
	                safeBigInt(yesEnergy.intervalEndDARTPrice),
	                yesEnergy.intervalTypeDARTPrices,
	
	                safeString(yesEnergy.rtFinalDARTPrice),
	
	                safeBigInt(yesEnergy.intervalStartRT15mPrices),
	                safeBigInt(yesEnergy.intervalEndRT15mPrices),
	                yesEnergy.intervalTypeRT15mPrices,
	
	                safeBigInt(yesEnergy.intervalStartDARTLoads),
	                safeBigInt(yesEnergy.intervalEndDARTLoads),
	                yesEnergy.intervalTypeDARTLoads,
	
	                safeBigInt(yesEnergy.intervalStartRTLoads),
	                safeBigInt(yesEnergy.intervalEndRTLoads),
	                yesEnergy.intervalTypeRTLoads
	            ));
	
	            System.out.println(formattedData.stream()
	                .map(Object::toString)
	                .collect(Collectors.joining(",")));	
	            
	            if (consoleSample.displayUnknownFields() == 1 && !unknownFields.isEmpty()) {
    	            for (unknownFieldsDetails field : unknownFields) {
    	                System.out.println(String.format(
    	                    "----,----,----,----,----,UNKNOWN FIELD,%s [ %d ],%s,%s,%s,%s,%s",
    	                    safeString(field.fieldName),
    	                    field.fieldID,
    	                    safeString(field.fieldFormat),
    	                    safeString(field.categoryName),
    	                    safeString(field.subcategoryName),
    	                    safeString(instrument),
    	                    field.fieldValues
    	                ));
    	            }
            	}
	        } else {
	            formattedData = new ArrayList<>(Arrays.asList(
	                "SYMBOL:", safeString(instrument),
	                " MARKET STATE:", safeString(marketUpdateInfo[1]), // market status
	                "MARKET STATUS:", safeString(currentPricing.mphaseQuals),
	                "CID UPDATE:", isCIDUpdate ? "YES" : "NO",
	                "CID TYPE:", safeString(cidType),
	
	                "ASK:", currentPricing.ask,
	                "SIZE:", currentPricing.askSize,
	                "EXG TIME:", utilities.convertTime(safeInstant(currentPricing.askExgTime)),
	                "QUALIFIERS:", safeString(currentPricing.askQuals),
	
	                "BID:", currentPricing.bid,
	                "SIZE:", currentPricing.bidSize,
	                "EXG TIME:", utilities.convertTime(safeInstant(currentPricing.bidExgTime)),
	                "QUALIFIERS:", safeString(currentPricing.bidQuals),
	
	                "LAST:", currentPricing.last,
	                "SIZE:", currentPricing.lastSize,
	                "EXG TIME:", utilities.convertTime(safeInstant(currentPricing.lastExgTime)),
	                "QUALIFIERS:", safeString(currentPricing.lastQuals)
	            ));
	
	            System.out.println(formattedData.stream()
	                .map(Object::toString)
	                .collect(Collectors.joining(" ", "", "")));
	
	            // Print unknown fields on separate lines (console-style)
	            if (consoleSample.displayUnknownFields() == 1 && !unknownFields.isEmpty()) {
	                for (unknownFieldsDetails field : unknownFields) {
	                    System.out.println(String.format(
	                        "SYMBOL: %s UNKNOWN FIELD: %s [ %d ] \tFORMAT: %s \tVALUE: %s \tCATEGORY: %s \tSUBCATEGORY: %s",
	                        safeString(instrument),
	                        safeString(field.fieldName),
	                        field.fieldID,
	                        safeString(field.fieldFormat),
	                        field.fieldValues,
	                        safeString(field.categoryName),
	                        safeString(field.subcategoryName)
	                    ));
	                }
	            }
	        }
	
	    } catch (IllegalFormatConversionException e) {
	        System.err.println("<<< Illegal format conversion while printing data: " + e.getMessage());
	    }
	}

    /**
	 * Formats a Date into a string safely.
	 */
	private String formatTime(Date date, SimpleDateFormat sdf) {
	    return (date != null && !date.toInstant().equals(Instant.EPOCH))
	        ? sdf.format(date)
	        : "----";
	}

	/**
	 * Returns a non-null string.
	 */
	private String safeString(String str) {
	    return (str != null) ? str : "N/A";
	}

	/**
	 * Returns a non-null BigInteger as a String.
	 */
	private String safeBigInt(BigInteger num) {
	    return (num != null) ? num.toString() : "0";
	}

	/**
	 * Returns a safe Instant value in epoch millis.
	 */
	private long safeInstant(Instant instant) {
	    return (instant != null) ? instant.toEpochMilli() : 0;
	}
 }