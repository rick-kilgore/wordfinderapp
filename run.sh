#!/bin/zsh

export PATH="/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin:$PATH"
java -jar java/wordfinder/build/libs/wordfinder.jar "$@"
