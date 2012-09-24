#!/bin/bash
#    The script to compile the source code.
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

source "$(dirname `readlink -f ${0}`)/config"
cd ${SEMAFOR_HOME}
pwd
classpath=".:./lib/semafor-deps.jar"

find edu \( -name "*.class" \) -exec rm '{}' \;
${JAVA_HOME_BIN}/javac -cp ${classpath} edu/cmu/cs/lti/ark/fn/data/prep/CoNLLInputPreparation.java
${JAVA_HOME_BIN}/javac -cp ${classpath} edu/cmu/cs/lti/ark/fn/data/prep/AllAnnotationsMergingWithoutNE.java
${JAVA_HOME_BIN}/javac -cp ${classpath} edu/cmu/cs/lti/ark/fn/parsing/ParserDriver.java
${JAVA_HOME_BIN}/javac -cp ${classpath} edu/cmu/cs/lti/ark/fn/evaluation/PrepareFullAnnotationXML.java


cd ${MST_PARSER_HOME}
pwd
find mst \( -name "*.class" \) -exec rm '{}' \;
${JAVA_HOME_BIN}/javac -cp ".:./lib/trove.jar:./lib/mallet.jar:./lib/mallet-deps.jar" mst/DependencyEnglish2OProjParser.java
${JAVA_HOME_BIN}/javac -cp ".:./lib/trove.jar:./lib/mallet.jar:./lib/mallet-deps.jar" mst/DependencyParser.java