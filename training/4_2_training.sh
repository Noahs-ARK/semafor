#!/bin/bash

set -e # fail fast
set -x

echo
echo "step 4ii: Training."
echo

source "$(dirname ${BASH_SOURCE[0]})/config.sh"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms5g -Xmx5g \
  edu.cmu.cs.lti.ark.fn.parsing.TrainArgIdApp \
  model:${model_dir}/argmodel.dat \
  alphabetfile:${SCAN_DIR}/parser.conf.unlabeled \
  localfeaturescache:${SCAN_DIR}/train.featurecache.jobj \
  l1-strength:0 \
  l2-strength:1e-7 \
  batch-size:4000 \
  save-every-k-batches:400 \
  num-models-to-save:30
#  warm-start-model:${model_dir}/argmodel.dat
