#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast
set -x

my_dir="$(dirname ${BASH_SOURCE[0]})"

${my_dir}/4_1_extractFeatures.sh
${my_dir}/4_2_training.sh
