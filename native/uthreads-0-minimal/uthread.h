#ifndef _UTHREAD_H_
#define _UTHREAD_H_

#include <stdint.h>

typedef struct uthread uthread_t;
typedef void (*start_routine_t)(uint64_t);

void ut_init();
uthread_t *ut_create(start_routine_t, uint64_t arg);
void ut_run();

void ut_yield();

#endif