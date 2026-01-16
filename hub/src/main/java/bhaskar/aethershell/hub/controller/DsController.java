package bhaskar.aethershell.hub.controller;

import bhaskar.aethershell.hub.service.PythonBridgeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/session")
public class DsController {

    @Autowired
    private PythonBridgeService pythonBridge;

    private final ConcurrentHashMap<String, List<String>> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> sessionResults = new ConcurrentHashMap<>();
    private volatile String currentSessionId = null;

    @PostConstruct
    public void initTunnel() {
        Thread tunnelThread = new Thread(() -> {
            try {
                System.out.println("AetherShell: Launching background tunnel...");
                // Note: Make sure no other 'cloudflared' is running in separate terminals
                ProcessBuilder pb = new ProcessBuilder("cloudflared", "tunnel", "--url", "http://localhost:8443");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                Pattern pattern = Pattern.compile("https://[a-zA-Z0-9-]+\\.trycloudflare\\.com");

                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        printQRCode(matcher.group());
                    }
                }
            } catch (Exception e) {
                System.err.println("Tunnel Auto-Start Failed: " + e.getMessage());
            }
        });
        tunnelThread.setDaemon(true);
        tunnelThread.start();
    }

    private void printQRCode(String url) {
        try {
            // Strip the fat to keep the grid tiny
            String middlePart = url.replace("https://", "").replace(".trycloudflare.com", "");

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.MARGIN, 1); // Small border helps camera focus

            BitMatrix matrix = new MultiFormatWriter().encode(middlePart, BarcodeFormat.QR_CODE, 0, 0, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();

            System.out.println("\n--- TIGHT SCAN (AETHERSHELL) ---");

            for (int y = 0; y < height; y++) {
                StringBuilder sb = new StringBuilder("        ");
                for (int x = 0; x < width; x++) {
                    // Using TWO blocks with NO extra space creates a solid pixel
                    sb.append(matrix.get(x, y) ? "██" : "  ");
                }
                System.out.println(sb.toString());
            }
            System.out.println("\n    ID: " + middlePart);
            System.out.println("    URL: " + url + "\n");

        } catch (Exception e) {
            System.err.println("QR Error: " + e.getMessage());
        }
    }

    @GetMapping("/latest")
    public String getLatestSession() {
        return (currentSessionId != null) ? currentSessionId : "NONE";
    }

    @GetMapping("/new")
    public String startSession() {
        clearOutputFolder();
        currentSessionId = UUID.randomUUID().toString().substring(0, 8);
        activeSessions.put(currentSessionId, new ArrayList<>());
        System.out.println("\n[SYSTEM] New Session: " + currentSessionId);
        return currentSessionId;
    }

    @PostMapping("/frame")
    public String receiveFrame(@RequestBody String coordinateString) {
        if (currentSessionId == null) return "Error";
        activeSessions.get(currentSessionId).add(coordinateString.trim());
        return "Stored";
    }

    @GetMapping("/done")
    public Map<String, Object> finalizeSession() {
        if (currentSessionId == null) return Collections.singletonMap("error", "No session");
        List<String> allFrames = activeSessions.get(currentSessionId);
        Map<String, Object> result = pythonBridge.callPythonWorker(currentSessionId, allFrames);
        if (result != null) sessionResults.put(currentSessionId, result);
        return result;
    }

    @GetMapping("/results/{id}")
    public Map<String, Object> getResults(@PathVariable String id) {
        return sessionResults.getOrDefault(id, Collections.singletonMap("status", "processing"));
    }

    private void clearOutputFolder() {
        try {
            String rootPath = System.getProperty("user.dir");
            String subPath = rootPath.endsWith("hub") ? "/src/main/resources/static/output/" : "/hub/src/main/resources/static/output/";
            File outputDir = new File(rootPath + subPath);
            if (outputDir.exists() && outputDir.isDirectory()) {
                File[] files = outputDir.listFiles();
                if (files != null) {
                    for (File f : files) if (f.getName().endsWith(".png")) f.delete();
                }
            }
        } catch (Exception e) {}
    }
}