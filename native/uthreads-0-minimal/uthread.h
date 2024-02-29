#ifndef _UTHREAD_H_
#define _UTHREAD_H_

#include <stdint.h>

/**
 * Thread descriptor - structure with information about a thread.
 */
typedef struct uthread uthread_t;
/**
 * Type of the thread entry point.
 */
typedef void (*start_routine_t)(uint64_t);

/**
 * Initializes the uthreads system.
 */
void ut_init();

/**
 * Creates a new thread in the ready state.
 */
uthread_t *ut_create(start_routine_t, uint64_t arg);

/**
 * Runs the uthreads system, returning when there aren't more threads alive. 
 */
void ut_run();

/**
 * Moves the running thread to the ready state.
 * If there aren't any other threads in the ready state, then the running thread continues in the running state.
 */
void ut_yield();

#endif