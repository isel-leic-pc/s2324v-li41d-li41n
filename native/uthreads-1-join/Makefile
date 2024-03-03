main: main.c uthread.c context_switch.s uthread.h ../utils/list.h ../utils/list
	gcc -z noexecstack -Wall -pedantic -g -o main main.c context_switch.s uthread.c ../utils/list

../utils/list: ../utils/list.h ../utils/list.c
	make -C ../utils

clean:
	rm -f main
