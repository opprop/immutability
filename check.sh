#!/bin/bash
set -e

# Environment
export JSR308=$(cd $(dirname "$0")/.. && pwd)
export CF=$JSR308/checker-framework
export CFI=$JSR308/checker-framework-inference
export JAVAC=$CF/checker/bin/javac

export PICO=$(cd $(dirname "$0") && pwd)

# Dependencies
export CLASSPATH=$PICO/build/classes/java/main:$PICO/build/resources/main:\
$PICO/build/libs/immutability.jar:$CFI/dist/checker-framework-inference.jar

# Command
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
    cmd="$JAVAC -cp "${CLASSPATH}" -processor "${CHECKER}" "${ARGS[@]}""
else
    cmd="$JAVAC "$DEBUG" -cp "${CLASSPATH}" -processor "${CHECKER}" -AatfDoNotCache "${ARGS[@]}""
fi

eval "$cmd"
