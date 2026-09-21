package QuoteManager.SymbolCheckSample;

import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.managers.dbc.DbcCodes.*;

/**
 * Provides methods for handling various connection, disconnection and error
 * events
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 *
 */
public class MyConsoleConnectionListener implements ConnectionListener {

	private SymbolCheckSample consoleSample;
	private boolean exitSample;
	private boolean connectionEstablished;

	public MyConsoleConnectionListener(SymbolCheckSample consoleSample) {
		this.consoleSample = consoleSample;
		this.exitSample = false;
		this.connectionEstablished = false;
	}

	/**
	 * Make request for data here, to ensure that a connection has
	 * been established before making the request.
	 * 
	 */
	public void onConnected(ConnectionEvent event) {
		if (!this.connectionEstablished) {
			System.out.println("Connected to the Level 1 Service.\n");
	        System.out.println("Server Connection: " + event.getConnectedHost());
			System.out.println("Your IP Address: " + event.getConnectionLocalIp());
			this.connectionEstablished = true;
		}
		this.consoleSample.getData();
	}

	// Required to implement this class, but not handled by this sample
	public void onConnecting(ConnectionEvent event) {

	}

	public void onDisconnected(ConnectionEvent event) {
		try {
			this.connectionEstablished = false;
			if (this.exitSample) {
				// The exitSample method takes a boolean argument.  True to issue a
				// disconnect and false otherwise.  Since the onDisconnected event
				// was issued, there is no connection to disconnect.
				this.consoleSample.exitSample(connectionEstablished);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onDisconnecting(ConnectionEvent event) {
		if (this.connectionEstablished) {
			try {
				// We check this first because it could result in an exception.
				// There is no need to display content if an exception occurs.
				DBCAPI_ERROR errorCode = DBCAPI_ERROR.valueOf(event.getStatus().name());
				System.out.println("--------------------------------------------------------------------------------");
				System.out.println(event.getStatusType().toString() + ":  " + event.getStatusString() + "\n");
				String indent = String.format("%" + (event.getStatusType().toString().length() + 3) + "s", "");

				String willReconnect = indent + "The API will attempt to reconnect unless it is set to \"manual connect:\"\n\n" + indent + "   jstandard.DBC.manualConnect=true\n\n" + indent + "Refer to \"Setting Up the Jstandard.properties File\" on the Developer\n" + indent + "Center.";

				String willNotReconnect = indent + "The API will not attempt to reconnect.";

				System.out.print(indent);

				switch (errorCode) {
				case ADDRESS_CHANGE:
					System.out.println("The connection closed because the user connected from another IP address.");
					this.exitSample = true;
					break;
				case CLOSED_BY_ADMIN:
					System.out.println("The connection was closed by an ICE Data Services administrator.");
					this.exitSample = false;
					break;
				case CLOSED_BY_SERVER:
					System.out.println("The ICE Data Services server closed the connection.");
					this.exitSample = false;
					break;
				case CLOSED_RECV_BUF_FULL:
					System.out.println("The connection closed because the socket receive buffer was full.");
					this.exitSample = true;
					break;
				case NO_HEARTBEAT:
					System.out.println("The connection closed because the API did not receive heartbeats from");
					System.out.println(indent + "the server");
					this.exitSample = false;
					break;
				case UNKNOWN:
					System.out.println("The connection failed for an unknown reason.");
					this.exitSample = false;
					break;
				case WINSOCK:
					System.out.println("The connection was disrupted due to a socket error.");
					this.exitSample = false;
					break;
				default:
					break;
				}
				System.out.println();
				if (!this.exitSample) {
					System.out.println(willReconnect);
				} else {
					System.out.println(willNotReconnect);
				}
				System.out.println("--------------------------------------------------------------------------------");
	         } catch (Exception ex) {
	             if (event.getConnectionException() != null)
	                System.out.println(event.getConnectionException().getMessage());
	             else
	             	this.exitSample = true;
	         }
		}
	}

	public void onError(ConnectionEvent event) {
		if (!this.connectionEstablished)
			System.out.println("failed to connect.\n");
		else
			this.connectionEstablished = false;
		try {
			System.out.println("--------------------------------------------------------------------------------");
			System.out.println(event.getStatusType().toString() + ":  " + event.getStatusString() + "\n");
			String indent = String.format("%" + (event.getStatusType().toString().length() + 3) + "s", "");

			String willReconnect = indent + "The API will attempt to connect again unless it is set to \"manual connect:\"\n\n" + indent + "   jstandard.DBC.manualConnect=true\n\n" + indent + "Refer to \"Setting Up the Jstandard.properties File\" on the Developer\n" + indent + "Center.";

			String willNotReconnect = indent + "The API will not make further attempts to connect.";

			System.out.print(indent);

			DBCAPI_ERROR errorCode = DBCAPI_ERROR.valueOf(event.getStatus().name());

			switch (errorCode) {
			case CONNREFUSED:
				System.out.println("The server is not listening or the connection was refused. The sample will try connecting again.");
				this.exitSample = false;
				break;
			case INVALIDNAME:
				System.out.println("The host specified is not valid.  Verify the host value and try connecting again.");
				this.exitSample = true;
				break;
			case MUST_UPGRADE:
				System.out.println("Must upgrade to a newer version of the ICE Data Services API.");
				this.exitSample = true;
				break;
			case NOT_CM:
				System.out.println("No response from the ICE Data Services Connection Manager.  Please try connecting again.");
				this.exitSample = true;
				break;
			case NOT_ENDOFDAY:
				System.out.println("Not authorized for use during market session; try after hours.");
				this.exitSample = true;
				break;
			case NOT_ENTITLED:
				System.out.println("The account specified is not entitled for this service.");
				this.exitSample = true;
				break;
			case PROXY:
				System.out.println("There was a proxy server error.  Verify configuration settings if using a proxy.");
				this.exitSample = true;
				break;
			case SERVER_NOT_AVAILABLE:
				System.out.println("The requested server type was unavailable.  The sample will try connecting again.");
				this.exitSample = false;
				break;
			case UNKNOWN:
				System.out.println("The connection failed for an unknown reason.  The sample will try connecting again.");
				this.exitSample = false;
				break;
			case WRONG_USERNAMEPASSWORD:
				System.out.println("The account username and/or password is not recognized.");
				this.exitSample = true;
				break;
			default:
				break;
			}
			System.out.println();
			if (!this.exitSample) {
				System.out.println(willReconnect);
			} else {
				System.out.println(willNotReconnect);
				this.consoleSample.exitSample(this.exitSample);
			}
			System.out.println("--------------------------------------------------------------------------------");
        } catch (Exception ex) {
            if (event.getConnectionException() != null)
               System.out.println(event.getConnectionException().getMessage());
            else
           	   this.exitSample = true;
        }
	}
}
