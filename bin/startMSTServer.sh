#!/bin/bash
#    The script to start the MST parser server.
#    Written by Dipanjan Das (dipanjan@cs.cmu.edu)
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

set -e # fail fast

MY_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"
source "${MY_DIR}/config.sh"

cd ${MST_PARSER_HOME}
echo "Current directory: ${MST_PARSER_HOME}"

TEMP_DIR="$(mktemp -d -t mst.XXXXXXXXXX)"
echo "TEMP_DIR: ${TEMP_DIR}"

${JAVA_HOME_BIN}/java -classpath ".:./lib/trove.jar:./lib/mallet.jar:./lib/mallet-deps.jar" -Xms8g -Xmx8g \
mst.DependencyEnglish2OProjParser ${MST_MODEL_DIR}/wsj.model ${TEMP_DIR} ${MST_PORT}

rm -r "${TEMP_DIR}"
