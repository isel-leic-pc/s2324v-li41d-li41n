#include <stdlib.h>
#include <time.h>
#include <stdbool.h>

#include "../utils/list.h"
#include "../utils/log.h"
#include "uthread.h"

#define STACK_SIZE (8 * 1024)

// structures...

// ... thread descriptor
struct uthread
{
  // needs to be the first field
  uint64_t rsp;
  uint8_t *stack;
  start_routine_t start;
  uint64_t arg;
  list_entry_t list_entry;
  list_entry_t joiners;
};

// ... struct mimicking the layout with the saved context
typedef struct uthread_context
{
  uint64_t r15;
  uint64_t r14;
  uint64_t r13;
  uint64_t r12;
  uint64_t rbx;
  uint64_t rbp;
  void (*func_addr)();
} uthread_context_t;

// globals ...

// ... the currently running thread
uthread_t *thread_running;
// ... the thread where the uthread system is running
uthread_t *thread_main;
// ... the queue of ready threads
list_entry_t queue_ready;

// External functions written in assembly
void context_switch(uthread_t *curr_thread, uthread_t *next_thread);
void context_switch_and_free(uthread_t *curr_thread, uthread_t *next_thread);

void schedule()
{
  uthread_t *next_thread = list_is_empty(&queue_ready)
                               ? thread_main
                               : node_of(list_remove_head(&queue_ready), uthread_t, list_entry);
  if (next_thread == thread_running)
  {
    // no context is needed because next_thread is already running
    return;
  }
  uthread_t *current = thread_running;
  thread_running = next_thread;
  context_switch(current, next_thread);
}

void schedule_and_free_current()
{
  uthread_t *next_thread = list_is_empty(&queue_ready)
                               ? thread_main
                               : node_of(list_remove_head(&queue_ready), uthread_t, list_entry);
  uthread_t *current = thread_running;
  thread_running = next_thread;
  context_switch_and_free(current, next_thread);
}

void internal_start()
{
  // call the threads entry-point
  thread_running->start(thread_running->arg);

  // unblock all joiners
  while (!list_is_empty(&thread_running->joiners))
  {
    list_add_tail(&queue_ready, list_remove_head(&thread_running->joiners));
  }

  // the thread's entry point returned, so free current thread and switch to next thread
  schedule_and_free_current();
}

void ut_init()
{
  list_init(&queue_ready);
}

uthread_t *ut_create(start_routine_t start_routine, uint64_t arg)
{
  uthread_t *thread = (uthread_t *)malloc(sizeof(uthread_t));
  thread->stack = (uint8_t *)malloc(STACK_SIZE);
  uthread_context_t *context = (uthread_context_t *)(thread->stack + STACK_SIZE - sizeof(uthread_context_t));

  context->func_addr = internal_start;

  thread->rsp = (uint64_t)context;
  thread->start = start_routine;
  thread->arg = arg;

  list_init(&(thread->joiners));

  list_add_tail(&queue_ready, &(thread->list_entry));

  return thread;
}

bool ut_free(uthread_t *thread)
{
  // If the stack is not null, then the thread didn't yet ended.
  if (thread->stack == NULL)
  {
    // Free thread descriptor.
    free(thread);
    return true;
  }
  logm("cannot free thread because it hasn't ended yet");
  return false;
}

void ut_join(uthread_t *thread)
{
  if (thread->stack == NULL)
  {
    // Thread already ended
    return;
  }
  list_add_tail(&thread->joiners, &thread_running->list_entry);
  schedule();
}

void ut_run()
{
  uthread_t main_thread_struct;
  thread_main = &main_thread_struct;
  thread_running = thread_main;

  schedule();
}

void ut_yield()
{
  if (!list_is_empty(&queue_ready))
  {
    list_add_tail(&queue_ready, &(thread_running->list_entry));
    schedule();
  }
}
