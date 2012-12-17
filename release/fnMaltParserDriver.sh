#!/bin/bash
#    The driver script for running SEMAFOR on sentences that have already
#    been parsed with MaltParser.
#    Written by Sam Thomson (sthomson@cs.cmu.edu)
#    Based on code by Dipanjan Das (dipanjan@cs.cmu.edu)
#    Copyright (C) 2011
#    Sam Thomson
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

set -e # fail fast

source "$(dirname ${0})/config"

if [ $# -lt 2 -o $# -gt 2 ]; then
   echo "USAGE: `basename "${0}"` <input-file> <output-file>"
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

# location of gzipped tarred malt-parsed input file. must be absolute path
INPUT_FILE="${1}"

# where to write the output
OUTPUT_FILE="${2}"

TEMP_DIR=$(mktemp -d -t semafor)
echo "TEMP_DIR: ${TEMP_DIR}"

TOKENIZED="${TEMP_DIR}/tokenized"
echo "**********************************************************************"
echo "Tokenizing file: ${INPUT_FILE}"
time gzip -cd ${INPUT_FILE} | ${SEMAFOR_HOME}/scripts/tokenize_malt.py > ${TOKENIZED}
echo "Finished tokenization."
echo "**********************************************************************"
echo
echo

POS_TAGGED="${TEMP_DIR}/pos.tagged"
echo "**********************************************************************"
echo "Part-of-speech tagging tokenized data...."
time gzip -cd ${INPUT_FILE} | ${SEMAFOR_HOME}/scripts/pos_tag_malt.py > ${POS_TAGGED}
echo "Finished part-of-speech tagging."
echo "**********************************************************************"
echo
echo

TEST_PARSED_FILE="${TEMP_DIR}/conll"
echo "**********************************************************************"
echo "Converting malt parse to conll format...."
time gzip -cd ${INPUT_FILE} | ${SEMAFOR_HOME}/scripts/malt_to_conll.py > ${TEST_PARSED_FILE}
echo "Finished converting malt parse to conll format."
echo "**********************************************************************"
echo
echo


if [ "${AUTO_TARGET_ID_MODE}" == "relaxed" ]
then 
    RELAXED_FLAG=yes
else
    RELAXED_FLAG=no
fi


if [ "${USE_GRAPH_FILE}" == "yes" ]
then
    GRAPH_FILE="${MODEL_DIR}/sparsegraph.gz"
else
    GRAPH_FILE=null
fi

ALL_LEMMA_TAGS_FILE="${TEMP_DIR}/all.lemma.tags"
FRAME_ELEMENTS_OUTPUT_FILE="${TEMP_DIR}/fes"

echo "**********************************************************************"
echo "Performing frame-semantic parsing"
cd ${SEMAFOR_HOME}
#CLASSPATH=".:${SEMAFOR_HOME}/lib/semafor-deps.jar"
CLASSPATH=".:${SEMAFOR_HOME}/target/Semafor-3.0-SNAPSHOT.jar"
echo CLASSPATH="$CLASSPATH"
time ${JAVA_HOME_BIN}/java \
    -classpath ${CLASSPATH} \
    -Xms4g -Xmx4g \
    edu.cmu.cs.lti.ark.fn.parsing.ParserDriver \
    mstmode:noserver \
    mstserver:null \
    mstport:12345 \
    posfile:${POS_TAGGED} \
    test-parsefile:${TEST_PARSED_FILE} \
    stopwords-file:${SEMAFOR_HOME}/stopwords.txt \
    wordnet-configfile:${SEMAFOR_HOME}/file_properties.xml \
    fnidreqdatafile:${MODEL_DIR}/reqData.jobj \
    goldsegfile:${GOLD_TARGET_FILE} \
    userelaxed:${RELAXED_FLAG} \
    testtokenizedfile:${TOKENIZED} \
    idmodelfile:${MODEL_DIR}/idmodel.dat \
    alphabetfile:${MODEL_DIR}/parser.conf \
    framenet-femapfile:${MODEL_DIR}/framenet.frame.element.map \
    eventsfile:${TEMP_DIR}/events.bin \
    spansfile:${TEMP_DIR}/spans \
    model:${MODEL_DIR}/argmodel.dat \
    useGraph:${GRAPH_FILE} \
    frameelementsoutputfile:${FRAME_ELEMENTS_OUTPUT_FILE} \
    alllemmatagsfile:${ALL_LEMMA_TAGS_FILE} \
    requiresmap:${MODEL_DIR}/requires.map \
    excludesmap:${MODEL_DIR}/excludes.map \
    decoding:${DECODING_TYPE}

end=`wc -l ${TOKENIZED}`
end=`expr ${end% *}`
echo "${end} sentences"

: '
echo "Producing final XML document:"
time ${JAVA_HOME_BIN}/java -classpath ${CLASSPATH} \
    -Xms4g -Xmx4g \
    edu.cmu.cs.lti.ark.fn.evaluation.PrepareFullAnnotationXML \
    testFEPredictionsFile:${FRAME_ELEMENTS_OUTPUT_FILE} \
    startIndex:0 \
    endIndex:${end} \
    testParseFile:${ALL_LEMMA_TAGS_FILE} \
    testTokenizedFile:${TOKENIZED} \
    outputFile:${OUTPUT_FILE}
'

echo "Finished frame-semantic parsing."
echo "**********************************************************************"
echo
echo

#rm -r "${TEMP_DIR}"
