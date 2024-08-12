package org.example.vfdcontrol.service.impl;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.example.vfdcontrol.service.VfdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class VfdServiceImpl implements VfdService {
    private static final Logger logger = LoggerFactory.getLogger(VfdServiceImpl.class);
    private SerialConnection connection;
    private static final int MODBUS_DELAY = 500;
    private static final int SLAVE_ADDRESS= 1;
    private boolean lastConnectionStatus = false;

    public VfdServiceImpl() {
        SerialParameters params = new SerialParameters();
        String portName = System.getenv("SERIAL_PORT");
        if (portName == null || portName.isEmpty()) {
            portName = "/dev/ttyS9";  // fallback to default
        }

        logger.info("Attempting to use port: {}", portName);
        params.setPortName(portName);
        params.setBaudRate(9600);
        params.setDatabits(8);
        params.setParity(2);  // Even parity
        params.setStopbits(1);
        params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
        params.setEcho(false);

        connection = new SerialConnection(params);
        try {
            connection.open();
            lastConnectionStatus = true;
            logger.info("Serial connection opened successfully on port: {}", portName);
        } catch (Exception e) {
            logger.error("Failed to open serial connection on port: {}", portName, e);
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
        ModbusSerialTransaction trans = new ModbusSerialTransaction(connection);
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
                connection.open();
                logger.info("Reopened serial connection");
            } catch (Exception e) {
                logger.error("Failed to reopen serial connection", e);
                throw new IOException("Failed to connect to VFD", e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    @Override
    public void connect() throws IOException {
        if (!isConnected()) {
            try {
                connection.open();
                logger.info("Serial connection opened successfully");
            } catch (Exception e) {
                logger.error("Failed to open serial connection", e);
                throw new IOException("Failed to connect to VFD", e);
            }
        }
    }

    @Override
    public void disconnect() throws IOException {
        if (isConnected()) {
            try {
                connection.close();
                logger.info("Serial connection closed successfully");
            } catch (Exception e) {
                logger.error("Failed to close serial connection", e);
                throw new IOException("Failed to disconnect from VFD", e);
            }
        }
    }
}
