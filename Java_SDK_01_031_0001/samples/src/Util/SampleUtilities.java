package Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import com.esignal.jstandard.beans.ConnectionFlags;
import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.beans.quote.MSTATUS;
import com.esignal.jstandard.beans.quote.Status;
import com.esignal.jstandard.managers.ResourceManagerFactory;

public class SampleUtilities {
	private static volatile SampleUtilities sampleUtilities;
    
    private SampleUtilities() {
	}
	
    public static SampleUtilities getInstance() {
        if (sampleUtilities == null) {
           synchronized (SampleUtilities.class) {
              if (sampleUtilities == null) {
            	  sampleUtilities = new SampleUtilities();
              }
           }
        }
        return sampleUtilities;
    }

    /**
     * Retrieves the name corresponding to the given category code.
     *
     * @param categoryCode The category code for the update.
     * @return The corresponding category name for the given category code.
     */
    public String getCategoryName(char categoryCode) {
        switch (categoryCode) {
        	case 'A':
        		return "CATEGORY_INDICE";
        	case 'B':
        		return "CATEGORY_STOCK";
        	case 'C':
        		return "CATEGORY_INTSTOCK";
        	case 'D':
        		return "CATEGORY_DATASET";
        	case 'E':
        		return "CATEGORY_GENERIC_DATA";
        	case 'F':
        		return "CATEGORY_FILE";
        	case 'G':
        		return "CATEGORY_STOCKOPTION";
        	case 'H':
        		return "CATEGORY_FUTURE";
        	case 'I':
        		return "CATEGORY_FUTUREOPTION";
        	case 'J':
        		return "CATEGORY_CURRENCYOPTION";
        	case 'K':
        		return "CATEGORY_MUTUALFUND";
        	case 'L':
        		return "CATEGORY_MONEYFUND";
        	case 'M':
        		return "CATEGORY_NALERT";
        	case 'N':
        		return "CATEGORY_CIDR_ALLFIELDS";
        	case 'O':
        	    return "CATEGORY_PING";
        	case 'P':
        		return "CATEGORY_PING";
        	case 'Q':
        		return "CATEGORY_CFTC";
        	case 'U':
        		return "CATEGORY_GAINER";
        	case 'V':
        		return "CATEGORY_HEADLINE";
        	case 'W':
        		return "CATEGORY_FORWARD";
			case 'X':
				return "CATEGORY_CIDR";
			case 'Y':
				return "CATEGORY_SPREAD";
			case 'Z':
				return "CATEGORY_INFO";
			case '1':
				return "CATEGORY_MESSAGE";
			case '2':
				return "CATEGORY_DAILY";
			case '3':
				return "CATEGORY_DAILYFUND";
			case '4':
				return "CATEGORY_TICK";
			case '5':
				return "CATEGORY_TICKFUND";
			case '6':
				return "CATEGORY_PASS";
			case '7':
				return "CATEGORY_BINARYROS";
			case '8':
				return "CATEGORY_NSTOCK";
			case '9':
				return "CATEGORY_NINE";
			case '*':
				return "CATEGORY_WRAPPER";
			case '?':
				return "CATEGORY_QUESTION";
			case '^':
				return "CATEGORY_STAT";
			case '_':
				return "CATEGORY_ACTION";
			case '#':
				return "CATEGORY_MAGICNUM";
			default:
				return "CATEGORY_UNKNOWN";
        }
    }

