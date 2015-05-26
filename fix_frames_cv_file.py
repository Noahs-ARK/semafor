#!/usr/bin/python

import sys

chunksz = 705
numchunks = 4 # number of chunks to combine

# hack to fix the sentnumbers if the extra sentence has been seen
def get_new_exnum(seen705, oldexnum, chunk):
    if (seen705):
	return chunk * chunksz + 1 + oldexnum
    return chunk * chunksz + oldexnum
    
def split_fe(fe_file, dest):
    
    f = open(fe_file, "r")
    wf = open(dest, "w")

    chunk = 0
    prev = 0
    seen705 = False
    for l in f:
        ele = l.strip().split("\t")
        old = int(ele[7])
        if abs(old - prev) > 500:
            chunk += 1
        if (prev == 705 and old != prev):
            seen705 = True
            #print "Seen 705 #######"
        new = get_new_exnum(seen705, old, chunk) 
        ele[7] = str(new)
        #print old, new
        wf.write(("\t").join(ele) + "\n");
        prev = old
    f.close()
    wf.close()

if __name__ == "__main__":
    fe_file = sys.argv[1]
    dest_file = sys.argv[2]
    split_fe(fe_file, dest_file)
