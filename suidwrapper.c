#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main()
{
    printf("uid:%d euid:%d\n",getuid(),geteuid());
    char *envp[] = {0,NULL};
    char *argv[] = {"bash",NULL};
    setreuid(geteuid(),-1);
    printf("uid:%d euid:%d\n",getuid(),geteuid());
    execve(argv[0], argv, envp);
    return 0;
}
