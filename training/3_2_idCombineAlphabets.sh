#!/bin/bash

set -e # fail fast

source "$(dirname ${0})/config.sh"

echo
echo "step 3.2: combine alphabets"
echo
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8g -Xmx8g -XX:ParallelGCThreads=1 \
  edu.cmu.cs.lti.ark.fn.identification.CombineAlphabets \
  ${model_dir} \
  ${alphabet_file}
