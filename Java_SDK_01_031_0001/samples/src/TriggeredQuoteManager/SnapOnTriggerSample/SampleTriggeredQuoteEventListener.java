package TriggeredQuoteManager.SnapOnTriggerSample;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatConversionException;
import java.util.TimeZone;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Required imports from the JStandard library
import com.esignal.jstandard.beans.quote.CidData;
import com.esignal.jstandard.beans.quote.CidFieldData;
import com.esignal.jstandard.beans.quote.CidPriceFieldData;
import com.esignal.jstandard.beans.quote.MSTATUS;
import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Status;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.QuoteListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.event.TriggeredQuoteEvent;
import com.esignal.jstandard.event.TriggeredQuoteEventListener;
import com.esignal.jstandard.managers.dbc.DbcCodes;

import Util.SampleUtilities;

/**
 * Provides methods for handling various states associated with Snap-on-Trigger
 * requests. Refactored to follow the modular style from the QualifiersParsingSample.
 */
public class SampleTriggeredQuoteEventListener implements TriggeredQuoteEventListener, QuoteListener {

    // Reference to the SnapOnTriggerSample object for accessing specific functions
    private final SnapOnTriggerSample consoleSample;

    // SampleUtilities instance
    private final SampleUtilities utilities = SampleUtilities.getInstance();

    // Track how many triggers have been added
    private int triggerCount = 0;

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
        Instant lastExgTime, settlementExgTime;
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
            bidExgTime = askExgTime = lastExgTime = settlementExgTime = highTime = Instant.EPOCH;
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
        int lastLineID, bidLineID, askLineID;
        long lastSeqNum, bidSeqNum, askSeqNum;
        short lastSeries, bidSeries, askSeries;
        int lastExgTime, bidExgTime, askExgTime;
        String lastExg, bidExg, askExg;
        double last, bid, ask;
        double lastSize, bidSize, askSize;
        String lastQuals, bidQuals, askQuals, securityStats;
        short[] lastQualifiers, bidQualifiers, askQualifiers, securityStatus;

