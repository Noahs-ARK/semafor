#!/bin/bash
set -e # fail fast
set -x

source "$(dirname ${BASH_SOURCE[0]})/../../training/config.sh"
cd ${SEMAFOR_HOME}

for i in {00..29};
do
	echo "evaluating argmodel.dat__00"$i
        echo "----------------------------------------"
	rm -rf $model_dir/argmodel.dat
	ln -s $model_dir/argmodel.dat_00${i} $model_dir/argmodel.dat
	ls -lah $model_dir/argmodel.dat
	./scripts/scoring/runTestWithGoldFrames.sh dev ${i}
        echo "----------------------------------------"
done

