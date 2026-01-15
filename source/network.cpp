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
    }
    return true;
}

void sendSignal(const std::string &host, int port, std::string endpoint)
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
        std::string request = "GET " + endpoint + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n";
        send(sock, request.c_str(), request.length(), 0);
        for (int i = 0; i < 10; i++)
            swiWaitForVBlank();
    }
    close(sock);
}

void sendFrame(const std::string &host, int port, const std::vector<Point> &points)
{
    if (points.empty())
        return;

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
        std::string body = "";
        for (const auto &p : points)
        {
            body += std::to_string(p.x) + "," + std::to_string(p.y) + ";";
        }

        std::string request = "POST /session/frame HTTP/1.1\r\n";
        request += "Host: " + host + "\r\n";
        request += "Content-Type: text/plain\r\n";
        request += "Content-Length: " + std::to_string(body.length()) + "\r\n";
        request += "Connection: close\r\n\r\n";
        request += body;

        send(sock, request.c_str(), request.length(), 0);

        // Linger for 30 frames (0.5s) to ensure slow Wi-Fi hardware finishes the task
        for (int i = 0; i < 30; i++)
            swiWaitForVBlank();
    }
    close(sock);
}