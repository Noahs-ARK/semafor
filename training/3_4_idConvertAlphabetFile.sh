#!/bin/bash

set -e # fail fast

# step 5: convert the alphabet file.

source "$(dirname ${0})/config.sh"

# gets the last model file created
model_file="$(ls ${model_dir}/idmodel.dat_* | sort -r | head -n1)"
echo
echo "Combining alphabet file with learned params for Frame IDing"
echo "Using model file: ${model_file}"
echo

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8g -Xmx8g -XX:ParallelGCThreads=${gc_threads} \
  edu.cmu.cs.lti.ark.fn.identification.training.ConvertAlphabetFile \
  ${alphabet_file} \
  ${model_file} \
  ${model_dir}/idmodel.dat \
  ${id_features}
