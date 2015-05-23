semmodel=$1
cv=$2
divmetric=$3
semhome="/usr0/home/sswayamd/semafor/semafor/"

set -e #fail fast


cd $semhome/scripts/scoring
./runSwabhaDiversityTestWithGoldFrameId.sh $divmetric $cv $semmodel

cd $semhome
resdir=$semhome"/experiments/"$semmodel"/results/"$divmetric"/partial/"
python oracle.py $resdir

echo "done"
