
#include <stdlib.h>
#include <stdio.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/epoll.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>

#include "sockets.h"
#include "../utils/list.h"
#include "../utils/log.h"
#include "uthread.h"

// internal functions from uthreads used here
void move_thread_running_to_list(list_entry_t *list);
void move_head_thread_to_ready(list_entry_t *list);
void schedule();

#define ILLEGAL_STATE(msg)                                         \
  do                                                               \
  {                                                                \
    printf("ILLEGAL STATE on %s:%d: %s", __FILE__, __LINE__, msg); \
  } while (0)
#define PANIC()                  \
  do                             \
  {                              \
    try(-1, __FILE__, __LINE__); \
  } while (0)

int try(int value, char *file, int line)
{
  if (value != -1)
  {
    return value;
  }
  printf("PANIC on %s:%d: %d - %s\n", file, line, errno, strerror(errno));
  exit(EXIT_FAILURE);
}

#define TRY(expr) try((expr), __FILE__, __LINE__)

#define DESCRIMINATOR_SOCKET (1)
#define DESCRIMINATOR_SERVER_SOCKET (2)

typedef struct socket
{
  int descriminator;
  // socket file descriptor
  int sd;
  list_entry_t reader_threads;
  list_entry_t writer_threads;
} socket_t;

typedef struct server_socket
{
  int descriminator;
  // socket file descriptor
  int sd;
  list_entry_t accept_threads;
} server_socket_t;

// Global epoll used for all sockets
int sockets_epoll;

void sockets_init()
{
  sockets_epoll = TRY(epoll_create(1));
}

void sockets_end()
{
  TRY(close(sockets_epoll));
}

#define MAX_EVENTS 32
struct epoll_event wait_events[MAX_EVENTS];

// Checks for events and moves associated threads into the ready state
void sockets_move_to_ready()
{
  while (1)
  {
    int len = TRY(epoll_wait(sockets_epoll, wait_events, MAX_EVENTS, 0));
    for (int i = 0; i < len; ++i)
    {
      struct epoll_event event = wait_events[i];
      int descriminator = *((int *)event.data.ptr);
      if (descriminator == DESCRIMINATOR_SERVER_SOCKET)
      {
        logm("server socket is ready");
        server_socket_t *ss = (server_socket_t *)event.data.ptr;
        while (!list_is_empty(&(ss->accept_threads)))
        {
          move_head_thread_to_ready(&(ss->accept_threads));
        }
      }
      else if (descriminator == DESCRIMINATOR_SOCKET)
      {
        socket_t *cs = (socket_t *)event.data.ptr;
        if (event.events & EPOLLIN)
        {
          logm("socket is ready for read");
          while (!list_is_empty(&(cs->reader_threads)))
          {
            move_head_thread_to_ready(&(cs->reader_threads));
          }
        }
        if (event.events & EPOLLOUT)
        {
          logm("socket is ready for write");
          while (!list_is_empty(&(cs->writer_threads)))
          {
            move_head_thread_to_ready(&(cs->writer_threads));
          }
        }
      }
      else
      {
        ILLEGAL_STATE("Invalid descriminator");
      }
    }
    if (len < MAX_EVENTS)
    {
      return;
    }
  }
}

server_socket_t *server_socket_create(uint64_t port)
{
  server_socket_t *ss = malloc(sizeof(server_socket_t));
  ss->descriminator = DESCRIMINATOR_SERVER_SOCKET;

  // create socket file descriptor
  ss->sd = TRY(socket(AF_INET, SOCK_STREAM, 0));

  // init list
  list_init(&(ss->accept_threads));

  // bind to port and listen
  struct sockaddr_in server_address;
  server_address.sin_family = AF_INET;
  server_address.sin_addr.s_addr = INADDR_ANY;
  server_address.sin_port = htons(port);
  TRY(bind(ss->sd, (struct sockaddr *)&server_address, sizeof(server_address)));
  TRY(listen(ss->sd, SOMAXCONN));

  // set O_NONBLOCK
  int flags = TRY(fcntl(ss->sd, F_GETFL, 0));
  TRY(fcntl(ss->sd, F_SETFL, flags | O_NONBLOCK));

  // register an event
  struct epoll_event event;
  event.events = EPOLLIN | EPOLLET;
  event.data.ptr = ss;
  TRY(epoll_ctl(sockets_epoll, EPOLL_CTL_ADD, ss->sd, &event));
  return ss;
}

void server_socket_close(server_socket_t *ss)
{
  // deregister event
  TRY(epoll_ctl(sockets_epoll, EPOLL_CTL_DEL, ss->sd, NULL));
  // close file descriptor
  TRY(close(ss->sd));
  ss->sd = 0;
  free(ss);
}

socket_t *socket_internal_create(int sd);
socket_t *server_socket_accept(server_socket_t *ss)
{
  while (1)
  {
    int res = accept(ss->sd, NULL, NULL);
    if (res != -1)
    {
      // res contains the client socket file descriptor
      return socket_internal_create(res);
    }
    if (errno == EAGAIN || errno == EWOULDBLOCK)
    {
      // ready -> not-ready, since there is not client socket ready to accept
      move_thread_running_to_list(&(ss->accept_threads));
      schedule();
    }
    else
    {
      PANIC();
    }
  }
}

socket_t *socket_internal_create(int sd)
{
  socket_t *cs = malloc(sizeof(socket_t));
  cs->descriminator = DESCRIMINATOR_SOCKET;
  cs->sd = sd;
  list_init(&(cs->reader_threads));
  list_init(&(cs->writer_threads));

  // set O_NONBLOCK
  int flags = TRY(fcntl(cs->sd, F_GETFL, 0));
  TRY(fcntl(cs->sd, F_SETFL, flags | O_NONBLOCK));

  // register an event
  struct epoll_event event;
  event.events = EPOLLIN | EPOLLOUT | EPOLLET;
  event.data.ptr = cs;
  TRY(epoll_ctl(sockets_epoll, EPOLL_CTL_ADD, cs->sd, &event));

  return cs;
}

socket_t *socket_create()
{
  int sd = TRY(socket(AF_INET, SOCK_STREAM, 0));
  return socket_internal_create(sd);
}

int socket_write(socket_t *socket, uint8_t *buf, size_t size)
{
  while (1)
  {
    int res = write(socket->sd, buf, size);
    if (res != -1)
    {
      return res;
    }
    else
    {
      if (errno == EAGAIN || errno == EWOULDBLOCK)
      {
        // ready -> not-ready, since socket is not ready to write
        logm("not ready to write, waiting");
        move_thread_running_to_list(&(socket->writer_threads));
        schedule();
      }
      else
      {
        // return the -1 error to the caller
        return res;
      }
    }
  }
}

int socket_read(socket_t *socket, uint8_t *buf, size_t size)
{
  logm("read");
  while (1)
  {
    int res = read(socket->sd, buf, size);
    logx("read returned", res);
    if (res != -1)
    {
      return res;
    }
    else
    {
      if (errno == EAGAIN || errno == EWOULDBLOCK)
      {
        // ready -> not-ready, since socket is not ready to read
        logm("not ready to read, waiting");
        move_thread_running_to_list(&(socket->reader_threads));
        schedule();
      }
      else
      {
        return res;
      }
    }
  }
}

void socket_close(socket_t *socket)
{
  // deregister event
  TRY(epoll_ctl(sockets_epoll, EPOLL_CTL_DEL, socket->sd, NULL));
  TRY(close(socket->sd));
  socket->sd = 0;
  free(socket);
}