        /**
         * Resets all CID details to their default values.
         */
        void reset() {
            lastLineID = bidLineID = askLineID = 0;
            lastSeqNum = bidSeqNum = askSeqNum = 0;
            lastSeries = bidSeries = askSeries = 0;
            lastExgTime = bidExgTime = askExgTime = 0;
            lastExg = bidExg = askExg = "----";
            last = bid = ask = 0;
            lastSize = bidSize = askSize = 0;
            lastQuals = bidQuals = askQuals = securityStats = "----";
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
        double open, high, low, close, ask, bid, last, settle, mid;
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
            open = high = low = close = ask = bid = last = settle = mid = 0;
            volume = 0;
            dayAheadLMP = dayAheadCongestion = dayAheadLoss = 0;
            rtPricesLMP = rtPricesCongestion = rtPricesLoss = 0;
            rtLoad = dayAheadLoad = rtFinalLoad = 0;
            rtLMP = rtCongestion = rtLoss = 0;
            hourAheadLMP = hourAheadCongestion = hourAheadLoss = 0;
            load = 0;
            rt15mPricesLMP = rt15mPricesCongestion = rt15mPricesLoss = 0;
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
    
// ///////////////////////////////////////////////////////////////////////////////
    // Metadata
    private long updateFlags;
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
    private boolean columnsPrepared = false;
    private boolean columnsPrinted = false;
    private String[] columnHeadings;

    // Qualifiers parsing
    // The following members are used to load and parse the "qualifiers.tab" file.
    // We need to load this file into an XML parser to retrieve the data associated with any
    // qualifier information we receive.
    // This information provides more details about the data.
    private static final int EXCHANGE_THRESHOLD = 191;
    private Document gDoc;
    private NodeList gQualifierList;
    private NodeList gSecurityStatusList;

    /**
     * Constructor for SampleTriggeredQuoteEventListener.
     *
     * @param consoleSample The SnapOnTriggerSample object for accessing specific functions.
     */
    public SampleTriggeredQuoteEventListener(SnapOnTriggerSample consoleSample) {
        this.consoleSample = consoleSample;
        loadQualifiersAndSecurityStatus();

        // If not displaying data to console in a row-by-row verbose manner,
        // we prepare column headings for CSV-like output.
        if (!consoleSample.displayData()) {
            createColumnHeadings();
        }
    }

    /**
     * Load and parse the qualifiers.tab file to retrieve qualifier and security status data.
     */
    private void loadQualifiersAndSecurityStatus() {
        if (consoleSample.displayData()) {
            System.out.println("Loading qualifiers information...");
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            gDoc = builder.parse(new URL("http://fs2.esignal.com/qualifiers.tab").openStream());
            gDoc.getDocumentElement().normalize();

            gQualifierList = gDoc.getElementsByTagName("q");
            gSecurityStatusList = gDoc.getElementsByTagName("securityStatus");

            if (consoleSample.displayData()) {
                System.out.println("Qualifiers information loaded.\n");
            }
        } catch (Exception e) {
            System.out.println("Could not load qualifiers information.");
            System.out.println(e + "\n");
        }
    }

    /**
     * Create and store the column headings for CSV-like output.
     */
    private void createColumnHeadings() {
        String[] headings = {
            "CURRENT TIME (EPOCH IN MS)",
            "CURRENT LOCAL TIME",
            "MARKET STATE",
            "MARKET PHASE",
            "STATUS",
            "FIELD NAME",
            "FIELD FORMAT",
            "INSTRUMENT",
            "SECURITY QUALIFIERS",
            "COMPANY NAME",
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
    }

    /**
     * Reset the main instance variables, mirroring the approach of the first file.
     */
    private void resetInstanceVariables() {
        instrument      = "----";
        companyName     = "----";
        listedExchange  = "----";
        currency        = "----";
        fieldName       = "----";
        fieldFormat     = "----";
        cidType         = "";
        isCIDUpdate = false;

        currentPricing.reset();
        pastPricing.reset();
        cidDetails.reset();
        yesEnergy.reset();
        marketUpdateInfo = new String[2];
   }

    /**
     * Create a date formatter as in the first file.
     */
    private SimpleDateFormat createDateFormatter() {
        String format = "MM/dd/yyyy hh:mm:ss a";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf;
    }

    /**
     * Return a description of the security status, similar to getSecurityStatusDescription(...) from the first file.
     */
    private String getSecurityStatusDescription(short id) {
        if (gSecurityStatusList == null || gSecurityStatusList.getLength() == 0) {
            return "No security status data";
        }
        for (int j = 0; j < gSecurityStatusList.getLength(); j++) {
            Node nNode = gSecurityStatusList.item(j);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) nNode;
                if (elem.getAttribute("id").equals(String.valueOf(id))) {
                    return elem.getAttribute("lname");
                }
            }
        }
        return "No security status data";
    }

