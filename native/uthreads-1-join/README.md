# `uthreads-1-join`

Second `uthreads` version, adding `ut_join`.
* Non-terminated threads can now be in one of three states: `running`, `ready`, or `not-ready`.
    * A thread is `not-ready` if it is not running nor in the ready queue.
* Threads go from `running` into `ready` by calling `ut_yield`.
* Threads go from `ready` into `running` by a `schedule` when the thread is at the head of the ready queue.
* Threads go from `ready` into `not-ready` by calling `ut_join`, if the joined thread has not yet completed.
* Thread descriptor must be valid after a thread terminates because other threads may perform a join on it.
* Separate allocated memory areas for the thread stack and the thread descriptor, since they have different lifetimes.
    * Thread stack is freed when the thread terminates.
    * Thread descriptor is freed when there is an explicit call to `ut_free`.