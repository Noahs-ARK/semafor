#!/usr/bin/python

import sys

def split_fe(fe_file, dest):
    chunksz = 556
    numchunks = 4
    
    f = open(fe_file, "r")
    wf = open(dest, "w")

    chunk = 0
    prev = 0
    for l in f:
        ele = l.strip().split("\t")
        old = int(ele[7])
        if abs(old - prev) > 500:
            chunk += 1
        new = chunk * chunksz + old
        ele[7] = str(new)
        wf.write(("\t").join(ele) + "\n");
        prev = old
    f.close()
    wf.close()

if __name__ == "__main__":
    fe_file = sys.argv[1]
    dest_file = sys.argv[2]
    split_fe(fe_file, dest_file)
