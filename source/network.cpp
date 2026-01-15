#include "network.h"
#include <nds.h>
#include <dswifi9.h>
#include <sys/socket.h>
#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

bool initWifi()
{
    if (!Wifi_InitDefault(WFC_CONNECT))
        return false;
    while (Wifi_AssocStatus() != ASSOCSTATUS_ASSOCIATED)
    {
        swiWaitForVBlank();
        iprintf(".");
    }
    return true;
}

void sendHttpRequest(const std::string &host, int port)
{
    struct hostent *server = gethostbyname(host.c_str());
    if (!server)
        return;

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    memcpy(&addr.sin_addr, server->h_addr_list[0], server->h_length);

    if (connect(sock, (struct sockaddr *)&addr, sizeof(addr)) >= 0)
    {
        std::string request = "GET / HTTP/1.1\r\nHost: " + host + "\r\nngrok-skip-browser-warning: 1\r\nConnection: close\r\n\r\n";
        send(sock, request.c_str(), request.length(), 0);

        char buffer[1024];
        int bytes;
        while ((bytes = recv(sock, buffer, sizeof(buffer) - 1, 0)) > 0)
        {
            buffer[bytes] = 0;
            iprintf("%s", buffer);
        }
        iprintf("\n\nPress START to exit.");
    }
    close(sock);
}