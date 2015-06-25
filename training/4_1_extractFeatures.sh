#!/bin/bash

set -e # fail fast
set -x

echo
echo "step 4i: create the alphabet file for the argument identification model."
echo

source "$(dirname ${BASH_SOURCE[0]})/config.sh"

mkdir -p ${SCAN_DIR}


${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms20g -Xmx20g \
   edu.cmu.cs.lti.ark.fn.parsing.CacheFrameFeaturesApp \
   ${fe_file} \
   ${parsed_file} \
   ${SCAN_DIR}/train.featurecache.jobj \
   ${SCAN_DIR}/parser.conf.unlabeled
