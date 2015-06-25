#!/bin/bash

set -e # fail fast
set -x

echo
echo "step 4ii: Training."
echo

source "$(dirname ${BASH_SOURCE[0]})/config.sh"

# l1, l2 and stopping criterion parameters obtained after tuning on dev set (Swabha)
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms20g -Xmx20g \
  edu.cmu.cs.lti.ark.fn.parsing.TrainArgIdApp \
  model:${model_dir}/argmodel.dat \
  alphabetfile:${SCAN_DIR}/parser.conf.unlabeled \
  localfeaturescache:${SCAN_DIR}/train.featurecache.jobj \
  l1-strength:0.00 \
  l2-strength:1e-8 \
  batch-size:4000 \
  save-every-k-batches:30 \
  num-models-to-save:40 \
  warm-start-model:${model_dir}/argmodel.dat


cp ${model_dir}/argmodel.dat_0039 ${model_dir}/argmodel.dat
cp ${SCAN_DIR}/parser.conf.unlabeled ${model_dir}/parser.conf

