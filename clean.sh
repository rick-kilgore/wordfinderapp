#!/bin/zsh

rm -rf python/*.c *.pickle python/wf.py __pycache__

if test "x$1" = "x-f"; then
  rm -rf wf java/wordfinder/build
fi
