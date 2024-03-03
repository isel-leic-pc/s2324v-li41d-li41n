# `uthreads-0-minimal`

First minimal `uthreads` version.
* Non-terminated threads can only be in one of two states: `running` or `ready`. I.e. no control synchronization.
* Threads go from `running` into `ready` by calling `ut_yield`.
* Threads go from `ready` into `running` by a `schedule` when the thread is at the head of the ready queue.
* Single allocated memory area for both the thread stack and the thread descriptor.
* No need to free the descriptor memory, since if is freed when the thread ends (along side the thread stack).