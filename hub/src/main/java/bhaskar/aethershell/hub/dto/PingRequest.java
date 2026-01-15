package bhaskar.aethershell.hub.dto;

import lombok.Data;

@Data // This Lombok tag handles all getters/setters automatically
public class PingRequest {
    private String message;
    private String sender;
}