#!/bin/bash
ROOT=$TRAVIS_BUILD_DIR/..
# Fail the whole script if any command fails
set -e
export SHELLOPTS

# Environment variables setup
export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(dirname $(readlink -f $(/usr/bin/which java)))))}
export JSR308=$ROOT
export AFU=$ROOT/annotation-tools/annotation-file-utilities
export CHECKERFRAMEWORK=$ROOT/checker-framework
export PATH=$AFU/scripts:$JAVA_HOME/bin:$PATH

# Split $TRAVIS_REPO_SLUG into the owner and repository parts
SLUGOWNER=opprop
SLUGREPO=${TRAVIS_REPO_SLUG##*/}

echo "------ Downloading everthing from SLUG_OWNER: $SLUGOWNER ------"

# Clone annotation-tools (Annotation File Utilities)
if [ -d $JSR308/annotation-tools ] ; then
    (cd $JSR308/annotation-tools && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$SLUGOWNER"/annotation-tools.git)
fi

# Clone stubparser
if [ -d $JSR308/stubparser ] ; then
    (cd $JSR308/stubparser && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$SLUGOWNER"/stubparser.git)
fi
# Clone checker-framework
if [ -d $JSR308/checker-framework ] ; then
    (cd $JSR308/checker-framework && git checkout pico-dependant && git pull)
else
    # ViewpointAdapter changes are not yet merged to master, so we depend on pico-dependant branch
    (cd $JSR308 && git clone -b pico-dependant --depth 1 https://github.com/"$SLUGOWNER"/checker-framework.git)
fi

# Clone checker-framework-inference
if [ -d $JSR308/checker-framework-inference ] ; then
    (cd $JSR308/checker-framework-inference && git checkout pico-dependant && git pull)
else
    # Again we depend on pico-dependant branch
    (cd $JSR308 && git clone -b pico-dependant --depth 1 https://github.com/"$SLUGOWNER"/checker-framework-inference.git)
fi

# Build annotation-tools (and jsr308-langtools)
(cd $JSR308/annotation-tools/ && ./.travis-build-without-test.sh)
# Build stubparser
(cd $JSR308/stubparser/ && mvn package -Dmaven.test.skip=true)
# Build checker-framework, with downloaded jdk
(cd $JSR308/checker-framework && ant -f checker/build.xml dist-downloadjdk)
# Build checker-framework-inference
(cd $JSR308/checker-framework-inference && gradle dist)

# Build PICO
(cd $JSR308/immutability && gradle build)
