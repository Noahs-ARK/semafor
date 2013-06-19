#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast

my_dir="$(dirname ${0})"

${my_dir}/4_1_createAlphabet.sh
${my_dir}/4_2_cacheFeatureVectors.sh
${my_dir}/4_3_training.sh
