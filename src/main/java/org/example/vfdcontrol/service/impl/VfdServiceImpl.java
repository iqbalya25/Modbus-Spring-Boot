package org.example.vfdcontrol.service.impl;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.example.vfdcontrol.service.VfdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;

@Service
public class VfdServiceImpl implements VfdService {
    private static final Logger logger = LoggerFactory.getLogger(VfdServiceImpl.class);
    private static final String IP_ADDRESS = "192.168.1.111";
    private static final int PORT = 502;
    private static final int SLAVE_ID = 1;
    private static final int BUTTON_ON_COIL = 3072;
    private static final int BUTTON_OFF_COIL = 3073;
    private static final int LAMP_STATUS_REGISTER = 1536;
    private static final int PULSE_DURATION_MS = 100; // Duration of the momentary pulse

    private TCPMasterConnection connection;

    public VfdServiceImpl() {
        try {
            InetAddress addr = InetAddress.getByName(IP_ADDRESS);
            connection = new TCPMasterConnection(addr);
            connection.setPort(PORT);
            connection.connect();
            logger.info("Successfully connected to Modbus device at {}:{}", IP_ADDRESS, PORT);
        } catch (Exception e) {
            logger.error("Failed to initialize Modbus connection", e);
        }
    }

    @Override
    public void turnOn() throws IOException {
        sendMomentaryPulse(BUTTON_ON_COIL);
    }

    @Override
    public void turnOff() throws IOException {
        sendMomentaryPulse(BUTTON_OFF_COIL);
    }

    @Override
    public boolean getLampStatus() throws IOException {
        int status = readRegister(LAMP_STATUS_REGISTER);
        return status == 1;
    }

    private void sendMomentaryPulse(int coil) throws IOException {
        try {
            // Turn the coil ON
            writeCoil(coil, true);

            // Wait for the pulse duration
            Thread.sleep(PULSE_DURATION_MS);

            // Turn the coil OFF
            writeCoil(coil, false);

            logger.info("Successfully sent momentary pulse to coil {}", coil);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while sending momentary pulse to coil {}", coil, e);
            throw new IOException("Failed to send momentary pulse", e);
        }
    }

    private void writeCoil(int coil, boolean state) throws IOException {
        try {
            if (!connection.isConnected()) {
                connection.connect();
            }
            WriteCoilRequest req = new WriteCoilRequest(coil, state);
            req.setUnitID(SLAVE_ID);
            ModbusResponse response = executeTransaction(req);
            if (!(response instanceof WriteCoilResponse)) {
                throw new IOException("Unexpected response type");
            }
            logger.info("Successfully wrote to coil {}: {}", coil, state);
        } catch (Exception e) {
            logger.error("Error writing to coil {}", coil, e);
            throw new IOException("Failed to write to coil", e);
        }
    }

    private int readRegister(int register) throws IOException {
        try {
            if (!connection.isConnected()) {
                connection.connect();
            }
            ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(register, 1);
            req.setUnitID(SLAVE_ID);
            ModbusResponse response = executeTransaction(req);
            if (response instanceof ReadMultipleRegistersResponse) {
                ReadMultipleRegistersResponse readResponse = (ReadMultipleRegistersResponse) response;
                int value = readResponse.getRegisterValue(0);
                logger.info("Successfully read from register {}: {}", register, value);
                return value;
            }
            throw new IOException("Unexpected response type");
        } catch (Exception e) {
            logger.error("Error reading from register {}", register, e);
            throw new IOException("Failed to read from register", e);
        }
    }

    private ModbusResponse executeTransaction(ModbusRequest request) throws ModbusException {
        ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
        transaction.setRequest(request);
        transaction.execute();
        return transaction.getResponse();
    }
}