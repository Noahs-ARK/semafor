#!/bin/bash

# Create required data, train the frameId model and train the argId model

set -e # fail fast
my_dir="$(dirname ${0})"

modelname="tune_"$1"_"$2
source ${my_dir}/tune_config.sh $modelname

l1=$1
l2=$2
iter=$3

#
${my_dir}/tune_2_createRequiredData.sh $modelname
${my_dir}/tune_4_1_createAlphabet.sh $modelname
# apparently, we don't need to do : cp ${model_dir}/scan/parser.conf.unlabeled ${model_dir}/alphabet.dat
${my_dir}/tune_4_2_cacheFeatureVectors.sh $modelname
${my_dir}/tune_4_3_training.sh $modelname $1 $2 $3
#
rm -rf ${experiments_dir}/model/parser.conf
ln -s ${experiments_dir}/model/scan/parser.conf.unlabeled ${experiments_dir}/model/parser.conf

#rm -rf ${experiments_dir}/model/argmodel.dat
ln -s ${experiments_dir}/model/argmodel.dat_0017  ${experiments_dir}/model/argmodel.dat
${my_dir}/../scripts/scoring/tune_runTestWithGoldFrames.sh $modelname "dev" > ${modelname}.out

echo "done!"
