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
export REPO_SITE=topnessman

echo "------ Downloading everthing from REPO_SITE: $REPO_SITE ------"

# Clone annotation-tools (Annotation File Utilities)
if [ -d $JSR308/annotation-tools ] ; then
    (cd $JSR308/annotation-tools && git pull)
else
    (cd $JSR308 && git clone -b pico-dependant-copy --depth 1 https://github.com/"$REPO_SITE"/annotation-tools.git)
fi

# Clone stubparser
if [ -d $JSR308/stubparser ] ; then
    (cd $JSR308/stubparser && git pull)
else
    (cd $JSR308 && git clone -b pico-dependant-copy --depth 1 https://github.com/"$REPO_SITE"/stubparser.git)
fi
# Clone checker-framework
if [ -d $JSR308/checker-framework ] ; then
    (cd $JSR308/checker-framework && git checkout pico-dependant-copy && git pull)
else
    # ViewpointAdapter changes are not yet merged to master, so we depend on pico-dependant branch
    (cd $JSR308 && git clone -b pico-dependant-copy --depth 1 https://github.com/"$REPO_SITE"/checker-framework.git)
fi

# Clone checker-framework-inference
if [ -d $JSR308/checker-framework-inference ] ; then
    (cd $JSR308/checker-framework-inference && git checkout pico-dependant-copy && git pull)
else
    # Again we depend on pico-dependant branch
    (cd $JSR308 && git clone -b pico-dependant-copy --depth 1 https://github.com/"$REPO_SITE"/checker-framework-inference.git)
fi

# Build annotation-tools (and jsr308-langtools)
(cd $JSR308/annotation-tools/ && ./.travis-build-without-test.sh)
# Build stubparser
(cd $JSR308/stubparser/ && mvn package -Dmaven.test.skip=true)
# Build checker-framework, with downloaded jdk
(cd $JSR308/checker-framework && ant -f checker/build.xml dist-downloadjdk)
# Build checker-framework-inference
(cd $JSR308/checker-framework-inference && gradle dist) # This step needs to be manually in $CFI executed due to path problems

# Build PICO
(cd $JSR308/immutability && ./gradlew build)
