#!/bin/bash

TESTFILE=testinput/inference/inferrable/FieldAssignCase3.java
ANNOTATEDFILE=annotated/FieldAssignCase3.java

subl $TESTFILE

./infer.sh $TESTFILE

subl $ANNOTATEDFILE
