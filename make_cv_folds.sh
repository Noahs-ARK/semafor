
dir="/usr0/home/sswayamd/semafor/semafor/training/data/cv/"
n=5

for i in {0..4};
do
touch $dir/cv$i/cv.train.sentences_$i.tokenized
touch $dir/cv$i/cv.train.sentences_$i.frame.elements
touch $dir/cv$i/cv.train.sentences.turboparsed.basic.stanford.lemmatized_$i.conll
echo -n $i"~~"

for j in {0..4}
do
k=$(((i+j)%n))

if [ $i == $k ]
then
continue
fi
echo -n $k" "

cat $dir/cv$k/cv.test.sentences_$k.tokenized >> $dir/cv$i/cv.train.sentences_$i.tokenized
cat $dir/cv$k/cv.test.sentences_$k.frame.elements >> $dir/cv$i/cv.train.sentences_$i.frame.elements
cat $dir/cv$k/cv.test.sentences.turboparsed.basic.stanford.lemmatized_$k.conll >> $dir/cv$i/cv.train.sentences.turboparsed.basic.stanford.lemmatized_$i.conll
done

echo

done
