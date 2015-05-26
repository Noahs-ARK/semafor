
dir="/usr0/home/sswayamd/semafor/semafor/training/data/cv/"
n=5

for i in {0..4};
do
touch $dir/cv$i/cv$i.train.sentences.tokenized
touch $dir/cv$i/cv$i.train.sentences.frame.elements
touch $dir/cv$i/cv$i.train.sentences.turboparsed.basic.stanford.lemmatized.conll
echo -n $i"~~"

for j in {0..4}
do
k=$(((i+j)%n))

if [ $i == $k ]
then
continue
fi
echo -n $k" "

cat $dir/cv$k/cv$k.test.sentences.tokenized >> $dir/cv$i/cv$i.train.sentences.tokenized
cat $dir/cv$k/cv$k.test.sentences.frame.elements >> $dir/cv$i/cv$i.train.sentences.frame.elements
cat $dir/cv$k/cv$k.test.sentences.turboparsed.basic.stanford.lemmatized.conll >> $dir/cv$i/cv$i.train.sentences.turboparsed.basic.stanford.lemmatized.conll
done

echo

done
