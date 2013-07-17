#!/bin/bash
set -e # fail fast


source "$(dirname ${0})/../training/config.sh"

#************************************ PREPROCESSING *******************************************#

echo "Root of Project:"
echo ${SEMAFOR_HOME}
echo

cd ${SEMAFOR_HOME}

#infix="test"
infix="dev"
processedfile="${training_dir}/cv.${infix}.sentences.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${infix}.sentences.tokenized"
fefile="${training_dir}/cv.${infix}.sentences.frame.elements"

fn_id_req_data_file="${MODEL_DIR}/reqData.jobj"

fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"


arg_model="${MODEL_DIR}/argmodel.dat"
echo ${arg_model}

id_model="${MODEL_DIR}/idmodel.dat"

parser_conf=${SCAN_DIR}/parser.conf.unlabeled

temp="$(mktemp -d --tmpdir=${training_dir} temp_arg_`date +%s`_XXX)"
echo "temp directory: $temp"


cd ${SEMAFOR_HOME}

#**********************************END OF PREPROCESSING********************************************#


#**********************************FRAME IDENTIFICATION********************************************#
end=`wc -l ${tokenizedfile}`
end=`expr ${end% *}`
echo "Start:0"
echo "End:${end}"

cd ${SEMAFOR_HOME}
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
    edu.cmu.cs.lti.ark.fn.identification.FrameIdentificationGoldTargets \
    startindex:0 \
    endindex:${end} \
    test-parsefile:${processedfile} \
    testtokenizedfile:${tokenizedfile} \
    fnidreqdatafile:${fn_id_req_data_file} \
    stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
    wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
    idmodelfile:${id_model} \
    frameelementsoutputfile:${temp}/output.frame.elements \
    test-framefile:${fefile} \
    clusterfeats:false \
    useGraph:null

#**********************************ARGUMENT IDENTIFICATION********************************************#
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
    edu.cmu.cs.lti.ark.fn.parsing.CreateAlphabet \
    ${temp}/output.frame.elements \
    ${processedfile} \
    ${temp}/file.fe.events.bin \
    ${parser_conf} \
    ${temp}/file.frame.elements.spans \
    false \
    1 \
    null \
    false \
    ${datadir}/validStartEndPOS


${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
    edu.cmu.cs.lti.ark.fn.parsing.DecodingMainArgs \
    ${arg_model} \
    ${parser_conf} \
    ${temp}/file.fe.events.bin \
    ${temp}/file.frame.elements.spans \
    ${temp}/file.predict.frame.elements \
    ${temp}/output.frame.elements \
    overlapcheck
#**********************************END OF ARGUMENT IDENTIFICATION********************************************#

rm -rf "$temp/file.gold.frame.elements"
cat ${fefile} | awk '{print "0""\t"$0}' > "${temp}/file.gold.frame.elements"

infixes="predict gold"
for infix in ${infixes}
do
    ${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms1000m -Xmx1000m \
        edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
        testFEPredictionsFile:${temp}/file.${infix}.frame.elements \
        startIndex:0 \
        endIndex:${end} \
        testParseFile:${processedfile} \
        testTokenizedFile:${tokenizedfile} \
        outputFile:${temp}/file.${infix}.xml
done


#********************************** Evaluation ********************************************#

rm -rf "${datadir}/results/full_$1_${infix}_ll_beam_100_exact_verbose"
echo "Exact Results"
cd ${SEMAFOR_HOME}
rm -rf "${temp}"
mkdir "${temp}"
scoring/fnSemScore_modified.pl \
    -c ${temp} \
    -l \
    -n \
    -e \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${datadir}/results/full_$1_${infix}_ll_beam_100_exact_verbose

rm -rf ${datadir}/results/full_$1_${infix}_ll_beam_100_partial_verbose
echo "Partial Results"
rm -rf ${temp}
mkdir ${temp}
scoring/fnSemScore_modified.pl -c ${temp} \
    -l \
    -n \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${datadir}/results/full_$1_${infix}_ll_beam_100_partial_verbose

rm -rf ${datadir}/results/fid_$1_${infix}_ll_exact_verbose
echo "Exact Results"
cd ${SEMAFOR_HOME}
rm -rf ${temp}
mkdir ${temp}
scoring/fnSemScore_modified.pl -c ${temp} \
    -l \
    -n \
    -e \
    -t \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${datadir}/results/fid_$1_${infix}_ll_exact_verbose

rm -rf ${datadir}/results/full_$1_${infix}_ll_partial_verbose
echo "Partial Results"
rm -rf ${temp}
mkdir ${temp}
scoring/fnSemScore_modified.pl -c ${temp} \
    -l \
    -n \
    -t \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${datadir}/results/full_$1_${infix}_ll_partial_verbose

#********************************** End of Evaluation ********************************************#

echo ${temp}
rm -rf ${temp}
