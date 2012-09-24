#!/bin/bash
source config

CLASSPATH=".:${SEMAFOR_HOME}:${SEMAFOR_HOME}/lib/semafor-deps.jar"

scala -classpath ${CLASSPATH}
