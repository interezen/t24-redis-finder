#!/bin/sh

# Package:   Redis Finder [java 1.8]
# File:      run.sh (Bourne shell script)
# Author:
# Comments:
# Copyright (C) Interezen co. - All Rights Reserved

# PARAMETER SETTING ###################################################
JAR="./t24-redis-finder.jar"
JDK="../../jdk1.8.0_111"
NOW=$(date +"%F")
LOG_DIR=logs
LOG_FILE=console.log.${NOW}
OPT="-Xms1g -Xmx1g"
DUMP="-Dlogback.configurationFile=./logback.xml -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+DisableExplicitGC -Djava.awt.headless=true"
#######################################################################

export LD_LIBRARY_PATH=.:./lib
export JAVA_HOME=${JDK}
export PATH=${JAVA_HOME}/bin:$PATH
export CLASS_PATH=.:./lib:./logback.xml
export LANG=ko_KR.UTF-8

case "$1" in
        'start')
                ############## S t a r t  d a e m o n ##############
                if ! test -d ${LOG_DIR} ; then
                        mkdir ${LOG_DIR}
                fi

                PROC=`ps -ef | grep ${JDK} | grep java | grep -v grep | grep ${JAR} | wc -l`
                if [ ${PROC} -gt 0 ]
                then
                    echo "Process Running Now...Please Check Process..Exit."
                    exit 1
                else
                    if [ -e ${JAR} ]
                    then
                            JAVA_EXE=`which java`
                            echo "Starting Server"
                            exec ${JAVA_EXE} ${OPT} ${DUMP} -jar ${JAR} -server -vmargs -Xverify:none -XX:+HeapDumpOnCtrlBreak > ${LOG_DIR}/${LOG_FILE} 2>&1 &
                    fi
                fi
                ;;
         'stop')
                ############## S t o p  d a e m o n ##############
                echo "Shutting down Server"
                PID=`ps ax | grep ${JDK} | grep java | grep ${JAR} | awk '{ print $1 }'`

                for JAR_PID in ${PID}
                do
                        if [ ${JAR_PID} ] && [ ${JAR_PID} -gt 0 ]; then
                                kill -9 ${JAR_PID}

                                if [ $? -eq 0 ]; then
                                        echo "Done."
                                fi
                        fi
                done
                ;;
esac

exit 0