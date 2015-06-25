#!/usr/env/python
import sys


def shorten_conlls(conllfile, newconllfile, valids):
	f = open(conllfile, "r")
	lines = [l for l in f]
	f.close()

	conlls = []
	conll = ""
	for line in lines:
		toks = line.strip()
		if toks == "":
			conlls.append(conll)
			conll = ""
			continue
		conll += line

	print "original conll size = ", len(conlls)

	wf = open(newconllfile, "w")
	for i in valids:
		wf.write(conlls[i])
		wf.write("\n")
	wf.close()


def shorten_toks(tokfile, newtokfile, valids):
	f = open(tokfile, "r")
        lines = [l for l in f]
        f.close()

	wf = open(newtokfile, "w")
        for i in valids:
                wf.write(lines[i])
        wf.close()

def shorten_fes(fefile, newfefile):
	f = open(fefile, "r")
        lines = [l for l in f]
        f.close()

        fes = []
        idx = 0
        prev = 1# CHANGE THIS FOR DIFFERENT CVS - TEST TRAIN OR DEV
        fe = ""
        for line in lines:
                toks = line.strip().split("\t")
		current = int(toks[7])
                if current > prev :
                        prev = current
			idx = idx + 1
                        fes.append(fe)
                        fe = ""
                toks[7] = str(idx)
                fe += "\t".join(toks) + "\n"
	fes.append(fe)

        print "number of fes = ", len(fes)

        wf = open(newfefile, "w")
        for fe in fes:
                wf.write(fe)
        wf.close()

if __name__ == "__main__":
	validfile=sys.argv[1] # valid example numbers, as per semafor evaluation
	
	#conllfile=sys.argv[2] # file in which only the valid example numbers need to be kept
	#newconllfile = conllfile[:-len(".conll")] + ".concise.conll"
	
#	tokfile = sys.argv[3]
#	newtokfile = tokfile[:-len(".tokenized")] + ".concise.tokenized"
#
	fefile = sys.argv[4]
        newfefile = fefile[:-len(".frames")] + ".concise.frames"

	vf = open(validfile,"r")
	valids=[int(l) for l in vf]
	vf.close()
        
	#shorten_conlls(conllfile, newconllfile, valids)
        #shorten_toks(tokfile, newtokfile, valids)
	shorten_fes(fefile, newfefile)
