package bhaskar.aethershell.hub.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class PythonBridgeService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_URL = "http://localhost:5001/process_vram";

    public Map<String, Object> callPythonWorker(String sessionId, List<String> frames) {
        Map<String, Object> request = new HashMap<>();
        request.put("session_id", sessionId);
        request.put("frames", frames);

        try {
            // POST to your Python Flask server
            return restTemplate.postForObject(PYTHON_URL, request, Map.class);
        } catch (Exception e) {
            System.out.println("Python Bridge Error: " + e.getMessage());
            return null;
        }
    }
}