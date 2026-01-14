#include <nds.h>
#include <dswifi9.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <netdb.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>

// Update this with the current URL from your ngrok terminal!
#define SERVER_HOST "vickie-postepileptic-yoko.ngrok-free.dev"
#define SERVER_PORT 80

void haltWithMessage(const char *msg)
{
    iprintf("%s\n", msg);
    iprintf("\nPress START to exit.");
    while (1)
    {
        swiWaitForVBlank();
        scanKeys();
        if (keysDown() & KEY_START)
            break;
    }
}

int main(void)
{
    consoleDemoInit();

    iprintf("=== DS Web Client ===\n");
    iprintf("Initializing WiFi...\n");

    // 1. Initialize WiFi (uses WFC data from firmware)
    if (!Wifi_InitDefault(true))
    {
        haltWithMessage("WiFi Init Failed.");
    }

    iprintf("Connecting to WiFi...\n");
    while (Wifi_AssocStatus() != ASSOCSTATUS_ASSOCIATED)
    {
        swiWaitForVBlank();
        // Optional: Add a timeout check here
    }
    iprintf("WiFi Connected!\n");

    // 2. Resolve Hostname to IP
    iprintf("Resolving %s...\n", SERVER_HOST);
    struct hostent *server = gethostbyname(SERVER_HOST);
    if (server == NULL)
    {
        haltWithMessage("DNS resolution failed.");
    }

    // 3. Create Socket
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0)
    {
        haltWithMessage("Socket creation failed.");
    }

    // 4. Set up Server Address
    struct sockaddr_in server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(SERVER_PORT);
    memcpy(&server_addr.sin_addr, server->h_addr_list[0], server->h_length);

    // 5. Connect
    iprintf("Connecting to server...\n");
    if (connect(sock, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0)
    {
        close(sock);
        haltWithMessage("Connection failed.");
    }

    // 6. Build and Send HTTP Request
    // This includes the bypass header for ngrok!
    const char *request =
        "GET / HTTP/1.1\r\n"
        "Host: " SERVER_HOST "\r\n"
        "ngrok-skip-browser-warning: 1\r\n" // skipping the ngrok welciome page
        "User-Agent: NintendoDS\r\n"
        "Connection: close\r\n\r\n";

    if (send(sock, request, strlen(request), 0) < 0)
    {
        close(sock);
        haltWithMessage("Failed to send request.");
    }
    iprintf("Request sent! Waiting...\n\n");

    // 7. Receive Response Loop
    // We use a loop because packets often arrive in chunks.
    char buffer[1024];
    int bytesReceived;
    while ((bytesReceived = recv(sock, buffer, sizeof(buffer) - 1, 0)) > 0)
    {
        buffer[bytesReceived] = '\0';
        iprintf("%s", buffer); // Print the raw HTTP response
    }

    if (bytesReceived < 0)
    {
        iprintf("\nError receiving data.");
    }

    // 8. Cleanup
    close(sock);
    iprintf("\n\n=== Connection Closed ===\n");
    haltWithMessage("Done!");

    return 0;
}