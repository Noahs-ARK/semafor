#!/bin/bash

set -e # fail fast

source "$(dirname ${0})/config.sh"

# step 3.3: creating feature events for each datapoint
echo
echo "step 3.3: creating feature events for each datapoint"
echo

# clobber the log file
log_file="${model_dir}/log"
if [ -e ${log_file} ]; then
    rm "${log_file}"
fi

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m \
  edu.cmu.cs.lti.ark.fn.identification.CreateEventsUnsupported \
  train-fefile:${fe_file} \
  train-parsefile:${parsed_file} \
  stopwords-file:${stopwords_file} \
  wordnet-configfile:${wordnet_config_file} \
  fnidreqdatafile:${fn_id_req_data_file} \
  logoutputfile:${model_dir}/log \
  model:${model_dir}/alphabet_combined.dat \
  eventsfile:${model_dir}/events \
  startindex:0 \
  endindex:${fe_file_length} \
  numthreads:${num_threads}
