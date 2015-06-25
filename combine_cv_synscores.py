#!/usr/bin/python
"""
After cross validation testing, combine the (partial) results files across all cv folds
"""

import sys

semhome = "/usr0/home/sswayamd/semafor/semafor/"
traindir = semhome + "training/data/"

splitsize = 705#556
numsplits = 4

ranks = 100
dest = traindir + "emnlp2015/semreranker.train.synscores/" 
extn = "thBest.synscore"

header = "Sentence ID\tSynscore"

def resfilename(cv, rank):
	fn = traindir + "cv/cv" + str(cv) + "/" + "cv" + str(cv) + ".test.sentences.turboparsed.basic.stanford.lemmatized/synscores/" + str(rank) + extn
	return fn

if __name__ == "__main__":
	for rank in range(ranks):
		alllines = [header]
		for i in range(numsplits):
			rfn = resfilename(i, rank)
			f = open(rfn, "r")
			lines = [l for l in f]
			f.close()
			lines = lines[1:]
			for line in lines:
				ele = line.strip().split("\t")
				ele[0] = str(int(ele[0])+i*splitsize)
				alllines.append("\t".join(ele))

		wf = open(dest + str(rank) + extn, "w")
		for l in alllines:
			wf.write(l+"\n")

		wf.close()
	print "done!"

