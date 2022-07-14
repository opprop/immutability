#!/bin/bash

echo Entering "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")" in `pwd`

set -e

export SHELLOPTS

if [ "$(uname)" == "Darwin" ] ; then
  export JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
else
  export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which javac))))}
fi

if [ -d "/tmp/plume-scripts" ] ; then
  git -C /tmp/plume-scripts pull -q
else
  git -C /tmp clone --depth 1 -q https://github.com/eisop-plume-lib/plume-scripts.git
fi


export CFI="${CFI:-$(pwd -P)/../checker-framework-inference}"

## Build Checker Framework Inference (which also clones & builds dependencies)
/tmp/plume-scripts/git-clone-related opprop checker-framework-inference ${CFI}
(cd $CFI && ./.ci-build-without-test.sh)

./gradlew assemble

echo Exiting "$(cd "$(dirname "$0")" && pwd -P)/$(basename "$0")" in `pwd`
