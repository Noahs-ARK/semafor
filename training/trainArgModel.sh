#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast
my_dir="$(dirname ${BASH_SOURCE[0]})"

source ${my_dir}/config.sh

${my_dir}/4_1_createAlphabet.sh
# apparently, we don't need to do : cp ${model_dir}/scan/parser.conf.unlabeled ${model_dir}/alphabet.dat
#${my_dir}/4_2_cacheFeatureVectors.sh
${my_dir}/4_3_training.sh


