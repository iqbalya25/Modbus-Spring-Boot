package org.example.vfdcontrol.controller;

import jdk.jfr.Frequency;
import org.example.vfdcontrol.dto.CommandRequestDTO;
import org.example.vfdcontrol.dto.FrequencyRequestDTO;
import org.example.vfdcontrol.dto.FrequencyResponseDTO;
import org.example.vfdcontrol.service.VfdService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/vfdcontrol")
@CrossOrigin(origins = "http://localhost:3000")
public class VfdController {
    private final VfdService vfdService;

    public VfdController(VfdService vfdService) {
        this.vfdService = vfdService;
    }

    @PostMapping("/frequency")
    public void setFrequency(@RequestBody FrequencyRequestDTO frequencyRequestDTO) {
        try {
            vfdService.setFrequency(frequencyRequestDTO.getFrequency());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/command")
    public void sendCommand(@RequestBody CommandRequestDTO commandRequestDTO) {
        try {
            vfdService.sendCommand(commandRequestDTO.getCommand());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/frequency")
    public FrequencyResponseDTO getFrequency() {
        try {
            int frequency = vfdService.readFrequency();
            return new FrequencyResponseDTO(frequency);
        } catch (Exception e) {
            e.printStackTrace();
            return new FrequencyResponseDTO(-1); // Indicate failure
        }
    }

    @GetMapping("/status")
    public String getConnectionStatus() {
        return vfdService.isConnected() ? "Connected" : "Disconnected";
    }


}
