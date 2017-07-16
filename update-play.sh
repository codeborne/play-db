#!/bin/bash

BANK_DIR=`pwd`
source `dirname "$BASH_SOURCE"`/play-env.sh

function build_play() {
  cd $PLAY_HOME/framework
  echo ""
  echo "Building play $NEW_REV ..."
  rm -fr *.jar
  time ant >> $BANK_DIR/conf/update-play.log 2>&1 || exit $?
  ln -s play-* play.jar
  echo "Built framework/`find . -name play-*.jar`"
}

pushd .

echo "Update Play! framework at $PLAY_HOME"

if [ ! -d $PLAY_HOME ]; then
  git clone --depth=10 -b $PLAY_BRANCH $PLAY_REPO $PLAY_HOME || exit $?
  build_play
elif [ -e $PLAY_HOME/.git/index.lock ]; then
  echo "Git lock exists, skipping Play update because other process is probably doing that"
else
  cd $PLAY_HOME
  OLD_REV=`git rev-parse HEAD`
  git fetch origin $PLAY_BRANCH || exit $?
  git checkout $PLAY_BRANCH
  git reset --hard origin/$PLAY_BRANCH || exit $?
  NEW_REV=`git rev-parse --short HEAD`
  git log $OLD_REV..$NEW_REV --pretty=format:"%h %ad %an : %s" --date=iso8601 --no-merges --graph
  [ "`find framework -name play-*.jar`" != "framework/play-1.5.x-$NEW_REV.jar" ] && build_play
fi
popd
