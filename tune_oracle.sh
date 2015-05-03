alphas="0.7"
for alpha in $alphas ; do

model="basic"
mdl="basic"
numtest="674" #"951"
prefix="dev"  # tuning is done on the dev set
metric="framenet_${prefix}_tune/turbo1_${alpha}"

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

scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric  "training/data/naacl2012/cv.${prefix}.sentences.tokenized"  "training/data/naacl2012/cv.${prefix}.sentences.frames"  "training/data/naacl2012/cv.${prefix}.sentences.turboparsed."$mdl".matsumoto.all.lemma.tags"  "experiments/"$model 

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric $prefix $model

cd ../../
echo "alpha = " $alpha >> basicresult.txt
python oracle.py $metric $model $numtest >> basicresult.txt

done
