metric="turbo2_0.15"
model="turbo_standard_20150218"

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

scala -cp "target/Semafor-3.0-alpha-05-SNAPSHOT.jar" -J-Xms4g -J-Xmx4g -J-XX:ParallelGCThreads=6 scripts/scoring/SwabhaDiversity.scala $metric

cd scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $metric

cd ../../
python oracle.py $metric $model

