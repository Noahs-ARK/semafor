#!/bin/bash
set -e # fail fast


source "$(dirname ${0})/config.sh"

# reg: $1
#************************************ PREPROCESSING *******************************************#

echo "Root of Project:"
echo ${SEMAFOR_HOME}
echo

cd ${SEMAFOR_HOME}



#infix=test
infix="dev"
processedfile="${training_dir}/cv.${infix}.sentences.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${infix}.sentences.tokenized"
fefile="${training_dir}/cv.${infix}.sentences.frame.elements"

fn_id_req_data_file="${datadir}/reqData.jobj"

dipanjan_dir="/mal2/dipanjan/experiments/FramenetParsing"
semeval_dir="${dipanjan_dir}/dipanjan_semeval"
fn_1_5_dir="${dipanjan_dir}/fndata-1.5"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"


arg_model="${datadir}/argmodel.dat"
echo ${arg_model}

#converted_id_model="${temp}/model.file"
converted_id_model="${model_dir}/idmodel.dat"

results_dir="${datadir}/results"
#results_file="${results_dir}/full_$1_${infix}_ll_beam_100_exact_verbose"
mkdir -p "{results_dir}"

temp="/cab0/sthomson/code/semafor/semafor/training/data/naacl2012/temp_arg_1352693945_01z"
echo "temp directory: $temp"


cd ${SEMAFOR_HOME}
#${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms4000m -Xmx4000m \
#    edu.cmu.cs.lti.ark.fn.identification.ConvertAlphabetFile \
#    ${alphabet_file} \
#    ${model_dir}/idmodel.dat \
#    ${converted_id_model}

#**********************************END OF PREPROCESSING********************************************#


#**********************************FRAME IDENTIFICATION********************************************#
end=`wc -l ${tokenizedfile}`
end=`expr ${end% *}`
echo "Start:0"
echo "End:${end}"

#rm -rf "${datadir}/results/full_$1_${infix}_ll_beam_100_exact_verbose"
echo "Exact Results"
cd ${semeval_dir}
#rm -rf "${temp}"
#mkdir "${temp}"
scoring/fnSemScore_modified.pl \
    -c ${temp} \
    -l \
    -n \
    -e \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${results_dir}/full_$1_${infix}_ll_beam_100_exact_verbose

#rm -rf ${}/results/full_$1_${infix}_ll_beam_100_partial_verbose
echo "Partial Results"
#rm -rf ${temp}
#mkdir ${temp}
scoring/fnSemScore_modified.pl -c ${temp} \
    -l \
    -n \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${results_dir}/full_$1_${infix}_ll_beam_100_partial_verbose

#rm -rf ${datadir}/results/fid_$1_${infix}_ll_exact_verbose
echo "Exact Results"
cd ${semeval_dir}
#rm -rf ${temp}
#mkdir ${temp}
scoring/fnSemScore_modified.pl -c ${temp} \
    -l \
    -n \
    -e \
    -t \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${results_dir}/fid_$1_${infix}_ll_exact_verbose

#rm -rf ${datadir}/results/full_$1_${infix}_ll_partial_verbose
echo "Partial Results"
#rm -rf ${temp}
#mkdir ${temp}
scoring/fnSemScore_modified.pl -c ${temp} \
    -l \
    -n \
    -t \
    -v \
    ${frames_single_file} \
    ${relation_modified_file} \
    ${temp}/file.gold.xml \
    ${temp}/file.predict.xml > ${results_dir}/full_$1_${infix}_ll_partial_verbose

echo ${temp}
#rm -rf ${temp}
