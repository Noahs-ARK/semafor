import sys

metric=sys.argv[1]
model=sys.argv[2]

examples=int(sys.argv[3]) # int(sys.argv[1])
ranks=100

dir="experiments/"+model+"/results/"+metric+"/partial/";

scores=[[0 for i in range(ranks)] for j in range(examples)]
avg=[0 for i in range(ranks)]

f=open(dir + "0thBest.xml","r")
s=[line.strip().split("\t")[-1] for line in f][1:]

sc=[float(i) for i in s]
f.close()
    

for ex in range(examples):
    scores[ex][0]=sc[ex]
avg[0]=sum(sc)*1.0/examples

for rank in range(1,ranks):
    f=open(dir + str(rank) + "thBest.xml","r")
    s=[line.strip().split("\t")[-1] for line in f][1:]
    sc=[float(i) for i in s]
    f.close()
    for ex in range(examples):
        scores[ex][rank]=max(scores[ex][rank-1],sc[ex])
    avg[rank]=sum(scores[i][rank] for i in range(examples))*1.0/examples

print avg[0],avg[-1]

#diff = 0
#good_ex=0
#for ex in range(examples):
#    if (scores[ex][99] - scores[ex][0] > diff):
#        good_ex = ex
#        diff = scores[ex][99] - scores[ex][0]
#
#print "best ex : " , good_ex
print scores[198]
