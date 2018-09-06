#!/bin/bash


export JSR308=$(cd $(dirname "$0")/.. && pwd)

# Playground top level directory
export IMMUTABILITY=$(cd $(dirname "$0") && pwd)

export CFI=$JSR308/checker-framework-inference

# Dependencies
export CLASSPATH=$IMMUTABILITY/build/classes/main:$CHECKERFRAMEWORK/dataflow/build:$CHECKERFRAMEWORK/javacutil/build:$CHECKERFRAMEWORK/stubparser/build:$CHECKERFRAMEWORK/framework/build:$CHECKERFRAMEWORK/checker/build:$SOLVER/bin:$CHECKERFRAMEWORK/framework/tests/junit-4.12.jar:$CHECKERFRAMEWORK/framework/tests/hamcrest-core-1.3.jar:$CFI/bin:$CFI/dist/org.ow2.sat4j.core-2.3.4.jar:$CFI/dist/commons-logging-1.2.jar:$CFI/dist/log4j-1.2.16.jar:$JSR308/jsr308-langtools/build/classes:$CLASSPATH

# dist directory of CheckerFramework Inference
distDir=$CFI/dist

SOLVER="pico.inference.solver.PICOSolverEngine"

declare -a ARGS
for i in "$@" ; do
    if [[ $i == "-ds" ]] ; then
        echo "Configured to use debug solver"
        SOLVER="checkers.inference.solver.DebugSolver"
        continue
    fi
    ARGS[${#ARGS[@]}]="$i"
done

# echo "${ARGS[@]}"

# Start the inference: jar files are used when making inference
java -cp "$distDir"/checker.jar:"$distDir"/plume.jar:"$distDir"/checker-framework-inference.jar:$CLASSPATH checkers.inference.InferenceLauncher --checker pico.inference.PICOInferenceChecker --solver "$SOLVER" --solverArgs=useGraph=false,collectStatistic=true --hacks=true --afuOutputDir=./annotated -m ROUNDTRIP "${ARGS[@]}"
