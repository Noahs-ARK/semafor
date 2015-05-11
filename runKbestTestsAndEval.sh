metric="framenet_test_tune/turbo2_0.1_0.125"
model="basic_tbps"
mdl="basic"

cv="test"
numtests="951" # needs to change with CV test or dev

# Need to copy alphabet.dat from parser.conf
# Need to set argmodel.dat to whatever the last one was, right after training...

set -e #fail fast
#git clone <semafor from Sam>
#git checkout swabha
#module load maven<>
#mvn package
#module load scala<>

mkdir -p experiments/$model/results/$metric

mkdir -p experiments/$model/output/$metric/xml
mkdir -p experiments/$model/output/$metric/frameElements

mkdir -p experiments/$model/tmp/

echo "trying to run SCALA..."

scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric "training/data/naacl2012/cv."$cv".sentences.tokenized" "training/data/naacl2012/cv."$cv".sentences.frames" "training/data/naacl2012/cv."$cv".sentences.turboparsed."$mdl".matsumoto.all.lemma.tags" "experiments/"$model 

echo "done running SCALA"

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric $cv $model

cd ../../
python oracle.py $metric $model $numtests

