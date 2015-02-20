import sys

ranks=100
examples=951 # int(sys.argv[1])
metric=sys.argv[1]
model=sys.argv[2]

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
