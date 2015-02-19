metric="turbo2_0.15"
model="turbo_standard_20150218"


set -e #fail fast
#git clone <semafor from Sam>
#git checkout swabha
#module load maven<>
#mvn package
#module load scala<>

mkdir -p experiments/$model/results/$metric
mkdir -p experiments/$model/output/$metric/frameElements
mkdir -p experiments/$model/output/$metric/xml


scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric

cd ../../
python oracle.py $metric

