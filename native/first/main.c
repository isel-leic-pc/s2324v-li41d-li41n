#include <stdio.h>

extern int answer();

int main()
{
  int theAnswer = answer();
  printf("Hello world, the answer is %d\n", theAnswer);
  return 0;
}
