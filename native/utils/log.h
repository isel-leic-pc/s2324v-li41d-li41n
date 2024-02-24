#ifndef _LOG_H_
#define _LOG_H_

#include <stdio.h>

#ifndef _LOG_DISABLE_
#define logm(message) printf("[%s]\n", (message))
#define logx(message, value) printf("[%s-%lx]\n", (message), (uint64_t)(value))
#else
#define logm(message)
#define logx(message, v)
#endif

#endif
