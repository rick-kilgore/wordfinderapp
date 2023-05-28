#!/bin/zsh

./buildpython.sh "$@" |& tee .mk.log
./buildjava.sh "$@" |& tee -a .mk.log