    /**
     * Return a description of a single qualifier ID, matching the style of the first file’s getQualifierData(...).
     */
    public String getQualifierData(short id, String exg) {
        if (gQualifierList == null || gQualifierList.getLength() == 0) {
            return "[" + id + ":No qualifier data]";
        }
        for (int j = 0; j < gQualifierList.getLength(); j++) {
            Node node = gQualifierList.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;

                // Qualifier IDs above 191 require matching the "exch" attribute
                if (id > 191) {
                    if (elem.getAttribute("id").equals(String.valueOf(id))
                            && elem.getAttribute("exch").contains(exg)) {
                        String desc = elem.getAttribute("lname").replace(",", "");
                        return "[" + id + ":" + desc + "]";
                    }
                } else if (elem.getAttribute("id").equals(String.valueOf(id))) {
                    String desc = elem.getAttribute("lname").replace(",", "");
                    return "[" + id + ":" + desc + "]";
                }
            }
        }
        return "[" + id + ":No qualifier data]";
    }

    /**
     * Build a combined string for a set of 4 qualifier IDs (mirrors getQualifierData(short[] ids, String exg) from the first file).
     */
    private String getQualifierData(short[] ids, String exg) {
        if (ids == null || ids.length == 0) {
            return "No qualifier data";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(getQualifierData(ids[i], exg));
        }
        return sb.toString();
    }

    /**
     * Convert a short representing the market phase to human-readable text (same approach as first file's getPhaseInfo()).
     */
    private String getPhaseInfo(short phaseInfo) {
        switch (phaseInfo) {
            case 0:  return "NoUpdate";
            case 1:  return "Closed";
            case 2:  return "Pre-Market";
            case 3:  return "Opening Auction";
            case 4:  return "Trading";
            case 5:  return "Mandatory Quote";
            case 6:  return "Volatility Interrupt";
            case 7:  return "Intraday Auction";
            case 8:  return "Closing Auction";
            case 9:  return "Alt Pricing";
            case 10: return "Post Market";
            case 11: return "Indicative Quote";
            default: return "UNKNOWN";
        }
    }

    /**
     * Build a combined string for 4 market-phase qualifiers, mirroring the first file's getPhaseInfo(short[]).
     */
    private String getPhaseInfo(short[] phaseInfo) {
        if (phaseInfo == null || phaseInfo.length == 0) {
            return "No market phase data";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phaseInfo.length; i++) {
            if (i > 0) sb.append(" | ");
            sb.append("[").append(phaseInfo[i]).append(":").append(getPhaseInfo(phaseInfo[i])).append("]");
        }
        return sb.toString();
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
        //System.out.println("Processing field item: " + Quote.FieldItem.getFieldName(item) + "   [ " + item.getId() + " ]");  
        
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
                break;
            case Quote.FieldItem.LRT_TYPE_OPEN:
                currentPricing.open = updatePriceField(item);
                break;
            case Quote.FieldItem.LRT_TYPE_HIGH:
                currentPricing.high = updatePriceField(item);
                currentPricing.highTime = item.getValueAsCidPriceFieldData().getExchangeTimeAsInstant();                
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
                yesEnergy.settle = item.getValueAsDouble();             
                break;
            case Quote.FieldItem.LRT_TYPE_MIDPRICE:
                yesEnergy.mid = item.getValueAsDouble();     
                currentPricing.mid = item.getValueAsDouble();
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
            default:            
                break;
        }                     
    }

    /**
     * Process last trade data (similar to the first file’s processLastTrade() method).
     */
    private void processLastTrade(FieldItem item) {
        fieldName = Quote.FieldItem.getFieldName(item);
        fieldFormat = item.getFormat().name();

        switch (item.getFormat()) {
            case DOUBLE:
                currentPricing.last = item.getValueAsDouble();
                break;
            case CIDDATA:
                CidData cidData = item.getValueAsCidData();
                currentPricing.lastLineID = cidData.getLineId();
                currentPricing.lastSeqNum = cidData.getSequenceNumberAsBigInteger();
                currentPricing.lastSeries = cidData.getSeries();
                currentPricing.lastExgTime = Instant.ofEpochSecond(cidData.getExchangeTimeNanoseconds());
                // Populate qualifiers as security qualifiers for LRT_TYPE_CIDDATA
                currentPricing.lastQualifiers = Arrays.copyOf(cidData.getSecurityQualifiers(), 4);
                break;
            case CIDFIELDDATA:
                CidFieldData cidFieldData = item.getValueAsCidFieldData();
                currentPricing.lastLineID = cidFieldData.getLineId();
                currentPricing.lastSeqNum = cidFieldData.getSequenceNumberAsBigInteger();
                currentPricing.lastSeries = cidFieldData.getSeries();
                currentPricing.lastExgTime = Instant.ofEpochSecond(cidFieldData.getExchangeTimeNanoseconds());
                currentPricing.lastExg = cidFieldData.getExchange();
                currentPricing.lastSize = cidFieldData.getSize();
                currentPricing.last = cidFieldData.getValue();
                currentPricing.lastQualifiers = Arrays.copyOf(cidFieldData.getQualifiers(), 4);
                break;
            case CIDPRICEFIELDDATA:
                CidPriceFieldData cidPriceFieldData = item.getValueAsCidPriceFieldData();
                currentPricing.lastLineID = cidPriceFieldData.getLineId();
                currentPricing.lastSeqNum = cidPriceFieldData.getSequenceNumberAsBigInteger();
                currentPricing.lastSeries = cidPriceFieldData.getSeries();
                currentPricing.lastExgTime = Instant.ofEpochSecond(cidPriceFieldData.getExchangeTimeNanoseconds());
                currentPricing.last = cidPriceFieldData.getValue();
                currentPricing.lastQualifiers = Arrays.copyOf(cidPriceFieldData.getQualifiers(), 4);
                break;
            default:
                break;
        }
    }

    /**
     * Process security status (similar to processSecurityStatus(...) in the first file).
     */
    private void processSecurityStatus(FieldItem item) {
        fieldName = Quote.FieldItem.getFieldName(item);
        fieldFormat = item.getFormat().name();

        CidData cidData = item.getValueAsCidData();
        currentPricing.securityStatus = Arrays.copyOf(cidData.getSecurityQualifiers(), 4);
    }

    /**
     * Process the bid side (similar to the first file’s processBidData(...)).
     */
    private void processBidData(FieldItem item) {
        fieldName = Quote.FieldItem.getFieldName(item);
        fieldFormat = item.getFormat().name();

        switch (item.getFormat()) {
            case DOUBLE:
                currentPricing.bid = item.getValueAsDouble();
                break;
            case CIDDATA:
                CidData cidData = item.getValueAsCidData();
                currentPricing.bidLineID = cidData.getLineId();
                currentPricing.bidSeqNum = cidData.getSequenceNumberAsBigInteger();
                currentPricing.bidSeries = cidData.getSeries();
                currentPricing.bidExgTime = Instant.ofEpochSecond(cidData.getExchangeTimeNanoseconds());
                currentPricing.bidQualifiers = Arrays.copyOf(cidData.getSecurityQualifiers(), 4);
                break;
            case CIDFIELDDATA:
                CidFieldData cidFieldData = item.getValueAsCidFieldData();
                currentPricing.bidLineID = cidFieldData.getLineId();
                currentPricing.bidSeqNum = cidFieldData.getSequenceNumberAsBigInteger();
                currentPricing.bidSeries = cidFieldData.getSeries();
                currentPricing.bidExgTime = Instant.ofEpochSecond(cidFieldData.getExchangeTimeNanoseconds());
                currentPricing.bidExg = cidFieldData.getExchange();
                currentPricing.bidSize = cidFieldData.getSize();
                currentPricing.bid = cidFieldData.getValue();
                currentPricing.bidQualifiers = Arrays.copyOf(cidFieldData.getQualifiers(), 4);
                break;
            case CIDPRICEFIELDDATA:
                CidPriceFieldData cidPriceFieldData = item.getValueAsCidPriceFieldData();
                currentPricing.bidLineID = cidPriceFieldData.getLineId();
                currentPricing.bidSeqNum = cidPriceFieldData.getSequenceNumberAsBigInteger();
                currentPricing.bidSeries = cidPriceFieldData.getSeries();
                currentPricing.bidExgTime = Instant.ofEpochSecond(cidPriceFieldData.getExchangeTimeNanoseconds());
                currentPricing.bid = cidPriceFieldData.getValue();
                currentPricing.bidQualifiers = Arrays.copyOf(cidPriceFieldData.getQualifiers(), 4);
                break;
            default:
                break;
        }
    }

    /**
     * Process the ask side (similar to the first file’s processAskData(...)).
     */
    private void processAskData(FieldItem item) {
        fieldName = Quote.FieldItem.getFieldName(item);
        fieldFormat = item.getFormat().name();

        switch (item.getFormat()) {
            case DOUBLE:
                currentPricing.ask = item.getValueAsDouble();
                break;
            case CIDDATA:
                CidData cidData = item.getValueAsCidData();
                currentPricing.askLineID = cidData.getLineId();
                currentPricing.askSeqNum = cidData.getSequenceNumberAsBigInteger();
                currentPricing.askSeries = cidData.getSeries();
                currentPricing.askExgTime = Instant.ofEpochSecond(cidData.getExchangeTimeNanoseconds());
                currentPricing.askQualifiers = Arrays.copyOf(cidData.getSecurityQualifiers(), 4);
                break;
            case CIDFIELDDATA:
                CidFieldData cidFieldData = item.getValueAsCidFieldData();
                currentPricing.askLineID = cidFieldData.getLineId();
                currentPricing.askSeqNum = cidFieldData.getSequenceNumberAsBigInteger();
                currentPricing.askSeries = cidFieldData.getSeries();
                currentPricing.askExgTime = Instant.ofEpochSecond(cidFieldData.getExchangeTimeNanoseconds());
                currentPricing.askExg = cidFieldData.getExchange();
                currentPricing.askSize = cidFieldData.getSize();
                currentPricing.ask = cidFieldData.getValue();
                currentPricing.askQualifiers = Arrays.copyOf(cidFieldData.getQualifiers(), 4);
                break;
            case CIDPRICEFIELDDATA:
                CidPriceFieldData cidPriceFieldData = item.getValueAsCidPriceFieldData();
                currentPricing.askLineID = cidPriceFieldData.getLineId();
                currentPricing.askSeqNum = cidPriceFieldData.getSequenceNumberAsBigInteger();
                currentPricing.askSeries = cidPriceFieldData.getSeries();
                currentPricing.askExgTime = Instant.ofEpochSecond(cidPriceFieldData.getExchangeTimeNanoseconds());
                currentPricing.ask = cidPriceFieldData.getValue();
                currentPricing.askQualifiers = Arrays.copyOf(cidPriceFieldData.getQualifiers(), 4);
                break;
            default:
                break;
        }
    }

    /**
     * Process the entire Quote, analogous to the first file’s processData(Quote).
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

        // Determine the listed exchange and process it if not already set
        FieldItem exchg = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_LISTEXG);
        listedExchange = (exchg != null) ? exchg.getValueAsString() : "----";

        // Determine the instrument to which this update applies
        FieldItem sym = quote.getFieldItem((short) Quote.FieldItem.LRT_TYPE_KEY);
        instrument = (sym != null) ? sym.getValueAsString() : "";
        
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
    }

    /**
     * Attempt to infer the exchange from the instrument if listedExchange is still default.
     */
    private void processListedExchange() {
        if ("----".equals(listedExchange) && instrument != null) {
            String[] parts = instrument.split("-");
            if (parts.length > 1) {
                listedExchange = parts[1];
            }
        }
    }

    /**
     * Produce the final output, either CSV or a single-line console output, 
     * matching the style from the first file’s displayFormattedData(...).
     */
    private void displayFormattedData(Date lastExgTime, Date bidExgTime, Date askExgTime) {
        try {
            if (!consoleSample.displayData()) {
                // Use Instant.toEpochMilli() for numeric representation of Instant
                if (!columnsPrepared) {
                    createColumnHeadings();
                }

                if (!columnsPrinted) {
                    System.out.println(String.join(",", columnHeadings));
                    columnsPrinted = true;
                }
                
                // Convert exchange times to human-readable format
                Instant now = Instant.now();
                SimpleDateFormat sdf = createDateFormatter();
                Date currTime = Date.from(now);
                
                // Include YES Energy data only for CSV output
                System.out.printf(
                    // Metadata
                    "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s," + // Current Time (epoch), Local Time, Market State, Phase, Status, etc.
                    // OHLC Data
                    "%.5f,%.5f,%.5f,%.5f," +                // Open, High, Low, Close
                    // Last Trade Data
                    "%s,%s,%s,%s,%.5f,%.5f,%.5f,%s,%d,%d,%d," + 
                    // Bid Data
                    "%s,%s,%s,%.5f,%.5f,%s,%d,%d,%d," + 
                    // Ask Data
                    "%s,%s,%s,%.5f,%.5f,%s,%d,%d,%d," +     
                    // YES Energy Data (Set 1)
                    "%.5f,%.5f,%.5f," +             // settle, mid, volume
                    // YES Energy Data (Set 2)
                    "%.5f,%.5f,%.5f," +             // day ahead lmp, day ahead congestion, day ahead loss
                    // YES Energy Data (Set 3)
                    "%.5f,%.5f,%.5f," +             // rt prices lmp, rt prices congestion, rt prices loss 
                    // YES Energy Data (Set 4)
                    "%.5f,%.5f,%.5f," +             // rt load, day ahead load, rt final load
                    // YES Energy Data (Set 5)
                    "%.5f,%.5f,%.5f," +             // rt lmp, rt congestion, rt loss
                    // YES Energy Data (Set 6)
                    "%.5f,%.5f,%.5f," +             // hour ahead lmp, hour ahead congestion, hour ahead loss
                    // YES Energy Data (Set 7)
                    "%.5f,%.5f,%.5f,%.5f\n",        // load, rt 15m prices lmp, rt 15m prices congestion, rt 15m prices loss
                    
                    utilities.convertTime(System.currentTimeMillis()),
                    sdf.format(currTime),
                    marketUpdateInfo[1],
                    currentPricing.mphaseQuals,
                    marketUpdateInfo[0],
                    fieldName,
                    fieldFormat,
                    instrument,
                    isCIDUpdate ? "*" : "",
                    cidType != null ? cidType : "N/A",
                    currentPricing.securityStats != null ? currentPricing.securityStats : "N/A",
                    companyName != null ? companyName : "N/A",

                    // OHLC Data
                    currentPricing.open != 0.0 ? currentPricing.open : 0.0,
                    currentPricing.high != 0.0 ? currentPricing.high : 0.0,
                    currentPricing.low != 0.0 ? currentPricing.low : 0.0,
                    currentPricing.close != 0.0 ? currentPricing.close : 0.0,

                    // Last Trade Data
                    utilities.convertTime(currentPricing.lastExgTime != null ? currentPricing.lastExgTime.toEpochMilli() : 0),
                    sdf.format(lastExgTime != null ? lastExgTime : Date.from(Instant.EPOCH)),
                    currentPricing.lastQuals != null ? currentPricing.lastQuals : "N/A",
                    currency != null ? currency : "N/A",
                    currentPricing.accumVolume != 0.0 ? currentPricing.accumVolume : 0.0,
                    currentPricing.last != 0.0 ? currentPricing.last : 0.0,
                    currentPricing.lastSize != 0.0 ? currentPricing.lastSize : 0.0,
                    currentPricing.lastExg != null ? currentPricing.lastExg : "N/A",
                    currentPricing.lastSeries,
                    currentPricing.lastSeqNum != null ? currentPricing.lastSeqNum : BigInteger.ZERO,
                    currentPricing.lastLineID,

                    // Bid Data
                    utilities.convertTime(currentPricing.bidExgTime != null ? currentPricing.bidExgTime.toEpochMilli() : 0),
                    sdf.format(bidExgTime != null ? bidExgTime : Date.from(Instant.EPOCH)),
                    currentPricing.bidQuals != null ? currentPricing.bidQuals : "N/A",
                    currentPricing.bid != 0.0 ? currentPricing.bid : 0.0,
                    currentPricing.bidSize != 0.0 ? currentPricing.bidSize : 0.0,
                    currentPricing.bidExg != null ? currentPricing.bidExg : "N/A",
                    currentPricing.bidSeries,
                    currentPricing.bidSeqNum != null ? currentPricing.bidSeqNum : BigInteger.ZERO,
                    currentPricing.bidLineID,

                    // Ask Data
                    utilities.convertTime(currentPricing.askExgTime != null ? currentPricing.askExgTime.toEpochMilli() : 0),
                    sdf.format(askExgTime != null ? askExgTime : Date.from(Instant.EPOCH)),
                    currentPricing.askQuals != null ? currentPricing.askQuals : "N/A",
                    currentPricing.ask != 0.0 ? currentPricing.ask : 0.0,
                    currentPricing.askSize != 0.0 ? currentPricing.askSize : 0.0,
                    currentPricing.askExg != null ? currentPricing.askExg : "N/A",
                    currentPricing.askSeries,
                    currentPricing.askSeqNum != null ? currentPricing.askSeqNum : BigInteger.ZERO,
                    currentPricing.askLineID,

                    // YES Energy Data (Set 1)
                    yesEnergy.settle != 0.0 ? yesEnergy.settle : 0.0,
                    yesEnergy.mid != 0.0 ? yesEnergy.mid : 0.0,
                    yesEnergy.volume != 0.0 ? yesEnergy.volume : 0.0,

                    // YES Energy Data (Set 2)
                    yesEnergy.dayAheadLMP != 0.0 ? yesEnergy.dayAheadLMP : 0.0,
                    yesEnergy.dayAheadCongestion != 0.0 ? yesEnergy.dayAheadCongestion : 0.0,
                    yesEnergy.dayAheadLoss != 0.0 ? yesEnergy.dayAheadLoss : 0.0,
                    
                    // YES Energy Data (Set 3)
                    yesEnergy.rtPricesLMP != 0.0 ? yesEnergy.rtPricesLMP : 0.0,
                    yesEnergy.rtPricesCongestion != 0.0 ? yesEnergy.rtPricesCongestion : 0.0,
                    yesEnergy.rtPricesLoss != 0.0 ? yesEnergy.rtPricesLoss : 0.0,
                    
                    // YES Energy Data (Set 4)
                    yesEnergy.rtLoad != 0.0 ? yesEnergy.rtLoad : 0.0,
                    yesEnergy.dayAheadLoad != 0.0 ? yesEnergy.dayAheadLoad : 0.0,
                    yesEnergy.rtFinalLoad != 0.0 ? yesEnergy.rtFinalLoad : 0.0,

                    // YES Energy Data (Set 5)
                    yesEnergy.rtLMP != 0.0 ? yesEnergy.rtLMP : 0.0,
                    yesEnergy.rtCongestion != 0.0 ? yesEnergy.rtCongestion : 0.0,
                    yesEnergy.rtLoss != 0.0 ? yesEnergy.rtLoss : 0.0,
                    
                    // YES Energy Data (Set 6)
                    yesEnergy.hourAheadLMP != 0.0 ? yesEnergy.hourAheadLMP : 0.0,
                    yesEnergy.hourAheadCongestion != 0.0 ? yesEnergy.hourAheadCongestion : 0.0,
                    yesEnergy.hourAheadLoss != 0.0 ? yesEnergy.hourAheadLoss : 0.0,

                    // YES Energy Data (Set 7)
                    yesEnergy.load != 0.0 ? yesEnergy.load : 0.0,
                    yesEnergy.rt15mPricesLMP != 0.0 ? yesEnergy.rt15mPricesLMP : 0.0,
                    yesEnergy.rt15mPricesCongestion != 0.0 ? yesEnergy.rt15mPricesCongestion : 0.0,
                    yesEnergy.rt15mPricesLoss != 0.0 ? yesEnergy.rt15mPricesLoss : 0.0
                );
            } else {
                System.out.printf(
                    "SYMBOL: %-20s MARKET STATE: %-15s MARKET PHASE: %-42s TYPE: %-42s " +
                    "ASK: %-12.5f BID: %-12.5f LAST: %-12.5f OPEN: %-12.5f HIGH: %-12.5f LOW: %-12.5f CLOSE: %-12.5f\n",
                    instrument, marketUpdateInfo[1], currentPricing.mphaseQuals, marketUpdateInfo[0],
                    currentPricing.ask, currentPricing.bid, currentPricing.last,
                    currentPricing.open, currentPricing.high, currentPricing.low, currentPricing.close
                );
            }
        } catch (IllegalFormatConversionException e) {
            System.err.println("<<< Illegal format conversion while printing data: " + e.getMessage());
        }
    }

    @Override
    public void onAdded(SymbolEvent event) {
        // Method intentionally left unimplemented (per first file).
    }

    @Override
    public void onDeleted(SymbolEvent event) {
        // Method intentionally left unimplemented (per first file).
    }

    public void onError(StatusEvent event) {
        System.out.println(event.getStatusString());
    }

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
        System.out.println(event.getSymbol() + ",NO DATA");
    }

    /**
     * The first file's onUpdate(...) simply calls processData(quote).
     */
    @Override
    public void onUpdate(QuoteEvent event) {
        Quote quote = event.getQuote();
        processData(quote);
    }

    @Override
    public void onError(TriggeredQuoteEvent event) {
    	if (event.getStatus() == DbcCodes.DBCAPI_ERROR.TRIGGER_JSON_INVALID)
    		System.out.println("Invalid JSON format for trigger data." + event.toString());
    	
    	
        if (consoleSample.displayData()) {
            System.out.println("ERROR: Trigger error encountered.\n");
            System.out.printf("             Trigger: %s\n", event.getTriggerName());
            System.out.printf("       Turnaround ID: %d\n", event.getTurnAround());
            System.out.printf("              Status: [ %d ] %s\n\n",
                              event.getStatusCode(), event.getStatusString());
        }
    }

    @Override
    public void onTriggerAcknowledged(TriggeredQuoteEvent event) {
        // Not implemented in the sample
    }

    @Override
    public void onTriggerAdded(TriggeredQuoteEvent event) {
        consoleSample.addTriggerName(event.getTriggerName(), triggerCount);
        ++triggerCount;
   
    }

    @Override
    public void onTriggerRemoved(TriggeredQuoteEvent event) {
        // Not implemented in the sample
    }
}
