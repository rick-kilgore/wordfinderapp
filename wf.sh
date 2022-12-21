#!/bin/bash

export PATH="/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin:$PATH"

cd $(dirname `realpath $0`)
java -jar wordfinder.jar "$@"
