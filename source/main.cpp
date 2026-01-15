#include <nds.h>
#include <stdio.h>
#include "config.h"
#include "network.h" // socket code here

int main(void)
{
    consoleDemoInit();
    AppConfig config;

    iprintf("Loading config from SD...\n");
    if (!loadConfig(config))
    {
        iprintf("Failed to load config.txt! Check SD card.\n");
    }
   else
    {
        iprintf("Host: %s\n", config.host.c_str());
        iprintf("Port: %d\n", config.port);
        
        if(initWifi()) {
            iprintf("WiFi Connected! Sending...\n");
            sendHttpRequest(config.host, config.port);
        } else {
            iprintf("WiFi Failed to connect!\n");
        }
    }

    while (1)
    {
        swiWaitForVBlank();
        scanKeys();
        if (keysDown() & KEY_START)
            break;
    }
    return 0;
}