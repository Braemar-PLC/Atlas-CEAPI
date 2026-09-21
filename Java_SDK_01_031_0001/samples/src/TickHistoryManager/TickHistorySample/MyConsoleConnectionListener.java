package TickHistoryManager.TickHistorySample;

import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.managers.dbc.DbcCodes.*;

/**
 * Provides methods for handling various connection, disconnection, and error
 * events for the Tick History Sample.
 * 
 * <p>Implements the {@link ConnectionListener} interface to handle specific
 * events such as connection establishment, disconnection, and errors in the
 * connection lifecycle.</p>
 * 
 * @author ICE Data Services - Developer Support
 */
public class MyConsoleConnectionListener implements ConnectionListener {

    private final TickHistorySample consoleSample;
    private boolean exitSample;
    private boolean connectionEstablished;

    /**
     * Constructor to initialize the listener with the TickHistorySample instance.
     * 
     * @param consoleSample The main application object that interacts with the connection listener.
     */
    public MyConsoleConnectionListener(TickHistorySample consoleSample) {
        this.consoleSample = consoleSample;
        this.exitSample = false;
        this.connectionEstablished = false;
    }

    /**
     * Called when a connection to the server is successfully established.
     * 
     * <p>Displays connection details and initiates data requests based on the mode of operation.</p>
     * 
     * @param event The event containing connection details.
     */
    @Override
    public void onConnected(ConnectionEvent event) {
        if (consoleSample.displayData() && !connectionEstablished) {
            connectionEstablished = true;
            printConnectionDetails(event);
        }

        // Determine the type of request to execute
        if (consoleSample.isDictionaryRequest()) {
            consoleSample.requestDictionary();
        } else {
            consoleSample.requestHistory(consoleSample.getCurrentIndex());
        }
    }

    /**
     * Called when a connection attempt is in progress.
     * 
     * <p>No specific actions are performed for this event in this implementation.</p>
     * 
     * @param event The event containing connection progress details.
     */
    @Override
    public void onConnecting(ConnectionEvent event) {
        // Connection is in progress (No action required in this sample)
    }

    /**
     * Called when the connection is lost.
     * 
     * <p>Resets the connection state and attempts to clean up resources if necessary.</p>
     * 
     * @param event The event containing disconnection details.
     */
    @Override
    public void onDisconnected(ConnectionEvent event) {
        connectionEstablished = false;
        try {
            if (exitSample) {
                // disconnect is false when the application has already lost the connection to the service.
                consoleSample.exitSample(false);
            }
        } catch (Exception e) {
            System.err.println("Error during sample exit: " + e.getMessage());
        }
    }

    /**
     * Called when the connection is in the process of being disconnected.
     * 
     * <p>Handles any pre-disconnection logic based on the event details.</p>
     * 
     * @param event The event containing disconnection progress details.
     */
    @Override
    public void onDisconnecting(ConnectionEvent event) {
        if (connectionEstablished) {
            handleDisconnection(event);
        }
    }

    /**
     * Called when an error occurs during the connection lifecycle.
     * 
     * <p>Handles the error by displaying appropriate messages and attempting recovery.</p>
     * 
     * @param event The event containing error details.
     */
    @Override
    public void onError(ConnectionEvent event) {
        connectionEstablished = false;
        handleError(event);
    }

    /**
     * Prints details about the established connection.
     * 
     * @param event The event containing connection details.
     */
    private void printConnectionDetails(ConnectionEvent event) {
        System.out.println("Connected to the Intraday History Service.");
        System.out.println("Server Connection: " + event.getConnectedHost());
        System.out.println("Your IP Address: " + event.getConnectionLocalIp() + "\n");
    }

    /**
     * Handles disconnection events, including determining the reason for disconnection
     * and whether to attempt reconnection.
     * 
     * @param event The event containing disconnection details.
     */
    private void handleDisconnection(ConnectionEvent event) {
    	if (event.getStatus() != DBCAPI.SUCCESS) {
	        try {
	            DBCAPI_ERROR errorCode = DBCAPI_ERROR.valueOf(event.getStatus().name());
	            printDisconnectionHeader(event);
	            printDisconnectionReason(errorCode);
	            printReconnectInstructions(errorCode);
	        } catch (Exception ex) {
	            printException(event, ex);
	        }
    	}
    }

	/**
     * Handles error events by determining the cause and displaying recovery instructions.
     * 
     * @param event The event containing error details.
     */
    private void handleError(ConnectionEvent event) {
        try {
            printErrorHeader(event);
            printErrorReason(DBCAPI_ERROR.valueOf(event.getStatus().name()));
            printReconnectInstructions(DBCAPI_ERROR.valueOf(event.getStatus().name()));
        } catch (Exception ex) {
            printException(event, ex);
        }
    }

    /**
     * Prints the header for disconnection events.
     * 
     * @param event The event containing disconnection details.
     */
    private void printDisconnectionHeader(ConnectionEvent event) {
        System.out.println("-----------------------------------------------------------------------------");
        System.out.println(event.getStatusType() + ": " + event.getStatusString() + "\n");
    }

