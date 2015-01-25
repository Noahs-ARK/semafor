#!/bin/bash

set -e # fail fast

echo
echo "step 4iii: Training."
echo

source "$(dirname ${BASH_SOURCE[0]})/config.sh"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms6500m -Xmx6500m \
  edu.cmu.cs.lti.ark.fn.parsing.TrainArgIdApp \
  model:${model_dir}/argmodel.dat \
  alphabetfile:${SCAN_DIR}/parser.conf.unlabeled \
  localfeaturescache:${SCAN_DIR}/featurecache.jobj \
  lambda:1.0 \
  numthreads:${num_threads} \
  batch-size:256
