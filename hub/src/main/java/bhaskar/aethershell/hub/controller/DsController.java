package bhaskar.aethershell.hub.controller;

import bhaskar.aethershell.hub.dto.PingRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController // Tells Spring this is an API
public class DsController {

    // 1. Handshake Endpoint (GET /)
    @GetMapping("/")
    public String index() {
        System.out.println("DS connected to root!");
        return "Hello from Spring Boot! Connection Successful.\n";
    }

    // 2. Data Endpoint (POST /ping)
    @PostMapping("/ping")
    public Map<String, Object> ping(@RequestBody PingRequest data) {
        System.out.println("Received from DS: " + data);

        // Creating the response JSON
        Map<String, Object> response = new HashMap<>();
        response.put("response", "pong");
        response.put("received", data);

        return response; // Spring automatically converts this Map to JSON
    }
}