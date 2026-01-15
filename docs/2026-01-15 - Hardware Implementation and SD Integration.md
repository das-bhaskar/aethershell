# AetherShell Developer Log 03

## Project AetherShell: Hardware Implementation & SD Integration

**Version:** 1.1  
**Date:** 2026-01-15  
**Subject:** Nintendo DS Client-Side Deployment & FAT Environment Calibration

## 1. Executive Summary

Log 03 details the successful deployment of the "Tactile Deck" (Nintendo DS) client. The primary focus was overcoming the physical limitations of the DS hardware regarding dynamic configuration and stable network handshaking within a modern homebrew environment (TWiLight Menu++).

---

## 2. Hardware Implementation Decisions

### A. Dynamic Configuration (`config.txt`)

- **Decision:** Migration from hardcoded connection strings to an external `config.txt` stored on the microSD root.
- **Reasoning:** Since the **ngrok ingress URL** changes per session, an external config file allows for rapid updates without re-compiling the binary.
- **Standardization:** The file was renamed from `.env` to `config.txt` to prevent macOS from flagging it as a protected Unix Executable and to ensure compatibility with the DS's simple file system.

### B. The TWiLight Menu++ Mount Delay

- **Decision:** Implementation of a 30-frame hardware delay (`swiWaitForVBlank`) immediately following `fatInitDefault()`.
- **Reasoning:** In modern environments like TWiLight Menu++, the SD card virtualization layer requires a "settling" period. Attempting to `fopen()` immediately upon boot resulted in persistent I/O failures.

### C. Networking & Header Injection

- **Decision:** Manual injection of the `ngrok-skip-browser-warning: 1` HTTP header.
- **Reasoning:** ngrok's security middleware serves an HTML warning page by default. To receive raw text data from the **Spring Boot Hub**, the DS must explicitly bypass this via the request header, as the hardware lacks the resources to parse complex HTML.

---

## 3. Modular Code Architecture

The C++ client was refactored from a monolithic file into a modular structure to ensure maintainability and scalability for the "AI Bridge" functionality.

- **`config.cpp` (The Reader):** Manages `libfat` initialization and parses the key-value pairs (`SERVER_HOST`, `SERVER_PORT`) from the SD card.
- **`network.cpp` (The Transceiver):** Handles the asynchronous `dswifi` handshake and low-level TCP socket management.
- **`main.cpp` (The Orchestrator):** Manages the application lifecycle and provides visual feedback to the user via the `consoleDemo` interface.

---

## 4. Integration Verification

| Milestone              | Status      | Result                                                |
| :--------------------- | :---------- | :---------------------------------------------------- |
| **FAT Initialization** | **SUCCESS** | SD card mounted via `fatInitDefault`.                 |
| **Config Parsing**     | **SUCCESS** | ngrok URL extracted from `config.txt`.                |
| **WiFi Handshake**     | **SUCCESS** | Associated with router; IP assigned.                  |
| **Socket Connection**  | **SUCCESS** | TCP connection established to Spring Boot on Port 80. |
| **Payload Delivery**   | **SUCCESS** | "Hello from Spring Boot!" received on DS hardware.    |

---

## 5. Technical Constraints & Fixes

- **SSL/TLS Constraint:** The DS hardware (ARM9) remains incapable of Port 443 encryption. All communication to the Hub must remain on Port 80 via the **ngrok** tunnel.
- **Visual Feedback:** A visual "heartbeat" (printing dots during `Wifi_AssocStatus`) was added to distinguish between a slow network handshake and a hardware crash.
- **Memory Safety:** Switched to temporary buffers during file parsing to avoid writing to read-only pointers returned by `std::string::c_str()`.

---

_End of Specification_
