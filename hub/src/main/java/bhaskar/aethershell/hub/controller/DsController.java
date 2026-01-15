package bhaskar.aethershell.hub.controller;

import org.springframework.web.bind.annotation.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/session")
public class DsController {

    /**
     * Storage:
     * Key: Session ID (e.g., "909d4725")
     * Value: A list where each entry is one full frame of coordinate data
     */
    private final ConcurrentHashMap<String, List<String>> activeSessions = new ConcurrentHashMap<>();

    // Tracks the current active session globally for the DS
    private String currentSessionId = null;

    // 1. START SESSION: Triggered by (Y) on DS
    @GetMapping("/new")
    public String startSession() {
        currentSessionId = UUID.randomUUID().toString().substring(0, 8);
        activeSessions.put(currentSessionId, new ArrayList<>());

        System.out.println("\n--- NEW SESSION INITIALIZED ---");
        System.out.println("ID: " + currentSessionId);
        System.out.println("Status: Awaiting Frame Data...");
        System.out.println("--------------------------------\n");

        return currentSessionId;
    }

    // 2. RECEIVE FRAME: Triggered by (A) on DS
    @PostMapping("/frame")
    public String receiveFrame(@RequestBody String coordinateString) {
        if (currentSessionId == null) {
            System.out.println("[!] REJECTED: DS attempted to send frame without starting session.");
            return "Error: Start session first.";
        }

        // Clean the data (remove any trailing semicolons or spaces)
        String data = coordinateString.trim();
        if (data.isEmpty()) return "Error: Empty frame.";

        // Store the data in the list for this session
        activeSessions.get(currentSessionId).add(data);

        // Verification: Count how many points we actually got
        String[] points = data.split(";");

        // Print the entire raw coordinate string to the console
        String preview = coordinateString;
        System.out.println("FULL RAW DATA RECEIVED: " + preview);

        System.out.println("[" + currentSessionId + "] RECEIVED FRAME " + activeSessions.get(currentSessionId).size());
        System.out.println("    Points: " + points.length);
        System.out.println("    Data Preview: " + preview);

        return "Frame Stored Successfully";
    }

    // 3. FINALIZE: Triggered by (X) on DS
    @GetMapping("/done")
    public String finalizeSession() {
        if (currentSessionId == null || !activeSessions.containsKey(currentSessionId)) {
            return "No active session found.";
        }

        List<String> allFrames = activeSessions.get(currentSessionId);
        int frameCount = allFrames.size();

        System.out.println("\n--- SESSION COMPLETE ---");
        System.out.println("Finalizing Session: " + currentSessionId);
        System.out.println("Total Frames Collected: " + frameCount);
        System.out.println("Ready for Python Animation Worker.");
        System.out.println("------------------------\n");

        // Note: We keep the data in activeSessions so Python can fetch it
        // Or we can trigger a Python ProcessBuilder here.

        return "Success: " + frameCount + " frames ready for processing.";
    }

    // 4. HELPER: To allow Python (or a browser) to see the data
    @GetMapping("/data/{id}")
    public List<String> getSessionData(@PathVariable String id) {
        return activeSessions.getOrDefault(id, new ArrayList<>());
    }
}