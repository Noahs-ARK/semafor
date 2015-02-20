#!/breate required data, train the frameId model and train the argId model

set -e # fail fast

# dangerous to use here, since it will fail if RISO completes...



./swabha_all_lemma_tags.sh

traindir="$(dirname ${0})/../../training"

${traindir}/2_createRequiredData.sh
#${traindir}/trainIdModel.sh
${traindir}/trainArgModel.sh

