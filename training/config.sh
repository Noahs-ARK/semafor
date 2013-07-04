#!/bin/bash -e                                                                                       

source "$(dirname ${0})/../bin/config.sh"




# choose a name for the model to train
#model_name="mst_frame_id_20130625"
#model_name="ancestor_frame_id_20130626"
model_name="ancestor_frame_id_partial_credit_20130627"

# should set to roughly the number of cores available
num_threads=8
gc_threads=2

classpath=".:${SEMAFOR_HOME}/target/Semafor-3.0-alpha-03.jar"
# the directory that contains framenet.frame.element.map and framenet.original.map
datadir="${SEMAFOR_HOME}/training/data"

# the directory the resulting model will end up in
model_dir="${datadir}/${model_name}"

id_features="ancestor"

old_model_dir="${MALT_MODEL_DIR}"

fn_id_req_data_file="${model_dir}/reqData.jobj"


# paths to the gold-standard annotated sentences, and dependency-parsed version of it
training_dir="${datadir}/naacl2012"
fe_file="${training_dir}/cv.train.sentences.frame.elements"
parsed_file="${training_dir}/cv.train.sentences.all.lemma.tags"
fe_file_length=`wc -l ${fe_file}`
fe_file_length=`expr ${fe_file_length% *}`

# path to store the alphabet we create:
alphabet_file="${model_dir}/alphabet.dat"

SCAN_DIR="${model_dir}/scan"

echo model_dir="${model_dir}"
