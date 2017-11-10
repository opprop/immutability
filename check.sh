#!/bin/bash
set -e
# Keep the environment settings from setup.sh script
export MYDIR=`dirname $0`
. ./$MYDIR/setup.sh

export CLASSPATH=./src/main/java:$CLASSPATH

DEBUG=""

declare -a ARGS
for i in "$@" ; do
    if [[ $i == "-d" ]] ; then
        echo "Typecheck using debug mode. Listening at port 5050. Waiting for connection...."
        DEBUG="-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5050"
        continue
    fi
    ARGS[${#ARGS[@]}]="$i"
done

cmd=""

if [ "$DEBUG" == "" ]; then
	cmd="javac -cp build/classes/main -processor pico.typecheck.PICOChecker "${ARGS[@]}""
else
	cmd="javac "$DEBUG" -cp build/classes/main -processor pico.typecheck.PICOChecker "${ARGS[@]}""
fi

eval "$cmd"
