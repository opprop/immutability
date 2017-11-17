#!/bin/bash
set -e
# Keep the environment settings from setup.sh script
export MYDIR=`dirname $0`
. ./$MYDIR/setup.sh

export CLASSPATH=build/classes/main:"$CFI"/dist/checker-framework-inference.jar:$CLASSPATH

DEBUG=""
CHECKER="pico.typecheck.PICOChecker"

declare -a ARGS
for i in "$@" ; do
    if [[ $i == "-d" ]] ; then
        echo "Typecheck using debug mode. Listening at port 5050. Waiting for connection...."
        DEBUG="-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5050"
        continue
    fi

    if [[ $i == "-i" ]] ; then
        echo "Typecheck using PICOInferenceChecker typechecking mode"
        CHECKER="pico.inference.PICOInferenceChecker"
        continue
    fi
    ARGS[${#ARGS[@]}]="$i"
done

cmd=""

if [ "$DEBUG" == "" ]; then
	cmd="javac -cp "${CLASSPATH}" -processor "${CHECKER}" "${ARGS[@]}""
else
	cmd="javac "$DEBUG" -cp "${CLASSPATH}" -processor "${CHECKER}" "${ARGS[@]}""
fi

eval "$cmd"