    /**
     * Retrieves the name corresponding to the given subcategory code.
     *
     * @param subcategoryCode The subcategory code for the update.
     * @return The corresponding subcategory name for the given subcategory code.
     */
    public String getSubcategoryName(char subcategoryCode) {
        switch (subcategoryCode) {
        	case 'A':
        		return "SUBCATEGORY_FCAST";
        	case 'B':
        		return "SUBCATEGORY_ENSBL";
        	case 'C':
        		return "SUBCATEGORY_CASHENERGY";
        	case 'D':
        		return "SUBCATEGORY_OBSV";
        	case 'E':
        		return "SUBCATEGORY_WASDE";
        	case 'F':
        		return "SUBCATEGORY_FX";
        	case 'G':
        		return "SUBCATEGORY_GB";
        	case 'H':
        		return "SUBCATEGORY_FUTURE";
        	case 'I':
        		return "SUBCATEGORY_FUTUREOPTION";
        	case 'J':
        		return "SUBCATEGORY_CURRENCYOPTION";
        	case 'K':
        		return "SUBCATEGORY_HK";
        	case 'M':
        		return "SUBCATEGORY_CLIMO";
        	case 'N':
        		return "SUBCATEGORY_MIFID_COT";
        	case 'P':
        		return "SUBCATEGORY_COREFUTOPT";
        	case 'Q':
        		return "SUBCATEGORY_CORESPREAD";
        	case 'R':
        		return "SUBCATEGORY_REFINERY";
        	case 'S':
        		return "SUBCATEGORY_COREFUT";
        	case 'T':
        		return "SUBCATEGORY_COT";
        	case 'U':
        		return "SUBCATEGORY_IFND";
        	case 'V':
        		return "SUBCATEGORY_AGG_DATA";
        	case 'W':
        		return "SUBCATEGORY_WEATHER";
			case 'X':
				return "SUBCATEGORY_FUNDAMENTAL";
			case 'Y':
				return "SUBCATEGORY_BOND";
			case 'Z':
				return "SUBCATEGORY_INTERNATIONAL";
			case '1':
				return "SUBCATEGORY_ENTSOG";
			case '2':
				return "SUBCATEGORY_ICECRED";
			case '3':
				return "SUBCATEGORY_KPLER";
			case '4':
				return "SUBCATEGORY_DJCAL";
			case '5':
				return "SUBCATEGORY_ENERGYSTOCK";
			default:
				return "SUBCATEGORY_UNKNOWN";
        }
    }

