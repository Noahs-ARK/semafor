#!/bin/bash -e

source "$(dirname `readlink -f ${0}`)/config"

# a temp directory where training events will be stored
event_dir="${datadir}/events"
mkdir -p "${event_dir}"
# gold standard frame id annotations
#training_dir="/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/NAACL2012/"
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

# step 1: alphabet creation
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m\
  edu.cmu.cs.lti.ark.fn.identification.AlphabetCreationThreaded \
  train-fefile:${fe_file} \
  train-parsefile:${parsed_file} \
  stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
  wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
  fnidreqdatafile:${datadir}/reqData.jobj \
  logoutputfile:${log_file} \
  model:${datadir}/alphabet.dat \
  eventsfile:${event_dir} \
  startindex:0 \
  endindex:${fe_file_length} \
  numthreads:4
