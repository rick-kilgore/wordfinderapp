#!/bin/zsh

#/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/javac -d . java/org/rkilgore/*.java >& mk.log && \
#/usr/bin/jar cvf wordfinder.jar org && \

gradle -p java/wordfinder jar
./clean.sh && \
if test "x$1" = "x-i"; then
  mv wf ~/bin/
fi

if test -e mk.log; then
  cat mk.log
fi
