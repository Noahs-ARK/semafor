#!/bin/bash -e

source "$(dirname `readlink -f ${0}`)/../release/config"
#source ../release/config

# the directory that contains framenet.frame.element.map and framenet.original.map
datadir="${SEMAFOR_HOME}/training"
# the directory that contains all the lexical unit xmls for FrameNet 1.5
# you can also add your own xmls to this directory
# for format information, take a look at the lu/ directory under the FrameNet release
luxmldir="/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/lu"
classpath="..:${SEMAFOR_HOME}:${SEMAFOR_HOME}/lib/semafor-deps.jar"

echo $datadir
echo $luxmldir
echo $classpath

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms2g -Xmx2g edu.cmu.cs.lti.ark.fn.identification.RequiredDataCreation \
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
