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


TOKENIZED="${OUTPUT_DIR}/tokenized"
POS_TAGGED="${OUTPUT_DIR}/pos.tagged"


echo "**********************************************************************"
echo "Tokenizing file: ${INPUT_FILE}"
time sed -f ${SEMAFOR_HOME}/scripts/tokenizer.sed ${INPUT_FILE} > ${TOKENIZED}
echo "Finished tokenization."
echo "**********************************************************************"
echo
echo

echo "**********************************************************************"
echo "Part-of-speech tagging tokenized data...."
pushd ${SEMAFOR_HOME}/scripts/jmx
time ./mxpost tagger.project < ${TOKENIZED} > ${POS_TAGGED}
popd
echo "Finished part-of-speech tagging tokenized data."
echo "**********************************************************************"
