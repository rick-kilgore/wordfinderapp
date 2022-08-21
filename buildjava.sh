#!/bin/zsh

#/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/javac -d . java/org/rkilgore/*.java >& mk.log && \
#/usr/bin/jar cvf wordfinder.jar org && \

PATH="/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home;$PATH"

gradle -p java/wordfinder jar |& tee mk.log
./clean.sh && \

if test -e mk.log; then
  cat mk.log
fi
