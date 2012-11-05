#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast

my_dir="$(dirname ${0})"

${my_dir}/2_createRequiredData.sh
${my_dir}/3_1_trainFrameIdentificationModel.sh
${my_dir}/3_2_combineAlphabets.sh
${my_dir}/3_3_createFeatureEvents.sh
# this step often throws an error at the end of LBFGS, even though it's successful
${my_dir}/3_4_trainBatch.sh || true
${my_dir}/3_5_convertAlphabetFile.sh
${my_dir}/4_1_createAlphabet.sh
${my_dir}/4_2_cacheFeatureVectors.sh
${my_dir}/4_3_training.sh
