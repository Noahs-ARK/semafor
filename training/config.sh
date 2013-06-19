#!/bin/bash -e                                                                                       

source "$(dirname ${0})/../bin/config.sh"

# choose a name for the model to train
model_name="mst_frame_id_20130619"

# should set to roughly the number of cores available
num_threads=8

classpath=".:${SEMAFOR_HOME}/target/Semafor-3.0-alpha-03.jar"
# the directory that contains framenet.frame.element.map and framenet.original.map
datadir="${SEMAFOR_HOME}/training/data"
# the directory that contains all the lexical unit xmls for FrameNet 1.5
# you can also add your own xmls to this directory
# for format information, take a look at the lu/ directory under the FrameNet release
luxmldir="${datadir}/framenet15/lu"

# the directory the resulting model will end up in
model_dir="${datadir}/${model_name}"


# config files
wordnet_config_file="${SEMAFOR_HOME}/dict/file_properties.xml"
stopwords_file="${SEMAFOR_HOME}/dict/stopwords.txt"

old_model_dir="${MST_MODEL_DIR}"

framenet_map_file="${datadir}/framenet.original.map"
fe_dict_file="${datadir}/framenet.frame.element.map"
all_related_words_file="${old_model_dir}/allrelatedwords.ser"
hv_correspondence_file="${old_model_dir}/hvmap.ser"
wn_related_words_for_words_file="${old_model_dir}/wnallrelwords.ser"
wn_map_file="${old_model_dir}/wnMap.ser"
revised_map_file="${old_model_dir}/revisedrelmap.ser"
lemma_cache_file="${old_model_dir}/hvlemmas.ser"
fn_id_req_data_file="${old_model_dir}/reqData.jobj"


# paths to the gold-standard annotated sentences, and dependency-parsed version of it
training_dir="${datadir}/naacl2012"
fe_file="${training_dir}/cv.train.sentences.frame.elements"
parsed_file="${training_dir}/cv.train.sentences.all.lemma.tags"
fe_file_length=`wc -l ${fe_file}`
fe_file_length=`expr ${fe_file_length% *}`

# path to store the alphabet we create:
alphabet_file="${model_dir}/alphabet_combined.dat"

SCAN_DIR="${model_dir}/scan"
