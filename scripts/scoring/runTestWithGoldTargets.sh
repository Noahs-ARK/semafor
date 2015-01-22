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

predicted_xml="${experiments_dir}/output/${cv}.full.predict.xml"
gold_xml="${experiments_dir}/output/${cv}.gold.xml"

scoring_script="${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl"

temp="${experiments_dir}/framecache"
mkdir -p "${temp}"

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


echo "Performing full parsing (but w/ gold targets) on ${cv} set, with model \"${model_name}\"..."
scala \
  -cp "${classpath}" \
  -J-Xms4g \
  -J-Xmx4g \
  -J-XX:ParallelGCThreads=2 \
  scripts/scoring/parseToXmlWithGoldTargets.scala \
  ${model_name} \
  ${cv}


echo "Evaluating frame id on ${cv} set, with model \"${model_name}\"..."
echo "Exact Results:"
${scoring_script} \
    -c ${temp} \
    -l \
    -n \
    -e \
    -t \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_dir}/frame_id_${cv}_exact" 2>/dev/null

tail -n1 "${results_dir}/frame_id_${cv}_exact"

echo "Partial Results"
${scoring_script} \
    -c ${temp} \
    -l \
    -n \
    -t \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_dir}/frame_id_${cv}_partial" 2>/dev/null

tail -n1 "${results_dir}/frame_id_${cv}_partial"

echo
echo "Evaluating full system (w/ gold targets) on ${cv} set, with model \"${model_name}\"..."
echo "Exact Results"
${scoring_script} \
    -c ${temp} \
    -l \
    -n \
    -e \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_dir}/full_${cv}_exact" 2>/dev/null

tail -n1 "${results_dir}/full_${cv}_exact"

echo "Partial Results"
${scoring_script} \
    -c ${temp} \
    -l \
    -n \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_dir}/full_${cv}_partial" 2>/dev/null

tail -n1 "${results_dir}/full_${cv}_partial"
