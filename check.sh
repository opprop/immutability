#!/bin/bash
set -e


export JSR308=$(cd $(dirname "$0")/.. && pwd)

# Playground top level directory
export IMMUTABILITY=$(cd $(dirname "$0") && pwd)

export CFI=$JSR308/checker-framework-inference

# Dependencies
export CLASSPATH=$IMMUTABILITY/build/classes/main:$CHECKERFRAMEWORK/dataflow/build:$CHECKERFRAMEWORK/javacutil/build:$CHECKERFRAMEWORK/stubparser/build:$CHECKERFRAMEWORK/framework/build:$CHECKERFRAMEWORK/checker/build:$SOLVER/bin:$CHECKERFRAMEWORK/framework/tests/junit-4.12.jar:$CHECKERFRAMEWORK/framework/tests/hamcrest-core-1.3.jar:$CFI/bin:$CFI/dist/org.ow2.sat4j.core-2.3.4.jar:$CFI/dist/commons-logging-1.2.jar:$CFI/dist/log4j-1.2.16.jar:$JSR308/jsr308-langtools/build/classes:$CLASSPATH


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
	cmd="javac "$DEBUG" -cp "${CLASSPATH}" -processor "${CHECKER}" -AatfDoNotCache "${ARGS[@]}""
fi

eval "$cmd"
