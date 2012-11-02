#!/bin/bash -e

# step 4iii: Training.

source "$(dirname ${0})/config"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8000m -Xmx8000m \
  edu.cmu.cs.lti.ark.fn.parsing.TrainingBatchMain \
  model:${datadir}/argmodel.dat \
  alphabetfile:${training_dir}/scan/parser.conf.unlabeled \
  localfeaturescache:${training_dir}/scan/featurecache.jobj \
  train-framefile:${training_dir}/cv.train.sentences.frame.elements \
  regularization:reg \
  lambda:0.1 \
  numthreads:4 \
  binaryoverlapfactor:false
