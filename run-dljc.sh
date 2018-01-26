#!/bin/bash

WORKING_DIR=$(pwd)

JSR308=$JSR308

DLJC="$JSR308"/do-like-javac
export AFU="$JSR308"/annotation-tools/annotation-file-utilities
export PATH="$PATH":"$AFU"/scripts

export CLASSPATH="$JSR308"/immutability/build/classes/main:$CHECKERFRAMEWORK/dataflow/build:$CHECKERFRAMEWORK/javacutil/build:$CHECKERFRAMEWORK/stubparser/build:$CHECKERFRAMEWORK/framework/build:$CHECKERFRAMEWORK/checker/build:$SOLVER/bin:$CHECKERFRAMEWORK/framework/tests/junit-4.12.jar:$CHECKERFRAMEWORK/framework/tests/hamcrest-core-1.3.jar:$CFI/bin:$CFI/dist/org.ow2.sat4j.core-2.3.4.jar:$CFI/dist/commons-logging-1.2.jar:$CFI/dist/log4j-1.2.16.jar:$JSR308/jsr308-langtools/build/classes:$SOLVER/bin:$CLASSPATH

#parsing build command of the target program
build_cmd="$2"
for i in ${@:3}
do
    build_cmd="$build_cmd "${i}""
done

#Typechecking or inference
if [[ "$1" = "-t" ]] ; then
    echo "Running typechecking"
    CHECKER="pico.typecheck.PICOChecker"
    #checker tool doesn't support --cfArgs yet, so the arguments don't have effect right now
    running_cmd="python $DLJC/dljc -t checker --checker "${CHECKER}" --cfArgs=\"-AprintErrorStack,-AprintFbcErrors\" --log_to_stderr -- $build_cmd"
elif [[ "$1" = "-i" ]] ; then
    echo "Running inference"
    echo "Cleaning logs and annotated directory from previous result"
    rm -rf logs annotated
    echo "Cleaning Done."
    CHECKER="pico.inference.PICOInferenceChecker"
    SOLVER="pico.inference.solver.PICOSolverEngine"
    running_cmd="python $DLJC/dljc -t inference --checker "${CHECKER}" --cfArgs=\"-AprintErrorStack\" --solver "${SOLVER}" --solverArgs=\"collectStatistic=true,useGraph=false\" --mode ROUNDTRIP -afud $WORKING_DIR/annotated -o logs -- $build_cmd "
else
    echo "Unknown tool: should be either -t|-i but found: ${1}"
    exit 1
fi

echo "============ Important variables ============="
echo "JSR308: $JSR308"
echo "CLASSPATH: $CLASSPATH"
echo "build cmd: $build_cmd"
echo "running cmd: $running_cmd"
echo "=============================================="

echo "Start Running...."
eval "$running_cmd"
