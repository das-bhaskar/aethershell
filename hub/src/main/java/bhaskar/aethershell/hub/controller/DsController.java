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
import java.util.List;

@RestController
@RequestMapping("/session")
public class DsController {

    @Autowired
    private PythonBridgeService pythonBridge;

    private final ConcurrentHashMap<String, List<String>> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> sessionResults = new ConcurrentHashMap<>();
    private volatile String currentSessionId = null;

    @PostConstruct
    public void initHub() {
        System.out.println("\n--- AETHERSHELL HUB STARTING ---");

        // 1. Setup Python Sandbox & Run Worker (Safe for Users)
        //setupAndRunPython();

        // 2. Launch Tunnels (Handles local binaries or system brew)
        startTunnel("cloudflared", "tunnel", "--url", "http://localhost:8443");
        startTunnel("ngrok", "http", "8080", "--scheme", "http", "--log=stdout");    }

 /*   private void setupAndRunPython() {
        new Thread(() -> {
            try {
                String root = System.getProperty("user.dir");
                // Check if we are in "Release Mode" (executable exists)
                // or "Dev Mode" (app.py exists)
                File visionEngine = new File(root, "vision_engine");
                File pythonScript = new File(root, "app.py");

                if (visionEngine.exists()) {
                    System.out.println("[SYSTEM] Portable Vision Engine detected. Launching...");
                    new ProcessBuilder("./vision_engine").inheritIO().start();
                } else if (pythonScript.exists()) {
                    System.out.println("[SYSTEM] Dev mode detected. Running app.py...");
//f0r development
                    new ProcessBuilder("python3", "app.py").inheritIO().start();
                } else {
                    System.err.println("[ERROR] No Python worker found (checked for vision_engine and app.py)");
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Python Startup Failed: " + e.getMessage());
            }
        }).start();
    }
*/
    private void startTunnel(String binName, String... args) {
        new Thread(() -> {
            try {
                // Portable logic: use ./bin if present (User mode), else use system bin (Dev mode)
                File localBin = new File(System.getProperty("user.dir") + "/" + binName);
                String commandToRun = localBin.exists() ? "./" + binName : binName;

                List<String> command = new ArrayList<>();
                command.add(commandToRun);
                command.addAll(Arrays.asList(args));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                // Matches http or https for Cloudflare and Ngrok
                Pattern pattern = Pattern.compile("http(s)?://[a-zA-Z0-9.-]+\\.(trycloudflare\\.com|ngrok-free\\.app)");

                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String url = matcher.group();
                        if (url.contains("cloudflare")) {
                            printQRCode(url);
                        } else if (url.contains("ngrok")) {
                            printDsConfig(url);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERROR] " + binName + " Tunnel Failed: " + e.getMessage());
            }
        }).start();
    }

    private void printQRCode(String url) {
        try {
            // Extraction logic for iPhone scan
            String id = url.replace("https://", "").replace(".trycloudflare.com", "");
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(id, BarcodeFormat.QR_CODE, 0, 0, hints);

            System.out.println("\n[ SCAN FOR IPHONE ]");
            for (int y = 0; y < matrix.getHeight(); y++) {
                StringBuilder sb = new StringBuilder("    ");
                for (int x = 0; x < matrix.getWidth(); x++) {
                    sb.append(matrix.get(x, y) ? "██" : "  ");
                }
                System.out.println(sb.toString());
            }
            System.out.println("    ID: " + id);
            System.out.println("    URL: " + url);
        } catch (Exception e) {
            System.err.println("QR Print Error: " + e.getMessage());
        }
    }

    private void printDsConfig(String url) {
        String cleanUrl = url.replace("http://", "").replace("https://", "");
        System.out.println("\n[ NDS SD CARD: config.txt PLEASE PASTE AS IS]");
        System.out.println("--------------------------------");
        System.out.println("SERVER_HOST=" + cleanUrl);
        System.out.println("SCHEME=http");
        System.out.println("SERVER_PORT=80");
        System.out.println("--------------------------------");
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
        System.out.println("\n[SESSION] New: " + currentSessionId);
        return currentSessionId;
    }

    @PostMapping("/frame")
    public String receiveFrame(@RequestBody String coordinateString) {
        if (currentSessionId == null) return "Error: No Session";
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
            // This gets the folder where the JAR is currently sitting
            String rootPath = System.getProperty("user.dir");

            // We change this to a simple "static/output" folder next to the JAR
            File outputDir = new File(rootPath + "/static/output/");

            if (!outputDir.exists()) {
                outputDir.mkdirs(); // Create it if it's missing
            }

            File[] files = outputDir.listFiles();
            if (files != null) {
                for (File f : files) if (f.getName().endsWith(".png")) f.delete();
            }
        } catch (Exception e) {
            System.err.println("Clear Folder Error: " + e.getMessage());
        }
    }
}