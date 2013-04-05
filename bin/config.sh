#!/bin/sh
######################## ENVIRONMENT VARIABLES ###############################
######### change the following according to your own local setup #############


# assumes this script (config.sh) lives in "${BASE_DIR}/semafor/bin/"
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )"
# path to the absolute path
# where you decompressed SEMAFOR.
SEMAFOR_HOME="${BASE_DIR}/semafor"

# Temporary folder location:
# Change the path to a custom temp directory if you want to control where Semafor puts its
# temp files. Will use system default if not specified.
#TMPDIR="${SEMAFOR_HOME}/temp"

# This indicates whether MST parser should run in the server mode or not
# Modes are "server" and "noserver"
MST_MODE=noserver

# Location of the MST parser root directory: Please change the 
# following path to the place where you decompressed stackedParserServer
# (to be downloaded from http://semafor-semantic-parser.googlecode.com/files/stackedParserServer.tgz).
MST_PARSER_HOME="${BASE_DIR}/stackedParserServer"

# Name of the machine where the MST parser server is running
# default is localhost, change if necessary.
MST_MACHINE=localhost

# Number of the port at which you want to run the MST parser --
# change if necessary.
MST_PORT=12345

# Change the following to the bin directory of your $JAVA_HOME
JAVA_HOME_BIN="/usr/bin"

# Change the following to the directory where you decompressed 
# the models for SEMAFOR 2.0.
MALT_MODEL_DIR="${BASE_DIR}/models/semafor_malt_model_20121129"
MST_MODEL_DIR="${BASE_DIR}/models/sem_2_1_20120522"

# If you want to use gold targets, 
# point to gold target file's absolute path. 
# if not, use "null" as the flag's value.
# Format of the gold target file 
# is space separated sentence spans, such as: 0, 1_4.
# Each line in the file corresponds to one sentence in the 
# input raw text file.
# Span indices start from 0
# Check out sample at ${SEMAFOR_HOME}/samples/sample_gold_targets.txt
GOLD_TARGET_FILE=null

# If using "auto" target identification, use "relaxed" or 
# "strict" identification.
# The relaxed identification scheme marks most words except proper 
# nouns to be targets
# The modes are "relaxed" and "strict"
AUTO_TARGET_ID_MODE=strict

# If you want to use the smoothed graph for frame identification (see 
# Das and Smith's ACL 2011 & NAACL 2012 papers), use "yes", else use "no"
USE_GRAPH_FILE=yes

# If you want to use beam decoding for argument identification, which 
# only prevents argument overlap, use "beam", else if you want exact
# decoding in the form AD^3 (see Das et al. *SEM 2012 paper) that respects
# multiple constraints, use "ad3". Note that results may not match *SEM 2012
# because of different Java implementation (paper used C++).
DECODING_TYPE="beam"


######################## END ENVIRONMENT VARIABLES #########################

echo "Environment variables:"
echo "SEMAFOR_HOME=${SEMAFOR_HOME}"
echo "TMPDIR=${TMPDIR}"
echo "MST_PARSER_HOME=${MST_PARSER_HOME}"
echo "MST_MODE=${MST_MODE}"
echo "MST_MACHINE=${MST_MACHINE}"
echo "MST_PORT=${MST_PORT}"
echo "JAVA_HOME_BIN=${JAVA_HOME_BIN}"
echo "MALT_MODEL_DIR=${MALT_MODEL_DIR}"
echo "GOLD_TARGET_FILE=${GOLD_TARGET_FILE}"
echo "AUTO_TARGET_ID_MODE=${AUTO_TARGET_ID_MODE}"
echo "USE_GRAPH_FILE=${USE_GRAPH_FILE}"
echo "DECODING_TYPE=${DECODING_TYPE}"