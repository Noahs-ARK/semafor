#!/bin/bash -e

source "$(dirname `readlink -f ${0}`)/config"

# make sure RequiredDataCreation is compiled
${JAVA_HOME_BIN}/javac -cp ${classpath} edu/cmu/cs/lti/ark/fn/identification/RequiredDataCreation.java
# run it
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms2g -Xmx2g \
    edu.cmu.cs.lti.ark.fn.identification.RequiredDataCreation \
      stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
      wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
      framenet-mapfile:${datadir}/framenet.original.map \
      luxmldir:${luxmldir} \
      allrelatedwordsfile:${datadir}/allrelatedwords.ser \
      hvcorrespondencefile:${datadir}/hvmap.ser \
      wnrelatedwordsforwordsfile:${datadir}/wnallrelwords.ser \
      wnmapfile:${datadir}/wnMap.ser \
      revisedmapfile:${datadir}/revisedrelmap.ser \
      lemmacachefile:${datadir}/hvlemmas.ser \
      fnidreqdatafile:${datadir}/reqData.jobj
