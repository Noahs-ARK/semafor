#!/usr/bin/python
"""
After cross validation testing, combine the (partial) results files across all cv folds
"""

import sys

semhome = "/usr0/home/sswayamd/semafor/semafor/"
expdir = semhome + "experiments/"

model = "basic"
semmodel = model + "_tbps_cv"
metric = "tbps_" + model + "_cv"

splitsize = 556
numsplits = 5

ranks = 100
dest = expdir + "basic_tbps/semreranker_train/results/partial/"
extn = "thBest.argid.predict.xml"

header = "Sentence ID\tRecall\tPrecision\tFscore"

def resfilename(cv, rank):
	fn = expdir + semmodel + str(cv) + "/results/" + metric + str(cv) + "/partial/" + str(rank) + extn
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

