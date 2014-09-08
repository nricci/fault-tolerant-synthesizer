#!/bin/bash

cd output/;

for dot_file in `ls *.dot`; do
    dot -Tjpeg $dot_file > $dot_file.jpeg
done

for dir in `ls -d */`; do
    cd $dir;
    for dot_file in `ls *.dot`; do
        dot -Tjpeg $dot_file > $dot_file.jpeg
    done
    cd ..
done

echo "dot2jpeg done."
