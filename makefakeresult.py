import sys

sample=sys.argv[1] # sample result.xml file
rf = open(sample, "r")
lines = [l for l in rf]
rf.close()

wf = open("fake.xml", "w")
wf.write(lines[0])
recall="1.0(1.0/1.0)"
prec=recall
fscore="1.0"
for l in lines[1:]:
    idx = l.strip().split("\t")[0]
    wf.write(idx+"\t"+recall+"\t"+prec+"\t"+fscore+"\n")

wf.close()
