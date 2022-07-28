#include <android/log.h>
#include <arpa/inet.h>
#include <jni.h>
#include <netinet/in.h>
#include <sstream>
#include <string>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include <vector>

#define TAG "DeusRat"

// Helper to create the packet
void createMagicPacket(unsigned char *packet, const unsigned int mac[6])
{
  // Magic packet consists of 6 times 0xff, followed by the MAC address (=6
  // bytes) 16 times.
  unsigned char macBytes[6];
  for (int i = 0; i < 6; i++) {
    packet[i] = 0xff;
    macBytes[i] = mac[i];
  }

  for (int i = 1; i <= 16; i++) {
    memcpy(&packet[i * 6], &macBytes, 6 * sizeof(unsigned char));
  }

  char printBuffer[5000];
  printBuffer[5000 - 1] = '\0';
  memset(printBuffer, 't', 5000 - 1);
  for (int i = 0; i < 102; i++) {
    sprintf(&printBuffer[i * 5], "0x%2x ", packet[i]);
    if ((i + 1) % 4 == 0) {
      sprintf(&printBuffer[i * 5] + 4, "\n");
    }
  }
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s", printBuffer);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_deusrat_WolFragment_sendMagicPacketNative(JNIEnv *env, jobject thiz)
{
  std::stringstream log;

  // WOL needs to be broadcast.
  char broadCastAddress[] = "192.168.0.255";
  // Magic WOL packet content needs target MAC address.
  char macAddress[] = "fc:34:97:14:92:b3";
  // Magic WOL packets are exactly 102 bytes.
  unsigned char packet[102];
  unsigned int mac[6];

  struct sockaddr_in client, server;

  // Parse mac.
  sscanf(macAddress, "%x:%x:%x:%x:%x:%x", &(mac[0]), &(mac[1]), &(mac[2]),
         &(mac[3]), &(mac[4]), &(mac[5]));

  // Fill the packet with magic.
  createMagicPacket(packet, mac);

  // Create UDP socket.
  int udpSock = socket(AF_INET, SOCK_DGRAM, 0);
  if (udpSock == -1) {
    log << ">Failed to create socket: " << strerror(errno) << std::endl;
    __android_log_print(ANDROID_LOG_ERROR, "DeusRat",
                        "Failed to setup socket: %s", strerror(errno));
    env->NewStringUTF(log.str().c_str());
  }

  // Make sure it can broadcast.
  setsockopt(udpSock, SOL_SOCKET, SO_BROADCAST, &broadCastAddress,
             sizeof broadCastAddress);
  // Init client info (not important for POC so port 0).
  client.sin_family = AF_INET;
  client.sin_addr.s_addr = INADDR_ANY;
  client.sin_port = 0;

  // Bind the socket.
  int bind_s = bind(udpSock, (struct sockaddr *)&client, sizeof(client));
  if (bind_s == -1) {
    log << ">Failed to bind socket: " << strerror(errno) << std::endl;
    __android_log_print(ANDROID_LOG_ERROR, "DeusRat",
                        "Failed to bind socket: %s", strerror(errno));
    env->NewStringUTF(log.str().c_str());
  }

  // Init server info. Server listens on port 5432.
  server.sin_family = AF_INET;
  server.sin_addr.s_addr = inet_addr(broadCastAddress);
  server.sin_port = htons(5432);

  // Actually send the packet.
  int result = sendto(udpSock, &packet, sizeof(unsigned char) * 102, 0,
                      (struct sockaddr *)&server, sizeof(server));
  if (result == -1) {
    log << ">Failed to send to server: " << strerror(errno) << std::endl;
    __android_log_print(ANDROID_LOG_ERROR, "DeusRat",
                        "Failed to send to server: %s", strerror(errno));
    return env->NewStringUTF(log.str().c_str());
  }

  close(udpSock);
  log << ">Successfully sent packet to broadcast address for MAC " << macAddress
      << std::endl;
  return env->NewStringUTF(log.str().c_str());
}

std::string collapse_log(std::vector<std::string> messages)
{
  std::string log;
  std::for_each(messages.begin(), messages.end(),
                [&](const std::string &s) { log += s + "\n"; });
  return log;
}

int setup_udp_sockets(const char *server_addr, int port,
                      int* sock,
                      struct sockaddr_in *client, struct sockaddr_in *server,
                      std::vector<std::string> log_messages)
{
  int ret = 0;

  // Create socket.
  *sock = socket(AF_INET, SOCK_DGRAM, 0);
  if (*sock == -1) {
    std::stringstream ss;
    ss << "Failed to create socket! Err: " << strerror(errno);
    log_messages.push_back(ss.str());

    return -1;
  }

  client->sin_family = AF_INET;
  client->sin_addr.s_addr = INADDR_ANY;
  client->sin_port = 0;

  // Bind client to socket.
  int success = bind(*sock, (struct sockaddr*)client, sizeof(*client));
  if (success == -1) {
    std::stringstream ss;
    ss << "Failed to bind client to socket! Err: " << strerror(errno);
    log_messages.push_back(ss.str());

    return -1;
  }

  // Set destination info.
  server->sin_family = AF_INET;
  server->sin_addr.s_addr = inet_addr(server_addr);
  server->sin_port = htons(port);

  return ret;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_deusrat_WolFragment_remoteLoginNative(JNIEnv *env,
                                                       jobject thiz)
{
  std::vector<std::string> log_messages;

  const char* pcInetAddr = "192.168.0.220";
  struct sockaddr_in client, server;

  int sock, success;
  success = setup_udp_sockets(pcInetAddr, 5432, &sock, &client, &server, log_messages);
  if (success == -1) {
    return env->NewStringUTF(collapse_log(log_messages).c_str());
  }

  // Send 1 byte to server.
  // As an MVP/POC, the server authenticates any user when receiving a single UDP byte.
  success = sendto(sock, "t", 1, 0, (struct sockaddr*)&server, sizeof(server));
  if (success == -1) {
    std::stringstream ss;
    ss << "Sending to the server failed. " << strerror(errno) << std::endl;
    log_messages.emplace_back(ss.str());
    return env->NewStringUTF(collapse_log(log_messages).c_str());
  }

  log_messages.emplace_back("Successfully sent login packet.");
  return env->NewStringUTF(collapse_log(log_messages).c_str());
}