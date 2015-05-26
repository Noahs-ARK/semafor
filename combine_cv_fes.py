#!/usr/bin/python
"""
After cross validation testing, combine the frame.elements output files across all cv folds
"""

import sys

semhome = "/usr0/home/sswayamd/semafor/semafor/"
expdir = semhome + "experiments/"

model = "basic"
semmodel = model + "_tbps_cv"
metric = "tbps_" + model + "_cv"
extn = "thBest.argid.predict.frame.elements"
dest = expdir + "basic_tbps/output/semreranker_train/frameElements/"

splitsize = 705
numsplits = 4
ranks = 100

def fefilename(cv, rank):
	fn = expdir + semmodel + str(cv) + "/output/" + metric + str(cv) + "/frameElements/" + str(rank) + extn
	return fn

#def fefilename(splitnum):
#	if (splitnum == 0):
#		return semhome + "/training/data/naacl2012/cv.train.sentences.frame.elements"
#        return semhome + "/training/data/naacl2012/cv.dev.sentences.frame.elements"

if __name__ == "__main__":
	for rank in range(ranks):
		alllines = []
		for i in range(numsplits):
			fefn = fefilename(i, rank)
			f = open(fefn, "r")
			lines = [l for l in f]
			f.close()
			for line in lines:
				ele = line.strip().split("\t")
				ele[7] = str(int(ele[7])+i*splitsize)
				alllines.append("\t".join(ele))

		wfname = dest + str(rank) + extn #semhome + "/training/data/naacl2012/cv.train+dev.sentences.frame.elements" 
		wf = open(wfname, "w")
		for l in alllines:
			wf.write(l+"\n")

		wf.close()
	print "done!"

