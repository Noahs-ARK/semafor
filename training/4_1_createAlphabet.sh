#!/bin/bash -e

# step 4i: create the alphabet file for the argument identification model.

source "$(dirname ${0})/config"

mkdir -p ${training_dir}/scan


${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
   edu.cmu.cs.lti.ark.fn.parsing.CreateAlphabet \
   ${training_dir}/cv.train.sentences.frame.elements \
   ${training_dir}/cv.train.sentences.all.lemma.tags \
   ${training_dir}/scan/cv.train.events.bin \
   ${training_dir}/scan/parser.conf.unlabeled \
   ${training_dir}/scan/cv.train.sentences.frame.elements.spans \
   true \
   false \
   1 \
   null \
   ${datadir}/framenet.frame.element.map
