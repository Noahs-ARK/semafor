#!/bin/bash -e

# step 4ii: Caching Feature Vectors.

source "$(dirname ${0})/config.sh"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
  edu.cmu.cs.lti.ark.fn.parsing.FrameFeaturesCache \
  eventsfile:${training_dir}/scan/cv.train.events.bin \
  spansfile:${training_dir}/scan/cv.train.sentences.frame.elements.spans \
  train-framefile:${fe_file} \
  localfeaturescache:${training_dir}/scan/featurecache.jobj
