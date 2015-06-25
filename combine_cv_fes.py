#!/usr/bin/python
"""
After cross validation testing, combine the .frame.elements / .frames output files across all cv folds
"""

import sys

semhome = "/usr0/home/sswayamd/semafor/semafor/"
expdir = semhome + "experiments/"

model = "basic"
semmodel = model + "_tbps_cv"
metric = "tbps_" + model + "_cv"
extn = "thBest.argid.predict.frame.elements"
dest = expdir + "basic_tbps/output/semreranker_train/frameElements/"

foldsize = 705 #2780 
numsplits = 4 #2
ranks = 100 #1

def fefilename(cv, rank):
	fn = expdir + semmodel + str(cv) + "/output/" + metric + str(cv) + "/frameElements/" + str(rank) + extn
	#fn = semhome + "training/data/cv/cv" + str(cv) + "/cv" + str(cv) +".test.sentences.frame.elements"
	return fn

#def fefilename(splitnum):
#	if (splitnum == 0):
#		return semhome + "/training/data/naacl2012/cv.train.sentences.frames"
#        return semhome + "/training/data/naacl2012/cv.dev.sentences.frames"

if __name__ == "__main__":
	for rank in range(ranks):
		alllines = []
		for fold in range(numsplits):
			fefn = fefilename(fold, rank)
                        print "read",fefn
			f = open(fefn, "r")
			lines = [l for l in f]
			f.close()
			for line in lines:
				ele = line.strip().split("\t")
				ele[7] = str(int(ele[7]) + fold*foldsize)
				alllines.append("\t".join(ele))

		#wfname = semhome + "/training/data/emnlp2015/semreranker.train.sentences.frame.elements"
		wfname = dest + str(rank) + extn #"/training/data/naacl2012/cv.train+dev.sentences.frames" 
                print "write", wfname
		wf = open(wfname, "w")
		for l in alllines:
			wf.write(l+"\n")

		wf.close()
	print "done!"

