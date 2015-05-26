#!/usr/bin/python
"""
Splits the cv.xxx.sentences.lrb.xml into specified number of folds for jacknifing
"""

import sys

def split_xml(xmlfilename):
	f = open(xmlfilename, 'r')
	lines = [l for l in f]
	f.close()


	header = lines[:7]
	footer = lines[-6:]
	extn=".test.sentences.lrb.xml"
        dest="/usr0/home/sswayamd/semafor/semafor/training/data/cv/cv"
	xf = open(dest+"0/cv0"+extn, "w")
        for h in header:
		xf.write(h)

	splits = ["705", "1410", "2115", "2820"]#["556", "1112", "1668", "2224"]
	starter = "<sentence ID="

	cv = 1
        sentId = 0
	for l in lines[6:-6]:
		line = l.strip()
		if line.startswith(starter):
			if len(splits)>0 and line == '<sentence ID=\"'+splits[0]+'\">':
				print line
				sentId=0
				splits = splits[1:]
				for f in footer:
					xf.write(f)
				xf.close()
				xf = open(dest + str(cv) + "/cv" + str(cv) + extn, "w")
				for h in header:
					xf.write(h)
				cv += 1
                        xf.write('            <sentence ID=\"'+str(sentId)+'\">\n')
                        sentId += 1
			continue
		xf.write(l)
	for f in footer:
 		xf.write(f)
	xf.close()

def combine_xml(train, dev, out):
        trainsz = 2780

	xf = open(train, "r")
	lines = [l for l in xf]
	footer = lines[-6:]
	lines = lines[:-6]
	xf.close()

	xf = open(dev, "r")
	devlines = [l for l in xf][7:-6]
	xf.close()

	for l in devlines:
		if l.strip().startswith("<sentence ID="):
		        ele = l.strip().split('\"')
			oldnum = int(ele[1])
			lines.append('            <sentence ID=\"'+str(oldnum + trainsz)+'\">\n')
        	else:
			lines.append(l)
	wf = open(out, "w")
	for l in lines:
		wf.write(l)
	for l in footer:
		wf.write(l)
	wf.close()

if __name__ == "__main__":
	split_xml(sys.argv[1])
	#sourcedir = sys.argv[1]
	#extn = ".sentences.lrb.xml"
	#train = sourcedir+"/cv.train"+extn
	#combine_xml(train, sourcedir+"/cv.dev"+extn, sourcedir+"/cv.train+dev"+extn)
