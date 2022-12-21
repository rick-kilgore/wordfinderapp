#!/bin/zsh

rm -rf python/*.c *.pickle python/wf.py __pycache__ java/app/build java/wordfinder/build

if test "x$1" = "x-f"; then
  rm -rf wf
fi
