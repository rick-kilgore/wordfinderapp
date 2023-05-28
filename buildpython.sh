#!/bin/zsh

source ~/.venv/bin/activate
if [ `uname` = 'Linux' ]; then
  ver=`python -V | perl -pe 's/^Python //; s/\.\d+$//'`
  hdir=/usr/include/python$ver
  libdir=/usr/lib/python$ver
else
  hdir=`echo /opt/homebrew/Cellar/python*/*/Frameworks/Python.framework/Headers`
  libdir=`echo /opt/homebrew/Cellar/python*/*/Frameworks/Python.framework/Versions/Current/lib`
fi

echo hdir is "$hdir"
echo libdir is "$libdir"

cd python
./gen_full.py && \
cython wf.py --embed |& tee .mk.log && \
gcc wf.c -o wf -I$hdir -L$libdir \
    -lpython3.10 -O2 |& tee -a .mk.log && \
mv wf ../
cd ..
if test "x$1" = "x-i"; then
  mv wf ~/bin/
fi

