#!/bin/bash
#    The driver script for running SEMAFOR using MaltParser as its dependency parser.
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


MY_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"
source "${MY_DIR}/config.sh"

if [ $# -lt 2 -o $# -gt 3 ]; then
   echo "USAGE: `basename "${0}"` <input-file> <output-file> <num-threads>"
   exit 1
fi

# location of input file. must be absolute path
INPUT_FILE="${1}"

# where to write the output
OUTPUT_FILE="${2}"

NUM_THREADS="${3}"


TEMP_DIR=$(mktemp -d -t semafor.XXXXXXXXXX)
echo "TEMP_DIR: ${TEMP_DIR}"

DEPENDENCY_PARSED_FILE="${TEMP_DIR}/conll"

bash ${MY_DIR}/runMalt.sh ${INPUT_FILE} ${TEMP_DIR}


echo "**********************************"
echo "Performing frame-semantic parsing."
cd ${SEMAFOR_HOME}
time ${JAVA_HOME_BIN}/java \
    -classpath ${CLASSPATH} \
    -Xms7g -Xmx7g \
    edu.cmu.cs.lti.ark.fn.Semafor \
    input-file:${DEPENDENCY_PARSED_FILE} \
    output-file:${OUTPUT_FILE} \
    model-dir:${MALT_MODEL_DIR} \
    numthreads:${NUM_THREADS}
echo "Finished frame-semantic parsing."
echo "********************************"
echo
echo

rm -r "${TEMP_DIR}"
