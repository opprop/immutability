#!/bin/bash

# TESTFILE=testinput/typecheck/CopyToCast.java
# TESTFILE=testinput/typecheck/ImmutableListProblem.java
TESTFILE=testinput/typecheck/ImmutablePerson.java

subl $TESTFILE

./check.sh $TESTFILE
