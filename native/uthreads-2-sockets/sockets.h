#ifndef _SOCKETS_H_
#define _SOCKETS_H_

#include <stddef.h>
#include <stdint.h>

// client socket descriptor
typedef struct socket socket_t;

// server socket descriptor
typedef struct server_socket server_socket_t;

// client socket...
// ... creation
socket_t *socket_create();
// ... termination
void socket_close(socket_t *socket);
// ... write
int socket_write(socket_t *socket, uint8_t *buf, size_t size);
// ... read
int socket_read(socket_t *socket, uint8_t *buf, size_t size);

// server socket...
// ... termination
void server_socket_close(server_socket_t *server_socket);
// ... creation, including bind and listen operations
server_socket_t *server_socket_create(uint64_t port);
// ... acception a client connection
socket_t *server_socket_accept(server_socket_t *server_socket);

#endif