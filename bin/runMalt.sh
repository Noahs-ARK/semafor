#!/bin/bash
#    A script for running MaltParser.
#    Written by Sam Thomson (sthomson@cs.cmu.edu)
#    Based on code by Dipanjan Das (dipanjan@cs.cmu.edu)
#    Copyright (C) 2013
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


MY_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"
source "${MY_DIR}/config.sh"

if [ $# -lt 2 -o $# -gt 2 ]; then
   echo "USAGE: `basename "${0}"` <input-file> <output-dir>"
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

# location of input file. must be absolute path
INPUT_FILE="${1}"

# where to write the output
OUTPUT_DIR="${2}"


POS_TAGGED="${OUTPUT_DIR}/pos.tagged"
TEST_PARSED_FILE="${OUTPUT_DIR}/conll"

bash ${MY_DIR}/tokenizeAndPosTag.sh ${INPUT_FILE} ${OUTPUT_DIR}


echo "**********************************************************************"
echo "Converting postagged input to conll."
# convert to conll so Malt can read it
time ${JAVA_HOME_BIN}/java -classpath ${CLASSPATH} \
    edu.cmu.cs.lti.ark.fn.data.prep.formats.ConvertFormat \
    --input ${POS_TAGGED} \
    --inputFormat pos \
    --output ${POS_TAGGED}.conll \
    --outputFormat conll
echo "Done converting postagged input to conll."
echo "**********************************************************************"

echo "**********************************************************************"
echo "Running MaltParser...."
pushd ${SEMAFOR_HOME}/scripts/maltparser-1.7.2
time java -Xmx2g -jar maltparser-1.7.2.jar -w ${MALT_MODEL_DIR} -c engmalt.linear-1.7 -i ${POS_TAGGED}.conll -o ${TEST_PARSED_FILE}
echo "Finished rrunning MaltParser."
echo "**********************************************************************"
echo
echo

rm "${POS_TAGGED}.conll"
