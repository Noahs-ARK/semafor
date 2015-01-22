#!/bin/bash
set -e # fail fast

cv=$1  # "test" or "dev"

source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"

cd ${SEMAFOR_HOME}

all_lemma_tags_file="${training_dir}/cv.${cv}.sentences.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${cv}.sentences.tokenized"
gold_fe_file="${training_dir}/cv.${cv}.sentences.frame.elements"


fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"

results_dir="${experiments_dir}/results"
mkdir -p "${results_dir}"
results_file="${results_dir}/argid_${cv}_exact"

predicted_xml="${experiments_dir}/output/${cv}.argid.predict.xml"
gold_xml="${experiments_dir}/output/${cv}.gold.xml"



# make a gold xml file whose tokenization matches the tokenization used for parsing
# (hack around the fact that SEMAFOR mangles token offsets)
end=`wc -l ${tokenizedfile}`
end=`expr ${end% *}`
echo "Start:0"
echo "End:${end}"
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms1g -Xmx1g \
    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
    testFEPredictionsFile:${gold_fe_file} \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:${all_lemma_tags_file} \
    testTokenizedFile:${tokenizedfile} \
    outputFile:${gold_xml}


echo "Performing argument identification on ${cv} set, with model \"${model_name}\"..."
scala \
  -cp "${classpath}" \
  -J-Xms4g \
  -J-Xmx4g \
  -J-XX:ParallelGCThreads=2 \
  scripts/scoring/parseToXmlWithGoldFrames.scala \
  ${model_name} \
  ${cv}


echo "Evaluating argument identification on ${cv} set..."
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl \
    -l \
    -n \
    -e \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_file}"

tail -n1 "${results_file}"
