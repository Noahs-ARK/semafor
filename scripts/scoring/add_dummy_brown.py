#!/usr/bin/env python

"""Adds dummy brown cluster features to conll file
   Input = conll file
   Output = Console """

__author__      = "swabha swayamdipta"


import sys

if __name__ == "__main__":
    f = open(sys.argv[1], "r")
    while True:
        line = f.readline()
        if not line:
            break
        line = line.strip()
        if line == "":
            print 
            continue
        ele = line.split("\t")
        ele.append("_")
        ele.append("_")
        #ele.append("oov")
        print '\t'.join(ele)
    f.close()


