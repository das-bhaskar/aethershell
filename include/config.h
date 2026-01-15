#ifndef CONFIG_H
#define CONFIG_H

#include <string>
#include <vector>

// Structure for the pixel coordinates
struct Point
{
    uint8_t x;
    uint8_t y;
};

struct AppConfig
{
    std::string host;
    int port;
};

bool loadConfig(AppConfig &config);

#endif