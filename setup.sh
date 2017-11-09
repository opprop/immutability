#!/bin/bash
export MYDIR=`dirname $0`

# Playground top level directory
export IMMUTABILITY=$MYDIR/

# CheckerFramework Inference
export CFI="$JSR308"/checker-framework-inference

# Dependencies
export CLASSPATH=$IMMUTABILITY/build/classes/main:$CHECKERFRAMEWORK/dataflow/build:$CHECKERFRAMEWORK/javacutil/build:$CHECKERFRAMEWORK/stubparser/build:$CHECKERFRAMEWORK/framework/build:$CHECKERFRAMEWORK/checker/build:$SOLVER/bin:$CHECKERFRAMEWORK/framework/tests/junit-4.12.jar:$CHECKERFRAMEWORK/framework/tests/hamcrest-core-1.3.jar:$CFI/bin:$CFI/dist/org.ow2.sat4j.core-2.3.4.jar:$CFI/dist/commons-logging-1.2.jar:$CFI/dist/log4j-1.2.16.jar:$JSR308/jsr308-langtools/build/classes:$CLASSPATH
