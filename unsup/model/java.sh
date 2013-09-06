#!/bin/zsh
here=$(dirname $0)
CP=${CP:-""}
CP="$CP:$here/bin"   ## eclipse build dir
CP="$CP:$(echo $here/lib/*.jar | tr ' ' ':')"
# echo $CP
java -Xmx3g -XX:ParallelGCThreads=1 -ea -cp "$CP" "$@"
