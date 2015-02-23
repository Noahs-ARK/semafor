alphas="0.5 0.6 0.7 0.8 0.9"
for alpha in $alphas ; do

metric="framenet_test_tune/turbo1_${alpha}"
model="turbo_basic"
mdl="basic"
numtest="951"

# Need to copy alphabet.dat from scan/parser.conf.unlabeled
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

scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric  "training/data/naacl2012/cv.test.sentences.tokenized"  "training/data/naacl2012/cv.test.sentences.frames"  "training/data/naacl2012/cv.test.sentences.turboparsed."$mdl".matsumoto.all.lemma.tags"  "experiments/"$model 

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric "test"

cd ../../
echo "alpha = " $alpha >> testresult.txt
python oracle.py $metric $model $numtest >> testresult.txt

done
