#!/bin/bash

set -e # fail fast
set -x

echo
echo "step 4i: create the alphabet file for the argument identification model."
echo

source "$(dirname ${BASH_SOURCE[0]})/config.sh"

mkdir -p ${SCAN_DIR}

dep_parsed_file="${training_dir}/cv.train.sentences.${dep_parser}parsed.conll"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4g -Xmx4g \
   edu.cmu.cs.lti.ark.fn.parsing.CacheFrameFeaturesApp \
   ${fe_file} \
   ${dep_parsed_file} \
   ${SCAN_DIR}/train.featurecache.jobj \
   ${SCAN_DIR}/parser.conf.unlabeled
