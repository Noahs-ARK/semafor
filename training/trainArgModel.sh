#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast
set -x

my_dir="$(dirname ${BASH_SOURCE[0]})"

${my_dir}/4_1_createAlphabet.sh
${my_dir}/4_2_cacheFeatureVectors.sh
${my_dir}/4_3_training.sh
