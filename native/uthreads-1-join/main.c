#include <stdio.h>
#include <stdint.h>
#include "uthread.h"

#include "../utils/log.h"

void thread_routine(uint64_t ix)
{
  for (int i = 0; i < 5; ++i)
  {
    printf("thread %ld on iteration %d\n", ix, i);
    ut_yield();
  }
  printf("thread %ld ending\n", ix);
}

void creator_thread(uint64_t ignore)
{
  uthread_t *ths[3];
  for (int i = 0; i < 3; ++i)
  {
    ths[i] = ut_create(thread_routine, i);
  }
  for (int i = 0; i < 3; ++i)
  {
    printf("Creator thread waiting for thread %d\n", i);
    ut_join(ths[i]);
    ut_free(ths[i]);
  }
}

int main()
{
  printf("main starting\n");
  ut_init();
  uthread_t *thread = ut_create(creator_thread, 0);
  ut_run();
  ut_free(thread);
  printf("main ending\n");

  return 0;
}