    /**
     * Prints the header for error events.
     * 
     * @param event The event containing error details.
     */
    private void printErrorHeader(ConnectionEvent event) {
        System.out.println("-----------------------------------------------------------------------------");
        System.out.println(event.getStatusType() + ": " + event.getStatusString() + "\n");
    }

    /**
     * Prints the reason for disconnection based on the error code.
     * 
     * @param errorCode The error code indicating the disconnection reason.
     */
    private void printDisconnectionReason(DBCAPI_ERROR errorCode) {
        switch (errorCode) {
            case ADDRESS_CHANGE:
                System.out.println("The connection closed because the user connected from another IP address.");
                break;
            case CLOSED_BY_ADMIN:
                System.out.println("The connection was closed by an ICE Data Services administrator.");
                break;
            case CLOSED_BY_SERVER:
                System.out.println("The ICE Data Services server closed the connection.");
                break;
            case CLOSED_RECV_BUF_FULL:
                System.out.println("The connection closed because the socket receive buffer was full.");
                break;
            case NO_HEARTBEAT:
                System.out.println("The connection closed because the API did not receive heartbeats from the server.");
                break;
            case WINSOCK:
                System.out.println("The connection was disrupted due to a socket error.");
                break;
            case UNKNOWN:
                System.out.println("The connection failed for an unknown reason.");
                break;
            default:
                System.out.println("Unhandled disconnection reason: " + errorCode);
                break;
        }
    }

    /**
     * Prints the reason for an error based on the error code.
     * 
     * @param errorCode The error code indicating the error reason.
     */
    private void printErrorReason(DBCAPI_ERROR errorCode) {   	    	    	
        switch (errorCode) {
            case CONNREFUSED:
                System.out.println("The server is not listening or the connection was refused. The sample will try connecting again.");
                break;
            case INVALIDNAME:
                System.out.println("The host specified is not valid. Verify the host value and try connecting again.");
                break;
            case MUST_UPGRADE:
                System.out.println("Must upgrade to a newer version of the ICE Data Services API.");
                break;
            case NOT_CM:
                System.out.println("No response from the ICE Data Services Connection Manager. Please try connecting again.");
                break;
            case NOT_ENDOFDAY:
                System.out.println("Not authorized for use during market session; try after hours.");
                break;
            case NOT_ENTITLED:
                System.out.println("The account specified is not entitled for this service.");
                break;
            case PROXY:
                System.out.println("There was a proxy server error. Verify configuration settings if using a proxy.");
                break;
            case SERVER_NOT_AVAILABLE:
                System.out.println("The requested server type was unavailable. The sample will try connecting again.");
                break;
            case UNKNOWN:
                System.out.println("The connection failed for an unknown reason. The sample will try connecting again.");
                break;
            case WRONG_USERNAMEPASSWORD:
                System.out.println("The account username and/or password is not recognized.");
                break; 
            default:
                System.out.println("Unhandled error reason: " + errorCode);
                break;
        }        
    }

    /**
     * Prints reconnect instructions based on the error code.
     * 
     * @param errorCode The error code indicating whether reconnection is possible.
     */
    private void printReconnectInstructions(DBCAPI_ERROR errorCode) {
        String indent = "    ";
        String willReconnect = "The API will attempt to reconnect unless it is set to \"manual connect:\"\n\n"
                + indent + "jstandard.DBC.manualConnect=true\n\n"
                + "Refer to \"Setting Up the Jstandard.properties File\" on the Developer Center.";

        String willNotReconnect = "The API will not make further attempts to connect.";

        if (exitSample || errorCode == DBCAPI_ERROR.ADDRESS_CHANGE || 
            errorCode == DBCAPI_ERROR.CLOSED_RECV_BUF_FULL || errorCode == DBCAPI_ERROR.INVALIDNAME) {
            System.out.println(willNotReconnect);
            try {
                System.out.println("");
                System.out.println("Exiting application...");
                // disconnect is false when the application has already lost the connection to the service.
                consoleSample.exitSample(false);
            } catch (Exception e) {
                System.err.println("Error during sample exit: " + e.getMessage());
            }
        } else {
            System.out.println(willReconnect);
        }

        System.out.println("-----------------------------------------------------------------------------");
    }

    /**
     * Prints details about an exception that occurred during an event.
     * 
     * @param event The event during which the exception occurred.
     * @param ex    The exception that was thrown.
     */
    private void printException(ConnectionEvent event, Exception ex) {
        if (event.getConnectionException() != null) {
            System.out.println(event.getConnectionException().getMessage());
        } else {
            System.out.println("An unexpected error occurred: " + ex.getMessage());
        }

        try {
            // disconnect is false when the application has already lost the connection to the service.
            consoleSample.exitSample(false);
        } catch (Exception e) {
            System.err.println("Error during sample exit: " + e.getMessage());
        }
    }
}
