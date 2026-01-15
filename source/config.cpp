#include "config.h"
#include <fat.h>
#include <stdio.h>
#include <string.h>
#include <nds.h>

bool loadConfig(AppConfig &config)
{
    // 1. Explicitly initialize FAT
    if (!fatInitDefault())
    {
        iprintf("FAT Init Failed!\n");
        return false;
    }

    // 2. TWiLight Menu Fix: Wait a few frames for the SD to 'settle'
    for (int i = 0; i < 30; i++)
        swiWaitForVBlank();

    // 3. Try opening with the explicit 'fat:/' prefix
    // TWiLight Menu is very specific about this.
    FILE *f = fopen("fat:/config.txt", "r");

    if (!f)
    {
        // Fallback for some TWiLight versions
        f = fopen("/config.txt", "r");
    }

    if (f)
    {
        char line[128];
        while (fgets(line, sizeof(line), f))
        {
            // Remove trailing newlines/spaces
            char *end = line + strlen(line) - 1;
            while (end > line && (*end == '\n' || *end == '\r' || *end == ' '))
            {
                *end = 0;
                end--;
            }

            if (strncmp(line, "SERVER_HOST=", 12) == 0)
            {
                config.host = (line + 12);
            }
            else if (strncmp(line, "SERVER_PORT=", 12) == 0)
            {
                config.port = atoi(line + 12);
            }
        }
        fclose(f);
        iprintf("Config Loaded!\n");
        return true;
    }

    iprintf("File Not Found on SD.\n");
    return false;
}