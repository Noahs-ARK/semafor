#!/bin/bash

set -e # fail fast
set -x

echo
echo "step 4ii: Training."
echo

source "$(dirname ${BASH_SOURCE[0]})/config.sh"
models_to_save=30
# l1, l2 and stopping criterion parameters obtained after tuning on dev set (Swabha)
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms20g -Xmx20g \
  edu.cmu.cs.lti.ark.fn.parsing.TrainArgIdApp \
  model:${model_dir}/argmodel.dat \
  alphabetfile:${SCAN_DIR}/parser.conf.unlabeled \
  localfeaturescache:${SCAN_DIR}/train.featurecache.jobj \
  l1-strength:0.00 \
  l2-strength:1e-8 \
  batch-size:4000 \
  save-every-k-batches:400 \
  num-models-to-save:$models_to_save 
  #warm-start-model:${model_dir}/argmodel.dat

rm -f ${model_dir}/argmodel.dat # remove the symbolic link
ln -s ${model_dir}/argmodel.dat_00$((models_to_save-1)) ${model_dir}/argmodel.dat

rm -f ${model_dir}/parser.conf
ln -s ${SCAN_DIR}/parser.conf.unlabeled ${model_dir}/parser.conf 

