#!/bin/bash

set -e # fail fast

source "$(dirname ${0})/config.sh"

# a temp directory where training events will be stored
event_dir="${datadir}/events"
mkdir -p "${event_dir}"

# clobber the log file
log_file="${datadir}/log"
if [ -e ${log_file} ]; then
    rm "${log_file}"
fi

# step 1: alphabet creation
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m\
  edu.cmu.cs.lti.ark.fn.identification.AlphabetCreationThreaded \
  train-fefile:${fe_file} \
  train-parsefile:${parsed_file} \
  stopwords-file:${stopwords_file} \
  wordnet-configfile:${wordnet_config_file} \
  fnidreqdatafile:${fn_id_req_data_file} \
  logoutputfile:${log_file} \
  model:${datadir}/alphabet.dat \
  eventsfile:${event_dir} \
  startindex:0 \
  endindex:${fe_file_length} \
  numthreads:${num_threads}
