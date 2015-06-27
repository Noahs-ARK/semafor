#!/bin/bash
set -e # fail fast

# Convert conll dependency parser output into our "all.lemma.tags" format
# dependency parsed files should be located at
# ${training_dir}/cv.{dev,test,train}.sentences.maltparsed.conll
# This will clobber the previous *.all.lemma.tags files, so make sure they're
# backed up if you need them.

source "$(dirname ${0})/../../training/config.sh"
training_dir="$(dirname ${0})/../../training/data/emnlp2015"

suffix="turboparsed.basic.stanford.lemmatized"
prefixes="train test dev"
for prefix in $prefixes ; do
    tmp_parse_file="${training_dir}/semreranker.${prefix}.sentences.tmp_parse_file"
    ${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms1g -Xmx1g \
        edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE \
          "${training_dir}/semreranker.${prefix}.sentences.tokenized" \
          "${training_dir}/semreranker.${prefix}.sentences.${suffix}.conll" \
          "${tmp_parse_file}" \
          "${training_dir}/semreranker.${prefix}.sentences.${suffix}.all.lemma.tags"
    rm "${tmp_parse_file}"
done
