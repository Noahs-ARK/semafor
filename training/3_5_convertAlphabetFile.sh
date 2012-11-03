#!/bin/bash -e

# step 5: convert the alphabet file.

source "$(dirname ${0})/config"

# gets the last model file created
# todo: fragile, better to just pad the numbers and "ls | sort -r | tail -1"
num_model_files=$(ls ${model_dir}/idmodel.dat_* | wc -l | tr -d [[:space:]])
model_file="${model_dir}/idmodel.dat_${num_model_files}0"

${JAVA_HOME_BIN}/java -classpath ${classpath} -Xms8g -Xmx8g \
  edu.cmu.cs.lti.ark.fn.identification.ConvertAlphabetFile \
  ${alphabet_file} \
  ${model_file} \
  ${model_dir}/final_model.dat


