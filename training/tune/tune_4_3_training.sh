#!/bin/bash

set -e # fail fast

echo
echo "step 4iii: Training."
echo

source $(dirname ${BASH_SOURCE[0]})/tune_config.sh $1

#<<<<<<< HEAD
#${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m \
 # edu.cmu.cs.lti.ark.fn.parsing.Training \
 # model:${experiments_dir}/argmodel.dat \
#=======
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms5g -Xmx5g \
  edu.cmu.cs.lti.ark.fn.parsing.TrainArgIdApp \
  model:${model_dir}/argmodel.dat \
  alphabetfile:${SCAN_DIR}/parser.conf.unlabeled \
  localfeaturescache:${SCAN_DIR}/featurecache.jobj \
  l1-strength:$2 \
  l2-strength:$3 \
  batch-size:4000 \
  save-every-k-batches:400 \
  num-models-to-save:$4
  #warm-start-model:${model_dir}/argmodel.dat \
