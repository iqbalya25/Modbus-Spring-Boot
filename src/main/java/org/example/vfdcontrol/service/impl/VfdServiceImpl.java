package org.example.vfdcontrol.service.impl;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import org.example.vfdcontrol.service.VfdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;

@Service
public class VfdServiceImpl implements VfdService {
    private static final Logger logger = LoggerFactory.getLogger(VfdServiceImpl.class);
    private TCPMasterConnection connection;
    private static final int MODBUS_DELAY = 500;
    private static final int SLAVE_ADDRESS = 1;
    private boolean lastConnectionStatus = false;

    public VfdServiceImpl() {
        String host = System.getenv("MODBUS_HOST");
        String portStr = System.getenv("MODBUS_PORT");
        int port = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 502; // Default Modbus TCP port

        if (host == null || host.isEmpty()) {
            host = "localhost";  // fallback to default
        }

        logger.info("Attempting to connect to Modbus TCP at {}:{}", host, port);
        try {
            InetAddress addr = InetAddress.getByName(host);
            connection = new TCPMasterConnection(addr);
            connection.setPort(port);
            connection.connect();
            lastConnectionStatus = true;
            logger.info("Modbus TCP connection established successfully to {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Failed to establish Modbus TCP connection to {}:{}", host, port, e);
            lastConnectionStatus = false;
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void monitorConnectionStatus() {
        boolean currentStatus = isConnected();
        if (currentStatus != lastConnectionStatus) {
            if (currentStatus) {
                logger.info("Device connected");
            } else {
                logger.warn("Device disconnected");
            }
            lastConnectionStatus = currentStatus;
        }
    }

    @Override
    public void setFrequency(int frequency) throws IOException, IllegalArgumentException {
        if (frequency < 0 || frequency > 6000) { // Adjust range as needed
            throw new IllegalArgumentException("Frequency must be between 0 and 400 Hz");
        }
        logger.info("Attempting to set frequency to: {}", frequency);
        int registerAddress = 14; // Frequency control register
        ModbusRequest request = new WriteSingleRegisterRequest(registerAddress, new SimpleRegister(frequency));
        request.setUnitID(SLAVE_ADDRESS);
        executeTransaction(request);
        logger.info("Frequency set successfully");
    }

    @Override
    public void sendCommand(int command) throws IOException, IllegalArgumentException {
        if (command < 0 || command > 65535) { // Adjust range as needed
            throw new IllegalArgumentException("Invalid command value");
        }
        logger.info("Sending command: {}", command);
        int registerAddress = 8; // Command control register
        ModbusRequest request = new WriteSingleRegisterRequest(registerAddress, new SimpleRegister(command));
        request.setUnitID(SLAVE_ADDRESS);
        executeTransaction(request);
        logger.info("Command sent successfully");
    }

    @Override
    public int readFrequency() throws IOException {
        logger.info("Reading frequency");
        int registerAddress = 200; // Frequency control register
        ModbusRequest request = new ReadMultipleRegistersRequest(registerAddress, 1);
        request.setUnitID(SLAVE_ADDRESS);
        ModbusResponse response = executeTransaction(request);
        if (response instanceof ReadMultipleRegistersResponse) {
            ReadMultipleRegistersResponse readResponse = (ReadMultipleRegistersResponse) response;
            int frequency = readResponse.getRegisterValue(0);
            logger.info("Frequency read: {}", frequency);
            return frequency;
        }
        logger.warn("Unexpected response type when reading frequency");
        return -1;
    }

    private ModbusResponse executeTransaction(ModbusRequest request) throws IOException {
        ensureConnection();
        ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
        trans.setRequest(request);
        trans.setRetries(5);
        try {
            logger.debug("Executing Modbus transaction: {}", request);
            trans.execute();

            // Add delay after sending request
            Thread.sleep(MODBUS_DELAY);

            ModbusResponse response = trans.getResponse();
            logger.debug("Received Modbus response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error executing Modbus transaction: {}", request, e);
            throw new IOException("Error communicating with VFD", e);
        }
    }

    private void ensureConnection() throws IOException {
        if (!isConnected()) {
            try {
                connection.connect();
                logger.info("Reopened Modbus TCP connection");
            } catch (Exception e) {
                logger.error("Failed to reopen Modbus TCP connection", e);
                throw new IOException("Failed to connect to VFD", e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    @Override
    public void connect() throws IOException {
        if (!isConnected()) {
            try {
                connection.connect();
                logger.info("Modbus TCP connection opened successfully");
            } catch (Exception e) {
                logger.error("Failed to open Modbus TCP connection", e);
                throw new IOException("Failed to connect to VFD", e);
            }
        }
    }

    @Override
    public void disconnect() throws IOException {
        if (isConnected()) {
            try {
                connection.close();
                logger.info("Modbus TCP connection closed successfully");
            } catch (Exception e) {
                logger.error("Failed to close Modbus TCP connection", e);
                throw new IOException("Failed to disconnect from VFD", e);
            }
        }
    }
}