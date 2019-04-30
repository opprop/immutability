#!/bin/bash
# Fail the whole script if any command fails
set -e

# Environment variables setup
export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(dirname $(readlink -f $(/usr/bin/which java)))))}
export JSR308=$(cd $(dirname "$0")/.. && pwd)
export AFU=$JSR308/annotation-tools/annotation-file-utilities
export CHECKERFRAMEWORK=$JSR308/checker-framework
export PATH=$AFU/scripts:$JAVA_HOME/bin:$PATH

#default value is opprop. REPO_SITE may be set to other value for travis test purpose.
export REPO_SITE=baoruiz

echo "------ Downloading everthing from REPO_SITE: $REPO_SITE ------"

# Clone checker-framework
if [ -d $JSR308/checker-framework ] ; then
    (cd $JSR308/checker-framework && git checkout pull-pico-changes && git pull)
else
    (cd $JSR308 && git clone -b pull-pico-changes --depth 1 https://github.com/"$REPO_SITE"/checker-framework.git)
fi

# Clone checker-framework-inference
if [ -d $JSR308/checker-framework-inference ] ; then
    (cd $JSR308/checker-framework-inference && git checkout pull-pico-changes && git pull)
else
    (cd $JSR308 && git clone -b pull-pico-changes --depth 1 https://github.com/"$REPO_SITE"/checker-framework-inference.git)
fi

# Build checker-framework, with downloaded jdk
(cd $JSR308/checker-framework && ./.travis-build-without-test.sh downloadjdk)

# Build checker-framework-inference
(cd $JSR308/checker-framework-inference && ./gradlew dist)

# Build PICO
(cd $JSR308/immutability && ./gradlew build)
