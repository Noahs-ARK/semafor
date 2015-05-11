#!/usr/bin/python
"""
Splits the cv.xxx.sentences.tokenized, cv.xxx.sentences.frame.elements and cv.xxx.sentences.yyy.conll
into specified number of folds for jacknifing
"""

import sys


def split_tok(tok_file, num_folds, dest):
    f = open(tok_file, 'r')
    lines = [l.strip() for l in f]
    f.close()
    req_sents = int(len(lines)/num_folds)
    print "total = ", req_sents
    extn = ".tokenized"

    tokfname = tok_file.split("/")[-1]
    split_pts=[] 
    for i in range(num_folds):
        if req_sents*(i+2) > len(lines):
            end = len(lines)
        else:
            end = req_sents*(i+1)
        split_pts.append(end)

        fn = dest + "/" + tokfname[:-len(extn)] + "_" + str(i) + extn;
        print fn 
        wf = open(fn, 'w')
        for l in lines[req_sents*i:end]:
            wf.write(l+"\n")
        wf.close()
    return split_pts

def split_fe(fe_file, sp, dest):
    f = open(fe_file, 'r')

    fefname = fe_file.split("/")[-1]
    extn = ".frame.elements"
    split_pts = [i for i in reversed(sp)]
    print split_pts
    req_num = sp[0]
    print req_num 
    k = 0
    #split_pts.pop()
    fname = dest + "/" + fefname[:-len(extn)] + "_" + str(k) + extn
    print fname
    fn = open(fname, "w")
    for l in f:
        ele = l.strip().split("\t")
        num = int(ele[7])
        #print split_pts, num
        if (num in split_pts or num > split_pts[-1]):
            print num, split_pts
            x = split_pts.pop()
            print x
            if num != 0:
                fn.close()
            k += 1
            fname = dest + "/" + fefname[:-len(extn)] + "_" + str(k) + extn
            print fname
            fn = open(fname, "w")
        ele[7] = str(int(ele[7]) % req_num)
        fn.write(("\t").join(ele) +"\n");

def split_conll(conll_file, splits, dest):
    sent = 0
    cf = open(conll_file, 'r')
    extn = ".conll"

    cfname = conll_file.split("/")[-1]
    
    f = open(dest + "/" + cfname[:-len(extn)] + "_0" + extn, "w")
    i = 1    
    for l in cf:
        if l.strip()=='':
            sent += 1
            if sent in splits:
                f.write(l)
	        f.close()
		f = open(dest + "/" + cfname[:-len(extn)] + "_" + str(i) + extn, "w")
		i += 1
                continue
        f.write(l)
    f.close()

if __name__=="__main__":

    file_to_be_split = sys.argv[1] # /path_to/cv.xxx.sentences
    conll_file = sys.argv[2] # /path_to/cv.xxx.sentences.yyy
    num_folds = int(sys.argv[3])
    dest = sys.argv[4] 

    tok_file = file_to_be_split+".tokenized"
    fe_file = file_to_be_split+".frame.elements"

    split_pts = split_tok(tok_file, num_folds, dest)
    split_fe(fe_file, split_pts, dest)
    split_conll(conll_file, split_pts, dest)
    
