#!/bin/bash

set -e # fail fast

source "$(dirname ${0})/config.sh"

echo
echo "RequiredDataCreation"
echo
mkdir -p "${model_dir}"

## config files
#wordnet_config_file="${SEMAFOR_HOME}/dict/file_properties.xml"
#stopwords_file="${SEMAFOR_HOME}/dict/stopwords.txt"
## the directory that contains all the lexical unit xmls for FrameNet 1.5
## you can also add your own xmls to this directory
## for format information, take a look at the lu/ directory under the FrameNet release
#luxmldir="${datadir}/framenet15/lu"
#framenet_map_file="${datadir}/framenet.original.map"
#all_related_words_file="${model_dir}/allrelatedwords.ser"
#hv_correspondence_file="${model_dir}/hvmap.ser"
#wn_related_words_for_words_file="${model_dir}/wnallrelwords.ser"
#wn_map_file="${model_dir}/wnMap.ser"
#revised_map_file="${model_dir}/revisedrelmap.ser"
#lemma_cache_file="${model_dir}/hvlemmas.ser"
#
#${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms2g -Xmx2g -XX:ParallelGCThreads=2 \
#    edu.cmu.cs.lti.ark.fn.identification.RequiredDataCreation \
#      stopwords-file:${stopwords_file} \
#      wordnet-configfile:${wordnet_config_file} \
#      framenet-mapfile:${framenet_map_file} \
#      luxmldir:${luxmldir} \
#      allrelatedwordsfile:${all_related_words_file} \
#      hvcorrespondencefile:${hv_correspondence_file} \
#      wnrelatedwordsforwordsfile:${wn_related_words_for_words_file} \
#      wnmapfile:${wn_map_file} \
#      revisedmapfile:${revised_map_file} \
#      lemmacachefile:${lemma_cache_file} \
#      fnidreqdatafile:${fn_id_req_data_file}

ln -s "${old_model_dir}/"{*.{map,jobj,ser,conf,mco,gz},argmodel.dat} "${model_dir}/"
