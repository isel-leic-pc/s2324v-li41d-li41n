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

int main()
{
  printf("main starting\n");
  ut_init();
  for (int i = 0; i < 3; ++i)
  {
    ut_create(thread_routine, i);
  }
  ut_run();
  printf("main ending\n");

  return 0;
}
