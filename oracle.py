import sys, os.path


def read_file(rank, directory):
    
    f=open(directory + str(rank) +"thBest.argid.predict.xml","r")
    lines = [l.strip() for l in f][1:]
    f.close()

    scores = [float(line.split("\t")[-1]) for line in lines] 
    return scores

def calc_oracle_score(directory):
    ranks = len([f for f in os.listdir(directory) if os.path.isfile(os.path.join(directory, f))])
    s = read_file(0, directory)
    examples = len(s)
    print examples, "examples", ranks, "ranks"

    scores = [[0 for i in range(ranks)] for j in range(examples)] # oracle scores
    avg = [0 for i in range(ranks)] # oracle corpus scores


    for ex in range(examples):
	scores[ex][0] = s[ex]
    avg[0] = sum(s) * 1.0 / examples

    for rank in range(1,ranks):
	sc = read_file(rank, directory)
        tot = 0
	for ex in range(examples):
            scores[ex][rank] = max(scores[ex][rank-1], sc[ex])
            tot += scores[ex][rank]
	avg[rank] = tot *1.0 / examples

#    for e in range(10):
#        for r in range(10):
#            print scores[e][r],
#        print
#    for r in range(ranks):
#        if r%10 == 0 or r < 10:
#            print avg[r]
    print avg[0],avg[-1]
    return avg[-1]

if __name__ == "__main__":
    #metric = sys.argv[1] # diversity metric name
    #model = sys.argv[2] # semafor model name
    #examples=int(sys.argv[3]) # num of framenet dev/test examples with gold annotations
    #directory = "/usr0/home/sswayamd/semafor/semafor/experiments/" + model + "/results/" + metric + "/partial/";
    directory = sys.argv[1]
    calc_oracle_score(directory)