    /**
     * Reads symbols from the specified file, one per line.
     * 
     * @param filePath The path to the file containing symbols.
     * @return A String[] array of symbols.
     */
    public String[] readSymbolsFromFile(String filePath) {
        List<String> symbols = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    symbols.add(trimmed);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading symbols from file: " + e.getMessage(), e);
        }
        return symbols.toArray(new String[0]);
    }

    /**
     * Retrieves JStandard API version and release date from the manifest.
     * 
     * @param factory Instance of ResourceManagerFactory
     * @return Array containing API version and release date
     */
    public String[] getJStandardVersion(ResourceManagerFactory factory) {
        String[] versionInfo = {"Unknown", "Unknown"};
        try {
            String path = factory.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ");
            JarFile jarFile = new JarFile(path);
            Manifest manifest = jarFile.getManifest();
            jarFile.close();

            Map<String, Attributes> apiAttributes = manifest.getEntries();
            Attributes apiInfo = apiAttributes.get("com/esignal/jstandard/");
            for (Object key : apiInfo.keySet()) {
                if (key.toString().contains("Implementation-Version")) {
                    versionInfo[0] = apiInfo.getValue((Name) key);
                } else if (key.toString().contains("Implementation-Timestamp")) {
                    versionInfo[1] = apiInfo.getValue((Name) key);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading version info: " + e.getMessage());
        }
        return versionInfo;
    }

    /**
     * Configures connection type based on the TLS mode.
     */
    public SockType setupConnectionType(String tlsConnectionMode, StringBuilder tlsDescription) {
        SockType connType;
 	   try {
            connType = SockType.getSockType(Integer.parseInt(tlsConnectionMode));
            switch (connType) {
                case SOCKTYPE_LEGACY:
                    tlsConnectionMode = "Standard TCP/IP (non-TLS)";
                    break;
                case SOCKTYPE_TLS:
                    tlsConnectionMode = "Secure TLS Connection";
                    break;
                case SOCKTYPE_TLS_PLUS:
                    tlsConnectionMode = "Secure TLS+ Not Supported; using Secure TLS Connection";
                    connType = SockType.SOCKTYPE_TLS;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            tlsConnectionMode = "Unknown Type Specified; using Standard TCP/IP (non-TLS)";
            connType = SockType.SOCKTYPE_LEGACY;
        }
 	   tlsDescription.setLength(0);
 	   tlsDescription.append(tlsConnectionMode);
 	   return connType;
    }

    /**
     * Retrieves connection settings from either the command line or properties
     * file, and returns to the calling function.
     * 
     * @return A fully populated ConnectionSettings object.
     */
    public ConnectionSettings getConnectionSettings(String host, String username, String password, SockType connType,
    												Properties sampleProps) {
        ConnectionSettings connectionSettings =
                new ConnectionSettings(host, username, password, connType);

        if (Boolean.parseBoolean(sampleProps.getProperty("samples.connection.useproxy"))) {
            String proxyusername = "";
            String proxypassword = "";
            connectionSettings.setProxyInfo(
                    sampleProps.getProperty("samples.connection.proxyhost"),
                    Integer.parseInt(sampleProps.getProperty("samples.connection.proxyport")),
                    proxyusername,
                    proxypassword
            );
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

        // This sample sets the DBCAPI_FLAG_EXPAND_DISPLAY_KEY flag to true, which will cause the API
        // to expand the LRT_TYPE_DISPLAY_KEY field for all updates when it is provided.  This flag 
        // applies to Streaming and Snapshot data provided by the QuoteManager.  The default value behavior
        // is for the API to not expand the LRT_TYPE_DISPLAY_KEY field unless it is for a "snapshot" request 
        // or for a "refresh" update.
        connectionSettings.setFlags(ConnectionFlags.CONNECTION_FLAGS.DBCAPI_FLAG_EXPAND_DISPLAY_KEY.getCode());
        
        return connectionSettings;
    }
    
    
	/**
	 * Converts a long value representing milliseconds since the epoch to a
	 * human-readable date/time.
	 * 
	 * @param milliseconds The number of milliseconds since the epoch.
	 * @return A human-readable date/time string.
	 */
    public String convertTime(long milliseconds) {
    	if (milliseconds == 0)
    		return "----";
    	
        // Convert milliseconds to an Instant
        Instant instant = Instant.ofEpochMilli(milliseconds);

        // Format the Instant into a human-readable date/time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                                                       .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

	/**
	 * Converts a long value representing milliseconds since the epoch to a
	 * human-readable date/time.
	 * 
	 * @param milliseconds The number of milliseconds since the epoch.
	 * @return A human-readable date/time string.
	 */
    public String convertINTERVALDateTime(BigInteger dateTime) {
        if (dateTime == null || dateTime.equals(BigInteger.ZERO))
            return "----";
    	
    	String dateTimeString = dateTime.toString();
    	
        // Format the dateTime into a human-readable date/time
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime convertedDateTime = LocalDateTime.parse(dateTimeString,inputFormat);
        
        // Format to desired output
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return convertedDateTime.format(outputFormat);
    }

    /**
     * Gets the Java SDK version being used for the project.
     * This is not the version of the JStandard API.
     * 
     * @return Array containing API version, vendor, and location.
     */
    public String[] getJavaVersion() {
    	String[] versionInfo = {"Unknown", "Unknown", "Unknown"};
    	
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaHome = System.getProperty("java.home");

        versionInfo[0] = javaVersion;
        versionInfo[1] = javaVendor;
        versionInfo[2] = javaHome;
        
        return versionInfo;
    }  
    
    /**
     * Returns market update type and market phase based on the provided status arrays.
     *
     * @param status The status array containing encoded market information.	[ LRT_TYPE_STATUS ]
     * @param mstatus The extended status flags represented as a long integer.	[ LRT_TYPE_MSTATUS ]
     * @return A Vector of char arrays: [updateType, marketPhase]
     */
    public String[] getMarketStatus(char[] status, Long mstatus) {
        String updateType = "----";
        String marketState = "----";        

        // Determine if the market is pre-market or Form T (after market)
        boolean bPreMarket = mstatus != null && ((mstatus & MSTATUS.ROSPREMARKET.code()) != 0 || (mstatus & MSTATUS.ROSPREMARKETQUOTE.code()) != 0);
        boolean bFormT = mstatus != null && ((mstatus & MSTATUS.ROSFORMT.code()) != 0);

        // Determine Market Phase
        if ((status[0] & Status.STATUS_BYTE_ONE.ROSOPEN.getValue()) != 0) {
        	marketState = "OPEN";
        } else if (bPreMarket) {
        	marketState = "PRE-MARKET";
        } else if (bFormT) {
        	marketState = "CLOSED (AFTER MARKET)";
        } else if ((status[0] & Status.STATUS_BYTE_ONE.ROSREALTIMETRADEORBIDASK.getValue()) != 0) {
            // It's possible for an LRT_TYPE_MSTATUS field to return with no flags set
            // if the update type is not a ROSREALTIMETRADEORBIDASK
            // In that case we indicate it is not a real-time market update
            // This can be the case for Mutual Funds and Money Market Funds as well as
            // for snapshots, but not guaranteed to be the case.
        	marketState = "NON-MARKET/NOT REALTIME";
        } else {
			// *************************************************************************************************
			// IMPORTANT:
			// Mutual Funds and Money Market Funds only get updates once a day and do not have a Market Status.
			// Electronic Transfer Funds (ETFs) trade regularly and do have a Market Status.
			// *************************************************************************************************
			marketState = "NOT APPLICABLE";
        }

        // Determine Update Type
        if ((status[0] & Status.STATUS_BYTE_ONE.ROSREALTIMETRADEORBIDASK.getValue()) != 0) {
        	// ROSREALTIMETRADEORBIDASK will be on for *any* real-time update
            // This is a real time trade, bid, ask, formt or qualified trade
            // *****************************************************************************************
            // IMPORTANT:
            // Updates for delayed feed offerings (ie Delayed NYSE) use the same flags as real-time 
            // updates. Delayed updates follow the delay rules for each exchange offering delayed data.
            //
            //         =======================================================================
            //         == The order of precedence is important for processing update types. ==
            //         == because more than one flag may be on for an update.               ==
            //         ========================================================================
            // *****************************************************************************************
            
            // An update can be one of three possible trades, a bid only update, an ask only update, or a bid/ask update.
            // An update cannot be both a trade and a quote (bid, ask)
            if ((status[5] & Status.STATUS_BYTE_SIX.WROSRTFORMT.getValue()) != 0) {
                // The value for this trade will be found in the LRT_TYPE_QUALIFIEDTRADE field
                updateType = "QUALIFIED TRADE";    // This is an after hours trade 
                // **************************************************************************************************
                // note:  FORMT is a subset of QUALIFIED TRADES.  Therefore, all FORMT trades are qualified trades.
                //        For backward compatibility, the LRT_TYPE_FORMT field will also contain the value for this
                //        update, however, the LRT_TYPE_QUALIFIEDTRADE field includes qualifiers that provide 
                //        additional details regarding the update, including if the update is a FORMT.
                //      
                //        The WROSRTQUALIFIEDTRADE and WROSRTTRADE flags will also be on, however, since the 
                //        WROSRTFORMT flag is on we know this to be a FormT (After Hours) trade.
                // **************************************************************************************************
            } else if ((status[5] & Status.STATUS_BYTE_SIX.WROSRTQUALIFIEDTRADE.getValue()) != 0) {
                // **************************************************************************************************
                // note:  The value for this trade will be found in the LRT_TYPE_QUALIFIEDTRADE field
                //
                //        The WROSRTTRADE flags will also be on, however, since the WROSRTQUALIFIEDTRADE is on
                //        we know this to be a qualified trade.
                // **************************************************************************************************
                updateType = "QUALIFIED TRADE";
            } else if ((status[5] & Status.STATUS_BYTE_SIX.WROSRTTRADE.getValue()) != 0) {
                // The value for this trade will be found in the LRT_TYPE_LAST field
                updateType = "TRADE";

            // Check for quotes (bids/asks)
            } else if ((status[0] & Status.STATUS_BYTE_ONE.ROSBIDASKMORECURRENT.getValue()) != 0) {
                boolean bid = (status[5] & Status.STATUS_BYTE_SIX.WROSRTBID.getValue()) != 0;
                boolean ask = (status[5] & Status.STATUS_BYTE_SIX.WROSRTASK.getValue()) != 0;
                if (bid && ask) {
                    updateType = "BID/ASK";
                } else if (bid) {
                    updateType = "BID";
                } else if (ask) {
                    updateType = "ASK";
                }

            // Check for fund trades
            } else {
                // Since the ROSREALTIMETRADEORBIDASK flag is on and everything else is off we can treat this as a real-time trade.
                // This can occur for funds.
                // The value for this update will be found in the LRT_TYPE_NAV field.
                updateType = "TRADE"; // Default case for funds
            }
        } else {
            // This is not a real-time update; this is a refresh for the given instrument.
            // It is recommended that consuming clients use the data to update existing displays, databases, 
            // computations, etc.
            //
            // A client may have missed a previous "real-time" update due to connection loss, packet drops, or 
            // Internet congestion.  A "refresh" message contains the most current data for the instrument and 
            // should be used to ensure client's do not miss anything.
            //
            // **************************************************************************************************
            // note:  SNAPSHOT requests always indicate refresh as their update type.
            // *************************************************************************************************
            updateType = "REFRESH";
        }

        String[] result = {updateType,marketState};

        return result;
    }
}
