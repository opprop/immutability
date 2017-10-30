#!/bin/bash

WORKING_DIR=$(pwd)

JSR308=$JSR308

DLJC="$JSR308"/do-like-javac
export AFU="$JSR308"/annotation-tools/annotation-file-utilities
export PATH="$PATH":"$AFU"/scripts

export CLASSPATH="$JSR308"/immutability/build/classes/main:$CHECKERFRAMEWORK/dataflow/build:$CHECKERFRAMEWORK/javacutil/build:$CHECKERFRAMEWORK/stubparser/build:$CHECKERFRAMEWORK/framework/build:$CHECKERFRAMEWORK/checker/build:$SOLVER/bin:$CHECKERFRAMEWORK/framework/tests/junit-4.12.jar:$CHECKERFRAMEWORK/framework/tests/hamcrest-core-1.3.jar:$CFI/bin:$CFI/dist/org.ow2.sat4j.core-2.3.4.jar:$CFI/dist/commons-logging-1.2.jar:$CFI/dist/log4j-1.2.16.jar:$JSR308/jsr308-langtools/build/classes:$SOLVER/bin:$CLASSPATH

#parsing build command of the target program
build_cmd="$1"
shift
while [ "$#" -gt 0 ]
do
    build_cmd="$build_cmd $1"
    shift
done


cd "$WORKING_DIR"
running_cmd="python $DLJC/dljc -t checker --checker pico.typecheck.PICOChecker -- $build_cmd "
echo "============ Important variables ============="
echo "JSR308: $JSR308"
echo "CLASSPATH: $CLASSPATH"
echo "build cmd: $build_cmd"
echo "running cmd: $running_cmd"
echo "=============================================="
eval "$running_cmd"
