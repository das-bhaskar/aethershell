#include <nds.h>
#include <stdio.h>
#include <vector>
#include "config.h"
#include "network.h"

#define COLOR_WHITE 0xFFFF
#define COLOR_BLACK 0x8000

u16 *canvasMemory;
std::vector<Point> currentFramePath;
bool isDrawing = false;

void setupScreens()
{
    // Force Main Engine (Console) to Top, Sub Engine (Drawing) to Bottom
    lcdMainOnTop();

    // 1. Setup Top Screen (Main Engine)
    videoSetMode(MODE_0_2D);
    vramSetBankA(VRAM_A_MAIN_BG);
    // Explicitly init console on Main engine
    consoleInit(NULL, 0, BgType_Text4bpp, BgSize_T_256x256, 31, 0, true, true);

    // 2. Setup Bottom Screen (Sub Engine) for 16-bit Bitmap
    videoSetModeSub(MODE_5_2D);
    vramSetBankC(VRAM_C_SUB_BG);
    int bgId = bgInitSub(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    canvasMemory = (u16 *)bgGetGfxPtr(bgId);

    // 3. Clear Bottom
    for (int i = 0; i < 256 * 192; i++)
        canvasMemory[i] = COLOR_WHITE;
}

void drawThickBrush(int x, int y)
{
    if (x < 3 || y < 3 || x > 252 || y > 188)
        return;
    for (int i = -2; i <= 2; i++)
    {
        for (int j = -2; j <= 2; j++)
        {
            canvasMemory[(y + i) * 256 + (x + j)] = COLOR_BLACK;
        }
    }
}

int main(void)
{
    setupScreens();
    AppConfig config;
    loadConfig(config);
    initWifi();

    while (1)
    {
        swiWaitForVBlank();
        scanKeys();

        u32 kDown = keysDown();
        u32 kHeld = keysHeld();
        u32 kUp = keysUp();

        // --- DRAWING & RECORDING ---
        if (kHeld & KEY_TOUCH)
        {
            touchPosition touch;
            touchRead(&touch);
            drawThickBrush(touch.px, touch.py);
            isDrawing = true;

            // Only record the point if it's different from the last one to save data
            if (currentFramePath.empty() ||
                currentFramePath.back().x != (uint8_t)touch.px ||
                currentFramePath.back().y != (uint8_t)touch.py)
            {
                currentFramePath.push_back({(uint8_t)touch.px, (uint8_t)touch.py});
            }
        }

        // PEN LIFT DETECTION: Insert "Break" signal (255, 255)
        if ((kUp & KEY_TOUCH) && isDrawing)
        {
            currentFramePath.push_back({255, 255});
            isDrawing = false;
        }

        // --- TOP SCREEN UI ---
        consoleClear();
        printf("\n   ==========================");
        printf("\n       AETHERSHELL HUB       ");
        printf("\n   ==========================");
        printf("\n\n   Points Recorded: %d", currentFramePath.size());
        printf("\n\n   --- CONTROLS ---");
        printf("\n   (Y) : START NEW SESSION");
        printf("\n   (A) : SEND FRAME & CLEAR");
        printf("\n   (X) : ANIMATE!");
        printf("\n\n   -----------------------");

        // --- MAPPED ACTIONS ---
        if (kDown & KEY_Y)
        {
            sendSignal(config.host, config.port, "/session/new");
            currentFramePath.clear();
            printf("\n\n   [!] NEW SESSION STARTED");
        }

        if (kDown & KEY_A)
        {
            printf("\n\n   [!] SENDING PATH DATA...");
            sendFrame(config.host, config.port, currentFramePath);
            // Clear both memory and recorded path
            for (int i = 0; i < 256 * 192; i++)
                canvasMemory[i] = COLOR_WHITE;
            currentFramePath.clear();
            printf("\n   [!] UPLOAD SUCCESSFUL");
        }

        if (kDown & KEY_X)
        {
            sendSignal(config.host, config.port, "/session/done");
            printf("\n\n   [!] ANIMATION TRIGGERED!");
        }
    }
    return 0;
}