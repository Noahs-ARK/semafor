#!/usr/bin/python
"""
Splits the cv.xxx.sentences.tokenized, cv.xxx.sentences.frame.elements and cv.xxx.sentences.yyy.conll
into specified number of folds for jacknifing
"""

import sys

model = "basic"


def split_tok(tok_file, num_folds, dest):
    f = open(tok_file, 'r')
    lines = [l.strip() for l in f]
    f.close()
    req_sents = int(len(lines)/num_folds)
    print "total = ", req_sents
    extn = ".test.sentences.tokenized"

    tokfname = tok_file.split("/")[-1]
    split_pts=[] 
    for i in range(num_folds):
        if req_sents*(i+2) > len(lines):
            end = len(lines)
        else:
            end = req_sents*(i+1)
        split_pts.append(end)

        fn = dest + "/cv" +  str(i) + "/cv" + str(i) + extn;
        print fn 
        wf = open(fn, 'w')
        for l in lines[req_sents*i:end]:
            wf.write(l+"\n")
        wf.close()
    #split_pts = split_pts[:-1]
    print split_pts
    return split_pts

# BUGGGY. IF THE SPLIT SIZES ARE UNEVEN IT WRITES WRONG SENTENCE NUMBERS IN THE VERY LAST FILE
def split_fe(fe_file, sp, dest, extn):
    f = open(fe_file, 'r')

    fefname = fe_file.split("/")[-1]
    
    
    split_pts = [i for i in reversed(sp)]
    req_num = sp[0]
    print split_pts
    k = 0
    #split_pts.pop()
    fname = dest + "/cv" + str(k) + "/cv" + str(k) + extn
    print fname
    fn = open(fname, "w")
    line = 0
    for l in f:
        line += 1
        ele = l.strip().split("\t")
        num = int(ele[7])
        if (num in split_pts or num > split_pts[-1]):
            x = split_pts.pop()
            print "at example",num
            print "popped", x, "from", split_pts
            if num != 0:
                fn.close()
            k += 1
            fname = dest + "/cv" + str(k) + "/cv" + str(k) + extn
            print fname
            fn = open(fname, "w")
        ele[7] = str(int(ele[7]) % req_num)
        fn.write(("\t").join(ele) +"\n")
    f.close()
    fn.close()

def split_conll(conll_file, splits, dest):
    sent = 0
    cf = open(conll_file, 'r')
    extn = ".test.sentences.turboparsed." + model + ".stanford.lemmatized.conll"

    cfname = conll_file.split("/")[-1]
    
    i = 0 
    wfname = dest + "/cv" + str(i) + "/cv" + str(i) +  extn
    print wfname   
    wf = open(wfname, "w")

    for l in cf:
        if l.strip()=='':
            sent += 1
            if sent in splits:
                wf.write(l)
	        wf.close()
		i += 1
    		wfname = dest + "/cv" + str(i) + "/cv" + str(i) +  extn
    		print wfname   
    		wf = open(wfname, "w")
                continue
        wf.write(l)
    wf.close()
    cf.close()

if __name__=="__main__":

    file_to_be_split = sys.argv[1] # /path_to/cv.xxx.sentences
    #model = sys.argv[2]
    num_folds = int(sys.argv[2])
    dest = sys.argv[3] 

    tok_file = file_to_be_split+".tokenized"
    conll_file = file_to_be_split+".turboparsed." + model + ".stanford.lemmatized.conll"
    fe_file = file_to_be_split+".frame.elements"
    frames_file = file_to_be_split+".frames"

    split_pts = split_tok(tok_file, num_folds, dest)
    split_fe(fe_file, split_pts, dest, ".test.sentences.frame.elements")
    #split_fe(frames_file, split_pts, dest, ".test.sentences.frames")
    #split_conll(conll_file, split_pts[:-1])
    
