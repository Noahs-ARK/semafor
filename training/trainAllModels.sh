#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast
set -x

my_dir="$(dirname ${BASH_SOURCE[0]})"

${my_dir}/2_createRequiredData.sh

#${my_dir}/trainIdModel.sh

${my_dir}/trainArgModel.sh
