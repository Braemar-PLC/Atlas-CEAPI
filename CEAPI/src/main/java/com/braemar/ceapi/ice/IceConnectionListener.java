package com.braemar.ceapi.ice;

import java.util.logging.Logger;

import com.braemar.ceapi.Main;
import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;

public class IceConnectionListener implements ConnectionListener {

    private static final Logger logger = Logger.getLogger(Main.class.getName());


    @Override
    public void onConnected(ConnectionEvent event) {
        logger.info("Connected to ICE: " + event.getConnectedHost());
    }

    @Override
    public void onConnecting(ConnectionEvent event) {
        logger.info("Connecting to ICE...");
    }

    @Override
    public void onDisconnected(ConnectionEvent event) {
        logger.warning("Disconnected from ICE");
    }

    @Override
    public void onDisconnecting(ConnectionEvent event) {
        logger.info("Disconnecting from ICE: " + event.getStatusString());
    }

    @Override
    public void onError(ConnectionEvent event) {
        logger.severe("ICE connection error: " + event.getStatusString());
    }
}

