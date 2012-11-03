#!/bin/bash -e

# step 4: training the frame identification model.

source "$(dirname ${0})/config"

model_name="models_0.0"
model_dir="${datadir}/${model_name}"

mkdir -p ${model_dir}

# clobber the log file
log_file="${datadir}/log"
if [ -e ${log_file} ]; then
    rm "${log_file}"
fi


${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8g -Xmx8g \
  edu.cmu.cs.lti.ark.fn.identification.TrainBatchModelDerThreaded \
  alphabetfile:${datadir}/alphabet_combined.dat \
  eventsfile:${datadir}/events \
  model:${model_dir}/idmodel.dat \
  regularization:reg \
  lambda:0.0 \
  restartfile:null \
  logoutputfile:${datadir}/log \
  numthreads:${num_threads}
