#!/bin/bash

# Environment
export JSR308=$(cd $(dirname "$0")/.. && pwd)
export CF=$JSR308/checker-framework
export CFI=$JSR308/checker-framework-inference

export PICO=$(cd $(dirname "$0") && pwd)

# Dependencies
export CLASSPATH=$PICO/build/classes/java/main:$CFI/dist/checker-framework-inference.jar
export external_checker_classpath=$PICO/build/classes/java/main:$PICO/build/resources/main

export AFU=$JSR308/annotation-tools/annotation-file-utilities
export PATH=$AFU/scripts:$PATH
export JDK_JAR=$CF/checker/dist/jdk8.jar

CHECKER=pico.inference.PICOInferenceChecker

SOLVER=pico.inference.solver.PICOSolverEngine

STUBS="src/main/java/pico/inference/jdk.astub"

declare -a ARGS
for i in "$@" ; do
    if [[ $i == "-ds" ]] ; then
        echo "Configured to use debug solver"
        SOLVER="checkers.inference.solver.DebugSolver"
        continue
    fi
    ARGS[${#ARGS[@]}]="$i"
done

SOLVER_ARGS=useGraph=false,collectStatistic=true

IS_HACK=true

# echo "${ARGS[@]}"

# Start the inference
$CFI/scripts/inference-dev -m ROUNDTRIP --checker "$CHECKER" --solver "$SOLVER" \
    --solverArgs="useGraph=false,collectStatistic=true" --hacks="$IS_HACK" \
    --cfArgs="-Astubs=$STUBS" \
    -afud ./annotated "${ARGS[@]}"
