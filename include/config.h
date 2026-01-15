#ifndef CONFIG_H
#define CONFIG_H

#include <string>

struct AppConfig {
    std::string host;
    int port;
};

// Initializes FAT and reads .env from the root
bool loadConfig(AppConfig &config);

#endif 