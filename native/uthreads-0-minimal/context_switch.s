.text
	
.global context_switch
.global context_switch_and_free

/* 
 * void context_switch(uthread_t *curr_thread, uthread_t *next_thread);
 *
 * Full context switch, saving the current thread context
 *  - %rdi has curr_thread
 *  - %rsi has next_thread
 */
context_switch:
    // suspend curr_thread ...
    // ... by saving the registers that aren't saved by the C compiler generated code 
	pushq %rbp
	pushq %rbx
	pushq %r12
	pushq %r13
	pushq %r14
	pushq %r15

    // ... %rsp is the only one that cannot be saved in the stack
    //     so we save it in the curr_thread descriptor
	movq %rsp, (%rdi)
	
    // resume next_thread ...
    // ... by loading the th1 stack from the th1 descriptor
	movq (%rsi), %rsp
	
    // ... and restoring the saved registers when next_thread was suspended
	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %rbx
	popq %rbp
	
    // ... returning to the point on next_thread that called context_switch
	ret

/*
 * void context_switch_and_free(uthread_t *curr_thread, uthread_t *next_thread);
 *
 * Context switch, freeing the current thread descriptor (and obviously *without* saving its context)
 *  - %rdi has curr_thread
 *  - %rsi has next_thread
 */
context_switch_and_free:
	
    // no need to preserve the curr_thread context because it is ending

    // restoring the stack pointer from the next_thread descriptor
	movq (%rsi), %rsp

    // the call to free is done on the next thread stack
    // otherwise we would be freeing the stack the call is being made
    call free
	
	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %rbx
	popq %rbp
	
	ret
