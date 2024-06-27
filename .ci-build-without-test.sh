#!/bin/bash

echo Entering "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")" in `pwd`

set -e

export SHELLOPTS

if [ "$(uname)" == "Darwin" ] ; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which javac))))}
fi

if [ -d "/tmp/git-scripts" ] ; then
  git -C /tmp/git-scripts pull -q
else
  git -C /tmp clone --depth 1 -q https://github.com/eisop-plume-lib/git-scripts.git
fi


export CFI="${CFI:-$(pwd -P)/../checker-framework-inference}"

## Build Checker Framework Inference (which also clones & builds dependencies)
/tmp/git-scripts/git-clone-related opprop checker-framework-inference ${CFI}
(cd $CFI && ./.ci-build-without-test.sh)

./gradlew assemble

echo Exiting "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")" in `pwd`
