#!/bin/bash

set -e # fail fast

echo
echo "step 4: training the frame identification model."
echo

source "$(dirname ${0})/config.sh"

mkdir -p ${model_dir}

# clobber the log file
log_file="${model_dir}/log"
if [ -e ${log_file} ]; then
    rm "${log_file}"
fi

${JAVA_HOME_BIN}/java \
    -classpath ${classpath} \
    -Xms1g \
    -Xmx8g \
    -XX:ParallelGCThreads=${gc_threads} \
  edu.cmu.cs.lti.ark.fn.identification.training.TrainBatch \
  alphabetfile:${model_dir}/alphabet.dat \
  eventsfile:${model_dir}/events \
  model:${model_dir}/idmodel.dat \
  regularization:l1 \
  lambda:1.0 \
  restartfile:null \
  logoutputfile:${model_dir}/log \
  numthreads:${num_threads} \
  use-partial-credit:true \
  cost-multiple:5
