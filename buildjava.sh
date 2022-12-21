#!/bin/zsh

PATH="/opt/gradle-7.6/bin:/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home:$PATH"

gradle -p java/wordfinder jar |& tee mk.log
cp java/wordfinder/build/libs/wordfinder.jar .
./clean.sh

if test -e mk.log; then
  cat mk.log
fi

