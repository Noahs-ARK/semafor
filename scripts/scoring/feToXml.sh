#!/bin/bash
set -e # fail fast
set -x

rank=$1
cv="test"  # "test" or "dev"


source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"

cd ${SEMAFOR_HOME}/


all_lemma_tags_file="${training_dir}/cv.${cv}.sentences.turboparsed.basic.stanford.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${cv}.sentences.tokenized"


fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"

inp_dir="${SEMAFOR_HOME}/experiments/basic_tbps/output/tbps_basic_1.30438665209_test/frameElements1000/"
inp_fe_file="${inp_dir}/${rank}thBest.argid.predict.frame.elements"

output_dir="${SEMAFOR_HOME}/experiments/basic_tbps/output/tbps_basic_1.30438665209_test/xml1000/"
outp_xml="${output_dir}/${rank}thBest.argid.predict.xml"


# make a gold xml file whose tokenization matches the tokenization used for parsing
# (hack around the fact that SEMAFOR mangles token offsets)
end=`wc -l ${tokenizedfile}`
end=`expr ${end% *}`
echo "Start:0"
echo "End:${end}"
${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms1g -Xmx1g \
    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
    testFEPredictionsFile:${inp_fe_file} \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:${all_lemma_tags_file} \
    testTokenizedFile:${tokenizedfile} \
    outputFile:${outp_xml}  # 2>/dev/null


