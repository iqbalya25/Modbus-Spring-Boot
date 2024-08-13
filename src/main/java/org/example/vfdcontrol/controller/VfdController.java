package org.example.vfdcontrol.controller;

import org.example.vfdcontrol.dto.CommandRequestDTO;
import org.example.vfdcontrol.dto.ModbusTcpDTO;
import org.example.vfdcontrol.service.VfdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/v1/vfdcontrol")
@CrossOrigin(origins = "http://localhost:3000")
public class VfdController {
    private final VfdService vfdService;

    public VfdController(VfdService vfdService) {
        this.vfdService = vfdService;
    }

    @PostMapping("/button")
    public ResponseEntity<String> executeCommand(@RequestBody CommandRequestDTO command) {
        try {
            if ("ON".equalsIgnoreCase(command.getCommand())) {
                vfdService.turnOn();
                return ResponseEntity.ok("Device turned on successfully");
            } else if ("OFF".equalsIgnoreCase(command.getCommand())) {
                vfdService.turnOff();
                return ResponseEntity.ok("Device turned off successfully");
            } else {
                return ResponseEntity.badRequest().body("Invalid command. Use 'ON' or 'OFF'.");
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error executing command: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ModbusTcpDTO.LampStatusResponse> getLampStatus() {
        try {
            boolean status = vfdService.getLampStatus();
            return ResponseEntity.ok(new ModbusTcpDTO.LampStatusResponse(status));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}