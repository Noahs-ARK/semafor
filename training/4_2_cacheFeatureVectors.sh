#!/bin/bash -e

# step 4ii: Caching Feature Vectors.

source "$(dirname ${0})/config"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
  edu.cmu.cs.lti.ark.fn.parsing.FrameFeaturesCache \
  eventsfile:${training_dir}/scan/cv.train.events.bin \
  spansfile:${training_dir}/scan/cv.train.sentences.frame.elements.spans \
  train-framefile:${training_dir}/cv.train.sentences.frame.elements \
  localfeaturescache:${training_dir}/scan/featurecache.jobj
