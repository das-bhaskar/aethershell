#ifndef NETWORK_H
#define NETWORK_H

#include <string>
#include <vector>
#include "config.h"

bool initWifi();
void sendSignal(const std::string &host, int port, std::string endpoint);
void sendFrame(const std::string &host, int port, const std::vector<Point> &points);

#endif