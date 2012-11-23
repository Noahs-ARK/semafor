#!/bin/bash

set -e # fail fast

# Convert maltparser output into our "all.lemma.tags" format
# maltparsed files should be located at
# ${training_dir}/cv.{dev,test,train}.sentences.maltparsed.conll
# This will clobber the previous *.all.lemma.tags files, so make sure they're
# backed up if you need them.

source "$(dirname ${0})/config.sh"

prefixes="dev test train"
for prefix in $prefixes
do
    tmp_parse_file="${training_dir}/cv.${prefix}.sentences.tmp_parse_file"
    ${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms2g -Xmx2g \
        edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE \
          ${training_dir}/cv.${prefix}.sentences.tokenized \
          ${training_dir}/cv.${prefix}.sentences.maltparsed.conll \
          ${tmp_parse_file} \
          ${stopwords_file} \
          ${wordnet_config_file} \
          ${training_dir}/cv.${prefix}.sentences.all.lemma.tags
    rm ${tmp_parse_file}
done
