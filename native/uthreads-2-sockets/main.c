#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
#include <errno.h>
#include <string.h>
#include "uthread.h"
#include "sockets.h"

#include "../utils/log.h"
#include "../utils/list.h"

#define PANIC(msg)                                                               \
  do                                                                             \
  {                                                                              \
    printf("PANIC on %s:%d: %d-%s", __FILE__, __LINE__, errno, strerror(errno)); \
  } while (0)

// Structure to describe an echoing thread
typedef struct echo_thread
{
  list_entry_t list_entry;
  socket_t *socket;
  int client_no;
  uthread_t *thread;
} echo_thread_t;

list_entry_t echo_threads;

void echo_loop(uint64_t data)
{
  echo_thread_t *echo_thread = (echo_thread_t *)data;
  socket_t *socket = echo_thread->socket;
  echo_thread->thread = ut_current_thread();
  uint8_t buf[128];
  int len = snprintf((char *)&buf, sizeof(buf), "hi, you are client - %d\n", echo_thread->client_no);
  int res = socket_write(socket, buf, len);
  if (res == -1)
  {
    PANIC();
  }
  while (1)
  {
    len = socket_read(socket, buf, sizeof(buf));
    if (len == 0)
    {
      socket_close(socket);
      // add current echo thread to the list of terminated threads
      list_add_tail(&echo_threads, &echo_thread->list_entry);
      return;
    }
    if (len == -1)
    {
      PANIC();
    }
    int res = socket_write(socket, buf, len);
    if (res == -1)
    {
      PANIC();
    }
  }
}

void accept_loop(uint64_t ignore)
{
  int client_no = 0;
  server_socket_t *server_socket = server_socket_create(8080);
  list_init(&echo_threads);
  while (1)
  {
    logm("accepting a new client");
    socket_t *socket = server_socket_accept(server_socket);
    logm("socket accepted");
    echo_thread_t *echo_thread = malloc(sizeof(echo_thread_t));
    echo_thread->socket = socket;
    echo_thread->client_no = ++client_no;
    ut_create(echo_loop, (uint64_t)echo_thread);
  }
}

// Thread that ...
// ... periodically prints an "alive" message on the standard output
// ... frees threads that terminated
void monitor_loop(uint64_t ignore)
{
  // polling, because we don't have a sleep yet.
  time_t last = time(NULL);
  while (1)
  {
    // Check if there are terminated threads in the list
    while (!list_is_empty(&echo_threads))
    {
      echo_thread_t *echo_thread = node_of(list_remove_head(&echo_threads), echo_thread_t, list_entry);
      logx("echo thread ended", echo_thread->client_no);
      ut_join(echo_thread->thread);
      ut_free(echo_thread->thread);
    }
    time_t now = time(NULL);
    if (now - last >= 5)
    {
      logm("still here");
      last = now;
    }
    ut_yield();
  }
}

int main()
{
  logm("main started");
  ut_init();
  ut_create(accept_loop, 0);
  ut_create(monitor_loop, 0);
  ut_run();
  logm("main ending");
  return 0;
}
