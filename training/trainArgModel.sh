#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast
my_dir="$(dirname ${BASH_SOURCE[0]})"

source ${my_dir}/config.sh

${my_dir}/4_1_extractFeatures.sh
${my_dir}/4_2_training.sh
