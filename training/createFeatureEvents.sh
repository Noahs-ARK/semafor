#!/bin/bash -e

source "$(dirname ${0})/config"

# step 3.3: creating feature events for each datapoint

training_dir="${datadir}/naacl2012"
fe_file="${training_dir}/cv.train.sentences.frame.elements"
fe_file_length=`wc -l ${fe_file}`
fe_file_length=`expr ${fe_file_length% *}`
parsed_file="${training_dir}/cv.train.sentences.all.lemma.tags"

# clobber the log file
log_file="${datadir}/log"
if [ -e ${log_file} ]; then
    rm "${log_file}"
fi

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m \
  edu.cmu.cs.lti.ark.fn.identification.CreateEventsUnsupported \
  train-fefile:${fe_file} \
  train-parsefile:${parsed_file} \
  stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
  wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
  fnidreqdatafile:${datadir}/reqData.jobj \
  logoutputfile:${datadir}/log \
  model:${datadir}/alphabet_combined.dat \
  eventsfile:${datadir}/events \
  startindex:0 \
  endindex:${fe_file_length} \
  numthreads:4
