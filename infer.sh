#!/bin/bash

# Keep the environment settings from setup.sh script
export MYDIR=`dirname $0`
. ./$MYDIR/setup.sh

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
