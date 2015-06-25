#!/bin/bash
set -e # fail fast
set -x

source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"

NAME=$1
PREFIX="test" # "test" or "dev"


#************************************ PREPROCESSING *******************************************#

echo "Root of Project:"
echo ${SEMAFOR_HOME}
echo
echo "Using semafor model:"
echo ${model_name}
echo
cd ${SEMAFOR_HOME}


fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"


GOLD_FILE="${datadir}/naacl2012/cv.${PREFIX}.sentences.lrb.xml"



#temp="$(mktemp -d --tmpdir=${training_dir} temp_arg_`date +%s`_XXX)"
temp="${experiments_dir}/tmp"
mkdir -p "${temp}"
echo "temp directory: $temp"

INPUT_DIR="${experiments_dir}/output/${NAME}/xml1000/"



###########################


all_lemma_tags_file="${training_dir}/cv.${PREFIX}.sentences.turboparsed.basic.stanford.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${PREFIX}.sentences.tokenized"
gold_fe_file="${training_dir}/cv.${PREFIX}.sentences.frame.elements"


output_dir="${experiments_dir}/output"
gold_xml="${output_dir}/${PREFIX}.gold.xml"

results_dir="${experiments_dir}/results"
results_file="${results_dir}/argid_${PREFIX}_exact"


# make a gold xml file whose tokenization matches the tokenization used for parsing
# (hack around the fact that SEMAFOR mangles token offsets)
#end=`wc -l ${tokenizedfile}`
#end=`expr ${end% *}`
#echo "Start:0"
#echo "End:${end}"
#${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms1g -Xmx1g \
#    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
#    testFEPredictionsFile:${gold_fe_file} \
#    startIndex:0 \
#    endIndex:${end} \
#    testParseFile:${all_lemma_tags_file} \
#    testTokenizedFile:${tokenizedfile} \
#    outputFile:${gold_xml}
#
#
#mkdir -p "${output_dir}/${NAME}/frameElements"
#mkdir -p "${output_dir}/${NAME}/xml"
#
#echo "Performing argument identification on ${PREFIX} set, with model \"${model_name}\"..."
#${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4g -Xmx4g -XX:ParallelGCThreads=2 \
#    edu.cmu.cs.lti.ark.fn.evaluation.SwabhaDiversityApp \
#    "${SEMAFOR_HOME}" \
#    "${NAME}" \
#    "${tokenizedfile}" \
#    "${gold_fe_file}" \
#    "${all_lemma_tags_file}" \
#    "${experiments_dir}" \
#    "${SEMAFOR_HOME}/experiments/swabha/diversekbestdeps" \
#    "${output_dir}"


DIVERSITY_RESULTS_DIR="${experiments_dir}/results/${NAME}"
#mkdir -p "${DIVERSITY_RESULTS_DIR}"

INPUT_FILES=$(cd "${INPUT_DIR}" > /dev/null && echo *)
#INPUT_FILE="${INPUT_DIR}/1thBest.xml"
#OUTPUT_FILE="${DIVERSITY_RESULTS_DIR}/exact/1thBest"

for INPUT_FILE in ${INPUT_FILES}; do
    cd "${SEMAFOR_HOME}"
#    echo "Argument Labeling Exact Results: ${INPUT_DIR}/${INPUT_FILE}"
#    mkdir -p "${DIVERSITY_RESULTS_DIR}/exact"
#    ./scripts/scoring/fnSemScore_swabha.pl \
#        -c ${temp} \
#        -l \
#        -n \
#        -e \
#        -s \
#        "${frames_single_file}" \
#        "${relation_modified_file}" \
#        "${GOLD_FILE}" \
#        "${INPUT_DIR}/${INPUT_FILE}" > "${DIVERSITY_RESULTS_DIR}/exact/${INPUT_FILE}"

    echo "Argument Labeling Partial Credit Results: ${INPUT_DIR}/${INPUT_FILE}"
    mkdir -p "${DIVERSITY_RESULTS_DIR}/partial1000/"
    ./scripts/scoring/fnSemScore_swabha.pl \
        -c ${temp} \
        -l \
        -n \
        -s \
        "${frames_single_file}" \
        "${relation_modified_file}" \
        "${GOLD_FILE}" \
        "${INPUT_DIR}/${INPUT_FILE}" > "${DIVERSITY_RESULTS_DIR}/partial1000/${INPUT_FILE}"
done
