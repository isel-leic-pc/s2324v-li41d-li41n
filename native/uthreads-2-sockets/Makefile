main: main.c uthread.c sockets.c context_switch.s uthread.h sockets.h ../utils/list.h ../utils/list
	gcc -z noexecstack -Wall -pedantic -g -o main main.c sockets.c context_switch.s uthread.c ../utils/list

../utils/list: ../utils/list.h ../utils/list.c
	make -C ../utils

clean:
	rm -f main
