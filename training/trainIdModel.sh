#!/bin/bash

# train the frameId model

set -e # fail fast
set -x

my_dir="$(dirname ${BASH_SOURCE[0]})"

${my_dir}/3_1_idCreateAlphabet.sh
${my_dir}/3_2_idCreateFeatureEvents.sh
# the following step often throws an error at the end of LBFGS, even though it's successful
${my_dir}/3_3_idTrainBatch.sh || true
${my_dir}/3_4_idConvertAlphabetFile.sh
