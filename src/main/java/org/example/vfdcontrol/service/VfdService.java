package org.example.vfdcontrol.service;

import java.io.IOException;

public interface VfdService {
    void turnOn() throws IOException;
    void turnOff() throws IOException;
    boolean getLampStatus() throws IOException;
}