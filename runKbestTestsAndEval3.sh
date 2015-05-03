metric="small"
model="basic"
mdl="basic"
numtests="951"

# Need to copy alphabet.dat from parser.conf
# Need to set argmodel.dat to whatever the last one was, right after training...

set -e #fail fast

mkdir -p experiments/$model/results/$metric

mkdir -p experiments/$model/output/$metric/xml
mkdir -p experiments/$model/output/$metric/frameElements

mkdir -p experiments/$model/tmp/

scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric "training/data/naacl2012/cv.test.sentences.tokenized" "training/data/naacl2012/cv.test.sentences.frames" "training/data/naacl2012/cv.test.sentences.turboparsed."$mdl".matsumoto.all.lemma.tags" "experiments/"$model 

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric "test" $model

#cd ../../
#python oracle.py $metric $model $numtests

