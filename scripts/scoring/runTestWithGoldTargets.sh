#!/bin/bash
set -x # echo commands
set -e # fail fast

source "$(dirname "${BASH_SOURCE[0]}")/../../training/config.sh"

cv="dev" # "test" # "${1}"

echo "results directory: ${results_dir}"
mkdir -p "${results_dir}"



#***************** Run SEMAFOR with Gold Targets, Auto Frame-id, Auto Arg-id ***********************#

scala -classpath ${CLASSPATH} -J-Xmx3g "${SEMAFOR_HOME}/scripts/scoring/runWithGoldTargets.scala" \
    "${model_dir}" \
    "${training_dir}/cv.${cv}.sentences.frames" \
    "${training_dir}/cv.${cv}.sentences.maltparsed.conll" \
    "${results_dir}/${cv}.autoframe.predicted.xml"

scala -classpath ${CLASSPATH} -J-Xmx3g "${SEMAFOR_HOME}/scripts/scoring/runWithGoldTargets.scala" \
    "${model_dir}" \
    "${training_dir}/cv.${cv}.sentences.frames" \
    "${training_dir}/cv.${cv}.sentences.maltparsed.conll" \
    "${results_dir}/${cv}.goldframe.predicted.xml" \
    true # use gold frames


#***************** Create a gold XML file with the same tokenization that SEMAFOR used ***********************#

processedfile="${training_dir}/cv.${cv}.sentences.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${cv}.sentences.tokenized"
fefile="${training_dir}/cv.${cv}.sentences.frame.elements"

end=`wc -l "${tokenizedfile}"`
end=`expr ${end% *}`


${JAVA_HOME_BIN}/java -classpath ${CLASSPATH} -Xmx1g \
    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
    testFEPredictionsFile:"${fefile}" \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:"${processedfile}" \
    testTokenizedFile:"${tokenizedfile}" \
    outputFile:"${results_dir}/${cv}.gold.xml"


#********************************** Evaluation ********************************************#

fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"


echo "Exact Frame Id Results"
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl -c "${results_dir}" \
    -l \
    -n \
    -e \
    -t \
    -v \
   "${frames_single_file}" \
    "${relation_modified_file}" \
    "${results_dir}/${cv}.gold.xml" \
    "${results_dir}/${cv}.autoframe.predicted.xml" > "${results_dir}/frameid_${cv}_exact_verbose"

echo "Partial Frame Id Results"
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl -c "${results_dir}" \
    -l \
    -n \
    -t \
    -v \
    "${frames_single_file}" \
    "${relation_modified_file}" \
    "${results_dir}/${cv}.gold.xml" \
    "${results_dir}/${cv}.autoframe.predicted.xml" > "${results_dir}/frameid_${cv}_partial_verbose"


echo "Exact Arg Results"
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl \
    -c "${results_dir}" \
    -l \
    -n \
    -e \
    -v \
    "${frames_single_file}" \
    "${relation_modified_file}" \
    "${results_dir}/${cv}.gold.xml" \
    "${results_dir}/${cv}.goldframe.predicted.xml" > "${results_dir}/arg_${cv}_exact_verbose"

echo "Partial Arg Results"
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl -c "${results_dir}" \
    -l \
    -n \
    -v \
    "${frames_single_file}" \
    "${relation_modified_file}" \
    "${results_dir}/${cv}.gold.xml" \
    "${results_dir}/${cv}.goldframe.predicted.xml" > "${results_dir}/arg_${cv}_partial_verbose"


echo "Exact Full Results"
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl \
    -c "${results_dir}" \
    -l \
    -n \
    -e \
    -v \
    "${frames_single_file}" \
    "${relation_modified_file}" \
    "${results_dir}/${cv}.gold.xml" \
    "${results_dir}/${cv}.autoframe.predicted.xml" > "${results_dir}/full_${cv}_exact_verbose"

echo "Partial Full Results"
${SEMAFOR_HOME}/scripts/scoring/fnSemScore_modified.pl -c "${results_dir}" \
    -l \
    -n \
    -v \
    "${frames_single_file}" \
    "${relation_modified_file}" \
    "${results_dir}/${cv}.gold.xml" \
    "${results_dir}/${cv}.autoframe.predicted.xml" > "${results_dir}/full_${cv}_partial_verbose"


#********************************** End of Evaluation ********************************************#
