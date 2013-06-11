#!/bin/bash

set -e # fail fast

source "$(dirname ${0})/config.sh"

# a temp directory where training events will be stored
event_dir="${model_dir}/events"
mkdir -p "${event_dir}"

log_file="${model_dir}/log"
if [ -e ${log_file} ]; then
    rm "${log_file}"
fi



# step 1: alphabet creation

echo
echo "Creating alphabet"
echo
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms6g -Xmx6g\
  edu.cmu.cs.lti.ark.fn.identification.AlphabetCreationThreaded \
  train-fefile:${fe_file} \
  train-parsefile:${parsed_file} \
  stopwords-file:${stopwords_file} \
  wordnet-configfile:${wordnet_config_file} \
  fnidreqdatafile:${fn_id_req_data_file} \
  logoutputfile:${log_file} \
  model:${model_dir}/alphabet.dat \
  eventsfile:${event_dir} \
  startindex:0 \
  endindex:${fe_file_length} \
  numthreads:${num_threads}
