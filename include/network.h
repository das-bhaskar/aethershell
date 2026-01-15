#ifndef NETWORK_H
#define NETWORK_H

#include <string>

bool initWifi();
void sendHttpRequest(const std::string &host, int port);

#endif