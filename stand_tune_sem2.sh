alpha="1.0"
betas="1.0"


model="standard"
mdl="standard"
numtest="674" #"951"
prefix="dev"  # tuning is done on the dev set

for beta in $betas; do

metric="framenet_${prefix}_tune/turbo2_${alpha}_${beta}"


set -e #fail fast

mkdir -p experiments/$model/results/$metric

mkdir -p experiments/$model/output/$metric/xml
mkdir -p experiments/$model/output/$metric/frameElements

mkdir -p experiments/$model/tmp/

scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric  "training/data/naacl2012/cv.${prefix}.sentences.tokenized"  "training/data/naacl2012/cv.${prefix}.sentences.frames"  "training/data/naacl2012/cv.${prefix}.sentences.turboparsed."$mdl".matsumoto.all.lemma.tags"  "experiments/"$model 

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric $prefix $model

cd ../../
echo "alpha = " $alpha >> resultstandard.txt
python oracle.py $metric $model $numtest >> resultstandard.txt

done
