#!/bin/bash -e                                                                                       

source "$(dirname "${BASH_SOURCE[0]}")/../bin/config.sh"



# choose a name for the model to train
export model_name="changeme"

# should set to roughly the number of cores available
export num_threads=8
export gc_threads=2

export classpath="${CLASSPATH}"
# the directory that contains framenet.frame.element.map and framenet.original.map
export datadir="${SEMAFOR_HOME}/training/data"

export experiments_dir="${SEMAFOR_HOME}/experiments/${model_name}"

# the directory the resulting model will end up in
export model_dir="${experiments_dir}/model"

# the directory the evaluation results will end up in
export results_dir="${experiments_dir}/results"

export id_features="ancestor"

export old_model_dir="${MALT_MODEL_DIR}"

export fn_id_req_data_file="${model_dir}/reqData.jobj"


# paths to the gold-standard annotated sentences, and dependency-parsed version of it
export training_dir="${datadir}/naacl2012"
export fe_file="${training_dir}/cv.train.sentences.frame.elements"
export parsed_file="${training_dir}/cv.train.sentences.all.lemma.tags"
export fe_file_length=`wc -l ${fe_file}`
export fe_file_length=`expr ${fe_file_length% *}`

# path to store the alphabet we create:
export alphabet_file="${model_dir}/alphabet.dat"

export SCAN_DIR="${model_dir}/scan"

echo num_threads="${num_threads}"
echo gc_threads="${gc_threads}"
echo datadir="${datadir}"
echo id_features="${id_features}"
echo fn_id_req_data_file="${fn_id_req_data_file}"
echo training_dir="${training_dir}"
echo fe_file="${fe_file}"
echo parsed_file="${parsed_file}"
echo fe_file_length="${fe_file_length}"
echo alphabet_file="${alphabet_file}"
echo SCAN_DIR="${SCAN_DIR}"
