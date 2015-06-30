#!/bin/bash
set -e # fail fast
set -x

cv=$1  # "test" or "dev"

source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"

cd ${SEMAFOR_HOME}

dep_parse_file="${training_dir}/cv.${cv}.sentences.${dep_parser}parsed.conll"
all_lemma_tags_file="${training_dir}/cv.${cv}.sentences.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${cv}.sentences.tokenized"
gold_fe_file="${training_dir}/cv.${cv}.sentences.frame.elements"

fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"

output_dir="${experiments_dir}/output"
mkdir -p "${output_dir}"
predicted_xml="${output_dir}/${cv}.argid.predict.xml"
gold_xml="${output_dir}/${cv}.gold.xml"

results_dir="${experiments_dir}/results"
mkdir -p "${results_dir}"
results_file="${results_dir}/argid_${cv}_exact"


# make a gold xml file whose tokenization matches the tokenization used for parsing
# (hack around the fact that SEMAFOR mangles token offsets)
end=`wc -l ${tokenizedfile}`
end=`expr ${end% *}`
echo "Start:0"
echo "End:${end}"
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms1g -Xmx1g \
    edu.cmu.cs.lti.ark.fn.evaluation.FrameElementsToXmlApp \
    testFEPredictionsFile:${gold_fe_file} \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:${all_lemma_tags_file} \
    testTokenizedFile:${tokenizedfile} \
    outputFile:${gold_xml}  # 2>/dev/null


echo "Performing argument identification on ${cv} set, with model \"${model_name}\"..."
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4g -Xmx4g -XX:ParallelGCThreads=2 \
    edu.cmu.cs.lti.ark.fn.evaluation.ParseToXmlWithGoldFramesApp \
    ${SEMAFOR_HOME} \
    ${model_name} \
    ${cv} \
    "${dep_parse_file}"



echo "Evaluating argument identification on ${cv} set..."
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl \
    -l \
    -n \
    -e \
    -a \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_dir}/argid_${cv}_exact" 2>/dev/null

tail -n1 "${results_dir}/argid_${cv}_exact"

# Dipanjan reported using this evaluation
echo "Evaluating argument identification on ${cv} set (counting gold frames)..."
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl \
    -l \
    -n \
    -e \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${gold_xml} \
    ${predicted_xml} > "${results_dir}/argid_${cv}_exact_count_gold_frames" 2>/dev/null


tail -n1 "${results_dir}/argid_${cv}_exact_count_gold_frames"
