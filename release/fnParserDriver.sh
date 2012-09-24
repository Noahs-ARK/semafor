#!/bin/bash
#    The driver script for SEMAFOR.
#    Written by Dipanjan Das (dipanjan@cs.cmu.edu)
#    with suggestions from Thomas Kleinbauer (thomas.kleinbauer@dfki.de)
#    Copyright (C) 2011
#    Dipanjan Das
#    Language Technologies Institute, Carnegie Mellon University
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

source "$(dirname `readlink -f ${0}`)/config"

if [ $# -lt 1 -o $# -gt 2 ]; then
   echo "USAGE: `basename "${0}"` <input-file> [<output-file>]"
   exit 1
fi

if [ `uname -m` != "x86_64" ]; then
   echo -n "\nNOTE: You should really be running this on a 64-bit architecture."
   # give the user the chance to CTRL-C here...
   for dot in 1 2 3 4 5 6; do
       sleep 1
       echo -n "."
   done
   echo
fi

# $3: location of line split file, must be absolute path
INPUT_FILE=`readlink -f "${1}"`

# output of FN parser
if [ $# = 2 ]; then
   OUTPUT_FILE=`readlink -f "${2}"`
else
   OUTPUT_FILE="${INPUT_FILE}.out"
fi


CLEAN_INPUT=${TEMP_DIR}/$$.input
grep -v '^\s*$' ${INPUT_FILE} > ${CLEAN_INPUT}
INPUT_FILE=${CLEAN_INPUT}

# The MST dependency parser assumes (hard-wired) that there is a temp
# directory "tmp" under its home directory, so we want to make sure
# that this directory exists.
if [ ! -d "${MST_PARSER_HOME}/tmp" ]; then
   mkdir "${MST_PARSER_HOME}/tmp"
   REMOVE_DOT_TMP=1
else
   REMOVE_DOT_TMP=0
fi

CLASSPATH=".:${SEMAFOR_HOME}/lib/semafor-deps.jar"

rm -f ${INPUT_FILE}.tokenized
if [ "${GOLD_TARGET_FILE}" == "null" ]
then
    echo "**********************************************************************"
    echo "Tokenizing file: ${INPUT_FILE}"
    sed -f ${SEMAFOR_HOME}/scripts/tokenizer.sed ${INPUT_FILE} > ${INPUT_FILE}.tokenized
    echo "Finished tokenization."
    echo "**********************************************************************"
    echo
    echo
else
    echo "**********************************************************************"
    echo "Gold target file provided, not tokenizing input file."
    cat ${INPUT_FILE} > ${INPUT_FILE}.tokenized
    echo "**********************************************************************"
    echo 
    echo
fi

echo "**********************************************************************"
echo "Part-of-speech tagging tokenized data...."
rm -f ${INPUT_FILE}.pos.tagged
cd ${SEMAFOR_HOME}/scripts/jmx
./mxpost tagger.project < ${INPUT_FILE}.tokenized > ${INPUT_FILE}.pos.tagged
echo "Finished part-of-speech tagging."
echo "**********************************************************************"
echo
echo

rm -f ${INPUT_FILE}.conll.input
rm -f ${INPUT_FILE}.conll.output
if [ "$MST_MODE" != "server" ]
then
    echo "**********************************************************************"
    echo "Preparing the input for MST Parser..."
    cd ${SEMAFOR_HOME}
    ${JAVA_HOME_BIN}/java \
	-classpath ${CLASSPATH} \
	edu.cmu.cs.lti.ark.fn.data.prep.CoNLLInputPreparation \
	${INPUT_FILE}.pos.tagged ${INPUT_FILE}.conll.input

    echo "Dependency parsing the data..."
    cd ${MST_PARSER_HOME}
    ${JAVA_HOME_BIN}/java -classpath ".:./lib/trove.jar:./lib/mallet-deps.jar:./lib/mallet.jar" \
	-Xms8g -Xmx8g mst.DependencyParser \
	test separate-lab \
	model-name:${MODEL_DIR}/wsj.model \
	decode-type:proj order:2 \
	test-file:${INPUT_FILE}.conll.input \
	output-file:${INPUT_FILE}.conll.output \
	format:CONLL
    echo "Finished dependency parsing."
    echo "**********************************************************************"
    echo
    echo
fi

if [ "${AUTO_TARGET_ID_MODE}" == "relaxed" ]
then 
    RELAXED_FLAG=yes
else
    RELAXED_FLAG=no
fi


if [ "${USE_GRAPH_FILE}" == "yes" ]
then
    GRAPH_FILE=${MODEL_DIR}/sparsegraph.gz
else
    GRAPH_FILE=null
fi

ALL_LEMMA_TAGS_FILE=${INPUT_FILE}.all.lemma.tags


echo "**********************************************************************"
echo "Performing frame-semantic parsing"
cd ${SEMAFOR_HOME}
${JAVA_HOME_BIN}/java \
    -classpath ${CLASSPATH} \
    -Xms4g -Xmx4g \
    edu.cmu.cs.lti.ark.fn.parsing.ParserDriver \
    mstmode:${MST_MODE} \
    mstserver:${MST_MACHINE} \
    mstport:${MST_PORT} \
    posfile:${INPUT_FILE}.pos.tagged \
    test-parsefile:${INPUT_FILE}.conll.output \
    stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
    wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
    fnidreqdatafile:${MODEL_DIR}/reqData.jobj \
    goldsegfile:${GOLD_TARGET_FILE} \
    userelaxed:${RELAXED_FLAG} \
    testtokenizedfile:${INPUT_FILE}.tokenized \
    idmodelfile:${MODEL_DIR}/idmodel.dat \
    alphabetfile:${MODEL_DIR}/parser.conf \
    framenet-femapfile:${MODEL_DIR}/framenet.frame.element.map \
    eventsfile:${INPUT_FILE}.events.bin \
    spansfile:${INPUT_FILE}.spans \
    model:${MODEL_DIR}/argmodel.dat \
    useGraph:${GRAPH_FILE} \
    frameelementsoutputfile:${INPUT_FILE}.fes \
    alllemmatagsfile:${ALL_LEMMA_TAGS_FILE} \
    requiresmap:${MODEL_DIR}/requires.map \
    excludesmap:${MODEL_DIR}/excludes.map \
    decoding:${DECODING_TYPE}

end=`wc -l ${INPUT_FILE}.tokenized`
end=`expr ${end% *}`
echo "Producing final XML document:"
${JAVA_HOME_BIN}/java -classpath ${CLASSPATH} \
    -Xms4g -Xmx4g \
    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
    testFEPredictionsFile:${INPUT_FILE}.fes \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:${ALL_LEMMA_TAGS_FILE} \
    testTokenizedFile:${INPUT_FILE}.tokenized \
    outputFile:${OUTPUT_FILE}


echo "Finished frame-semantic parsing."
echo "**********************************************************************"
echo
echo

# clean up
if [ ${REMOVE_DOT_TMP} = 1 ]; then
   /bin/rm -rf "${MST_PARSER_HOME}/tmp"
fi