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
SLUGOWNER=${TRAVIS_REPO_SLUG%/*}
SLUGREPO=${TRAVIS_REPO_SLUG##*/}

echo "------ Downloading everthing from SLUG_OWNER: $SLUGOWNER ------"

##### Clone checker-framework
if [ -d $JSR308/checker-framework ] ; then
    (cd $JSR308/checker-framework && git pull)
else
    # ViewpointAdaptor changes are not yet merged to master, so we depend on vputil-wrapup branch
    (cd $JSR308 && git clone -b vputil-wrapup --depth 1 https://github.com/"$SLUGOWNER"/checker-framework.git)
fi

# This also builds annotation-tools and jsr308-langtools
(cd $ROOT/checker-framework/ && ./.travis-build-without-test.sh downloadjdk)

###### Build PICO
(cd $JSR308/immutability && gradle build)
