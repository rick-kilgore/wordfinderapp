#!/bin/zsh

if [ `uname` = 'MacOS' ]; then
  alias grep=ggrep
fi
PATH="/opt/gradle-7.6/bin:/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home:$PATH"

cp -v wwf.txt java/app/src/main/assets/
cp -v scrabble_words.txt java/app/src/main/assets/
gradle -p java/app assembleRelease |& tee -a mk.log
./clean.sh

if test -e mk.log; then
  cat mk.log
fi

numdevs=`adb devices | grep -iPv 'list of devices|^\s*$' | wc -l | tr -d ' '`
if test $numdevs != '0'; then
  adb install -r java/app/build/outputs/apk/release/app-release.apk
  /Users/rkilgore/bin/usleep 250
  adb shell am start -n org.rkilgore.wordfinderapp/.MainActivity
fi
