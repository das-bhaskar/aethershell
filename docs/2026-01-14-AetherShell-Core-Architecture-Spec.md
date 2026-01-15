# Project AetherShell: Technical Specification

**Version:** 1.0  
**Date:** 2026-01-14  
**Subject:** Heterogeneous Retro-Modern Integration Architecture

## 1. Executive Summary

AetherShell is a multi-tiered communication platform bridging legacy handheld hardware (**Nintendo DS**) with modern mobile environments (**iPhone/Android**) via a centralized Java-based hub. The system leverages AI-driven narrative logic and persistent state management to create a seamless, cross-device interactive experience.

---

## 2. Component Analysis

### A. External Device Layer

- **Nintendo DS (The Tactile Deck):** Utilizing a custom C++ codebase. It handles hardware-level inputs including the **Resistive Touch Screen (Stylus)**, **D-Pad**, and **L/R Shoulder Buttons**. Communication is strictly **Raw HTTP** over Port 80.
- **iPhone (The Cinematic Viewport):** A modern mobile client requiring **Encrypted HTTPS** (Port 443). It serves as the high-resolution interface for AI-generated lore and global state visualization.

### B. The Middleman Layer (Gateway)

- **Middleman #1 (ngrok):** Acting as a TCP/HTTP tunnel. It provides a public ingress point for the Nintendo DS, bypassing local NAT/Firewall constraints and maintaining the insecure HTTP scheme required by the DS.
- **Middleman #2 (Cloudflare):** Operates as the **SSL Termination Point**. It intercepts modern HTTPS requests from the iPhone, validates the security handshake, and forwards the "cleaned" HTTP traffic to the internal hub.

### C. Internal Backend (The PC Hub)

- **Spring Boot Hub (Manager):** The central authority (Java/Port 8080). Handles Session Management, Command Parsing, and exclusively manages the connection to the **PostgreSQL Database**.
- **Flask API (AI Specialist):** A Python-based service (Port 5001) utilizing **LangChain**. It handles complex natural language processing and coordinates directly with the **Gemini API** via secure HTTPS calls.

---

## 3. Visual Architecture Map

╔════════════════════════════════════════════════════════════════════════════════════════╗
║ AETHERSHELL SYSTEM BOUNDARY ║
╠════════════════════════════════════════════════════════════════════════════════════════╣
║ [ EXTERNAL DEVICES ] [ MIDDLEMAN LAYER ] [ INTERNAL BACKEND (YOUR PC) ]║
║ ║
║ ┌────────────────┐ ┌───────────────────┐ ┌───────────────────────┐ ║
║ │ NINTENDO DS │ │ MIDDLEMAN #1 │ │ SPRING BOOT HUB │ ║
║ │ (ARM9 / C++) │──HTTP──>│ NGROK │──HTTP────>│ (Java / 8080) │ ║
║ └────────────────┘ │ (Pass-thru/Tunnel)│ │ - Session Manager │ ║
║ └───────────────────┘ │ - Command Parser │ ║
║ └─────┬───────────┬─────┘ ║
║ ┌────────────────┐ ┌───────────────────┐ │ │ ║
║ │ IPHONE │ │ MIDDLEMAN #2 │ │ │ ║
║ │ (iOS / Swift) │──HTTPS─>│ CLOUDFLARE │──HTTP──────────>┤ │(Local ║
║ └────────────────┘ │ (SSL Terminator) │ │ │ HTTP) ║
║ └───────────────────┘ │ │ ║
║ v v ║
║ ┌──────────┐┌─────────┐ ║
║ │ POSTGRES ││ FLASK │ ║
║ │ DB ││ API │ ║
║ └──────────┘└────┬────┘ ║
║ │ ║
║ │ ║
║ (HTTPS CALL) ║
║ │ ║
║ v ║
║ ┌───────────────────────┐ ║
║ │ GEMINI API │ ║
║ │ (The LLM Brain) │ ║
║ │ - Direct Connection │ ║
║ └───────────────────────┘ ║
╚════════════════════════════════════════════════════════════════════════════════════════╝

---

## 4. Communication Logic Flow

1. **Ingress:** Data enters from External Devices through the Middlemen. Middlemen strip SSL/Tunnels and deliver standardized HTTP packets to Spring Boot on Port 8080.
2. **Persistence:** Spring Boot commits the raw event to **PostgreSQL**.
3. **Intelligence:** Spring Boot makes a **Local HTTP** call to the **Flask API** (Port 5001). Flask coordinates with **Gemini** via HTTPS.
4. **Egress:** Spring Boot broadcasts the updated state back to the DS (lightweight string) and iPhone (structured JSON).

---

## 5. Port Configuration Matrix

| Service            | Protocol | Port           | Destination               |
| :----------------- | :------- | :------------- | :------------------------ |
| **Spring Boot**    | HTTP     | 8080           | Central Hub / Postgres    |
| **Flask API**      | HTTP     | 5001           | LangChain / AI Specialist |
| **PostgreSQL**     | TCP      | 5432           | Persistent Storage        |
| **External (DS)**  | HTTP     | 80 (via ngrok) | Spring Boot               |
| **External (iOS)** | HTTPS    | 443 (via CF)   | Spring Boot               |

---

_End of Specification_
