#!/bin/bash

export PATH="/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin:$PATH"

cd $HOME/local/wordfinder
java -jar java/wordfinder/build/libs/wordfinder.jar "$@"
