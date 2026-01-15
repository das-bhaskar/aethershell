package bhaskar.aethershell.hub.controller;

import bhaskar.aethershell.hub.service.PythonBridgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/session")
public class DsController {

    @Autowired
    private PythonBridgeService pythonBridge;

    /**
     * Storage for Raw Data from NDS
     * Key: Session ID, Value: List of coordinate strings (Frames)
     */
    private final ConcurrentHashMap<String, List<String>> activeSessions = new ConcurrentHashMap<>();

    /**
     * Storage for Final Results from Python/Gemini
     * This is what the iPhone will eventually poll to get the AI guess and image links
     */
    private final ConcurrentHashMap<String, Map<String, Object>> sessionResults = new ConcurrentHashMap<>();

    // Tracks the current active session globally for the DS
    private String currentSessionId = null;

    // 1. START SESSION: Triggered by (Y) on DS
    @GetMapping("/new")
    public String startSession() {
        currentSessionId = UUID.randomUUID().toString().substring(0, 8);
        activeSessions.put(currentSessionId, new ArrayList<>());

        System.out.println("\n--- NEW SESSION INITIALIZED ---");
        System.out.println("ID: " + currentSessionId);
        System.out.println("--------------------------------\n");

        return currentSessionId;
    }

    // 2. RECEIVE FRAME: Triggered by (A) on DS
    @PostMapping("/frame")
    public String receiveFrame(@RequestBody String coordinateString) {
        if (currentSessionId == null) {
            return "Error: Start session first.";
        }

        String data = coordinateString.trim();
        if (data.isEmpty()) return "Error: Empty frame.";

        activeSessions.get(currentSessionId).add(data);

        System.out.println("[" + currentSessionId + "] Frame " + activeSessions.get(currentSessionId).size() + " received.");
        return "Frame Stored";
    }

    // 3. FINALIZE & PROCESS: Triggered by (X) on DS
    @GetMapping("/done")
    public Map<String, Object> finalizeSession() {
        if (currentSessionId == null || !activeSessions.containsKey(currentSessionId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No active session found");
            return error;
        }

        List<String> allFrames = activeSessions.get(currentSessionId);

        System.out.println("\n--- TRIGGERING TRIPLE-GATE WORKER (Port 5001) ---");

        // This calls the Python Flask server we built
        Map<String, Object> result = pythonBridge.callPythonWorker(currentSessionId, allFrames);

        if (result != null) {
            // Save the result so the iPhone can fetch it later by ID
            sessionResults.put(currentSessionId, result);
            System.out.println(" AI Interpretation: " + result.get("ai_description"));
        } else {
            System.out.println(" Python Bridge failed to return a result.");
        }

        return result;
    }

    // 4. IPHONE ENDPOINT: The iPhone app calls this to see if the AI is done
    @GetMapping("/results/{id}")
    public Map<String, Object> getResults(@PathVariable String id) {
        return sessionResults.getOrDefault(id, Collections.singletonMap("status", "processing"));
    }

    // 5. DEBUG: View raw data
    @GetMapping("/data/{id}")
    public List<String> getSessionData(@PathVariable String id) {
        return activeSessions.getOrDefault(id, new ArrayList<>());
    }
}