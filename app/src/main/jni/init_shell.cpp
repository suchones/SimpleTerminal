//
// Created by kr on 1/11/16.
//

#include "init_shell.h"
#include <errno.h>
#include <stddef.h>
#include "jni.h"
#include <fcntl.h>
#include <android/log.h>
#include <stdlib.h>
#include <termio.h>
#include <cstring>
#include <cstdlib>
#include <stack>

//unused
#include <termios.h>
#include <string.h>
#include <stdio.h>
#include <sgtty.h>
#include <android/tts.h>
#include <android/log.h>
#include <utmp.h>
#include <unistd.h>
#include <stdio.h>
#include <jni.h>
#include <strings.h>

#define APPNAME "SimpleTerminal"

std::deque<char> buffer;
int master_terminal_fd;
int shell_slave_pid=-1;


extern "C"{
JNIEXPORT jint JNICALL
Java_link_kjr_SimpleTerminal_MainActivity_get_1buffer_1size(JNIEnv *env, jclass type) {

    return (jint)buffer.size();
}
JNIEXPORT void JNICALL
Java_link_kjr_SimpleTerminal_MainActivity_read(JNIEnv *env, jclass type) {
    ssize_t count=1;
    do {
        unsigned char cbuf[500];
        count=read(master_terminal_fd,cbuf,500);
        for(int i=0;i<count;i++){
            buffer.push_front(cbuf[i]);
        }
        __android_log_print(ANDROID_LOG_INFO,APPNAME,"reading,count:%d buffersize:%d",count,buffer.size());

    } while (count>0);
    __android_log_print(ANDROID_LOG_INFO,APPNAME,"done reading");
}


JNIEXPORT jchar JNICALL
Java_link_kjr_SimpleTerminal_MainActivity_get_1char__(JNIEnv *env, jclass type) {
    char f=buffer.back();
    buffer.pop_back();
    return (jchar) f;

}

JNIEXPORT void JNICALL
Java_link_kjr_SimpleTerminal_MainActivity_put_1char(JNIEnv *env, jclass type, jchar c) {
    char  cchar=c;
    char buf[1]={cchar};
    write(master_terminal_fd,buf,1);
}

JNIEXPORT jint JNICALL
Java_link_kjr_SimpleTerminal_MainActivity_get_1pts(JNIEnv *env, jclass type) {

    master_terminal_fd = getpt();
    __android_log_print(ANDROID_LOG_INFO,APPNAME,"running");
    __android_log_print(ANDROID_LOG_INFO,APPNAME,"ptsname:%s, pt:%d",ptsname(master_terminal_fd), master_terminal_fd);

    pid_t pid=fork();
    __android_log_print(ANDROID_LOG_INFO,APPNAME,"pid:%d",pid);

    if(pid<0){
        __android_log_print(ANDROID_LOG_INFO,APPNAME,"could not fork");
    }
    if(pid==0){
        setsid();
        unlockpt(master_terminal_fd);
        grantpt(master_terminal_fd);
        int slave_terminal=open(ptsname(master_terminal_fd),O_RDWR);
        ioctl(slave_terminal, TIOCSCTTY, 0);


        dup2(slave_terminal, 0);
        dup2(slave_terminal, 1);
        dup2(slave_terminal, 2);


        __android_log_print(ANDROID_LOG_INFO,APPNAME,"will now start shell");
        int ret=execl(strdup("/system/bin/sh"),strdup("/system/bin/sh"),(char*)0);
        __android_log_print(ANDROID_LOG_INFO,APPNAME,"this code should not be reached, ret:%d errno:%s",ret,strerror(errno));

        exit(-1);
    } else {
        /*
        sleep(5);
        unsigned char h[500];
        char* msg=strdup("ps\nls\n");write(master_terminal_fd,msg,strlen(msg));
        /**/
    }
}


}
