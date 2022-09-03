#!/bin/zsh

#/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/javac -d . java/org/rkilgore/*.java >& mk.log && \
#/usr/bin/jar cvf wordfinder.jar org && \

PATH="/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home;$PATH"

gradle -p java/wordfinder jar |& tee mk.log
gradle -p java/app assembleRelease |& tee -a mk.log
./clean.sh && \

if test -e mk.log; then
  cat mk.log
fi

if test "x$1" = "x-i"; then
  adb install -r java/app/build/outputs/apk/release/app-release.apk
  adb shell am start -n org.rkilgore.wordfinderapp/.MainActivity
fi
