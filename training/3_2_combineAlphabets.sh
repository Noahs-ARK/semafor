#!/bin/bash -e

source "$(dirname ${0})/config.sh"

# step 3.2: combine alphabets
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m\
  edu.cmu.cs.lti.ark.fn.identification.CombineAlphabets \
  ${datadir} \
  ${alphabet_file}
