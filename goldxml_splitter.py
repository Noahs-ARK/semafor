#!/usr/bin/python
"""
Splits the cv.xxx.sentences.tokenized, cv.xxx.sentences.frame.elements 
and cv.xxx.sentences.yyy.conll into specified number of folds for jacknifing
"""

import sys

def split_xml(xmlfilename):
	f = open(xmlfilename, 'r')
	lines = [l for l in f]
	f.close()


	header = lines[:7]
	footer = lines[-6:]
	extn=".lrb.xml"

	xf = open(xmlfilename[:-len(extn)]+"_0"+extn, "w")
        for h in header:
		xf.write(h)

	splits = ["556", "1112", "1668", "2224"]
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
				xf = open(xmlfilename[:-len(extn)] + "_" + str(cv) + extn, "w")
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


if __name__ == "__main__":
	split_xml(sys.argv[1])
