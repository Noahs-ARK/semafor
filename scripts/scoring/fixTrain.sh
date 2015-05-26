#!/bin/bash
set -e # fail fast
set -x

cv="dev"  # "test" or "dev"

source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"

cd ${SEMAFOR_HOME}

all_lemma_tags_file="${training_dir}/cv.${cv}.sentences.turboparsed.standard.stanford.concise.all.lemma.tags"
tokenizedfile="${training_dir}/cv.${cv}.sentences.concise.tokenized"


fn_1_5_dir="${datadir}/framenet15/"
frames_single_file="${fn_1_5_dir}/framesSingleFile.xml"
relation_modified_file="${fn_1_5_dir}/frRelationModified.xml"

output_dir="/usr0/home/sswayamd/semafor/semafor/fix/"
#mkdir -p "${output_dir}"
#gold_xml="${training_dir}/cv.${cv}.concise.gold.xml"
gold_fe_file=$1 #"${training_dir}/cv.${cv}.sentences.concise.frame.elements"
gold_xml="${output_dir}/dedup.0th.argid.predict.xml"


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
    outputFile:${gold_xml}  # 2>/dev/null


