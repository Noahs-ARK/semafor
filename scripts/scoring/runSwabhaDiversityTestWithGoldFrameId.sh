#!/bin/bash
set -e # fail fast

source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"


NAME="$1"

#************************************ PREPROCESSING *******************************************#

echo "Root of Project:"
echo ${SEMAFOR_HOME}
echo

fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"


GOLD_FILE="${training_dir}/cv.test.sentences.lrb.xml"


EXPERIMENT_DIR="${experiments_dir}"

#temp="$(mktemp -d --tmpdir=${training_dir} temp_arg_`date +%s`_XXX)"
temp="${EXPERIMENT_DIR}/tmp"
#mkdir "${temp}"
#echo "temp directory: $temp"

INPUT_DIR="${EXPERIMENT_DIR}/output/${NAME}/xml"
RESULTS_DIR="${EXPERIMENT_DIR}/results/${NAME}"
mkdir -p "${RESULTS_DIR}"

INPUT_FILES=$(cd "${INPUT_DIR}" > /dev/null && echo *)
#INPUT_FILE="${INPUT_DIR}/1thBest.xml"
#OUTPUT_FILE="${RESULTS_DIR}/exact/1thBest"

for INPUT_FILE in ${INPUT_FILES}; do
    cd "${SEMAFOR_HOME}"
    echo "Argument Labeling Exact Results: ${INPUT_DIR}/${INPUT_FILE}"
    mkdir -p "${RESULTS_DIR}/exact"
    ./scripts/scoring/fnSemScore_swabha.pl \
        -c ${temp} \
        -l \
        -n \
        -e \
        -s \
        "${frames_single_file}" \
        "${relation_modified_file}" \
        "${GOLD_FILE}" \
        "${INPUT_DIR}/${INPUT_FILE}" > "${RESULTS_DIR}/exact/${INPUT_FILE}"

    echo "Argument Labeling Partial Credit Results: ${INPUT_DIR}/${INPUT_FILE}"
    mkdir -p "${RESULTS_DIR}/partial"
    ./scripts/scoring/fnSemScore_swabha.pl \
        -c ${temp} \
        -l \
        -n \
        -s \
        "${frames_single_file}" \
        "${relation_modified_file}" \
        "${GOLD_FILE}" \
        "${INPUT_DIR}/${INPUT_FILE}" > "${RESULTS_DIR}/partial/${INPUT_FILE}"
done
