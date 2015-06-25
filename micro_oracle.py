import sys, os.path


def read_file(rank, directory):
    
    f=open(directory + str(rank) +"thBest.argid.predict.xml","r")
    lines = [l.strip().split("\t") for l in f][1:]
    f.close()

    fscores = [float(line[-1]) for line in lines] 
    rec_num = [float(line[1].split("(")[1].split("/")[0]) for line in lines]
    rec_denom = [float(line[1].split("/")[1][:-1]) for line in lines]

    pre_num = [float(line[2].split("(")[1].split("/")[0]) for line in lines]
    pre_denom = [float(line[2].split("/")[1][:-1]) for line in lines]
    return fscores, rec_denom, rec_num, pre_denom, pre_num

def calc_oracle_score(directory):
    ranks = len([f for f in os.listdir(directory) if os.path.isfile(os.path.join(directory, f))])
    s, rec_denom, rec_num, pre_denom, pre_num = read_file(0, directory)
    examples = len(s)
    print examples, "examples", ranks, "ranks"

    scores = [[0.0 for i in range(ranks)] for j in range(examples)] # oracle f- scores
    pnums = [[0.0 for i in range(ranks)] for j in range(examples)] # oracle precision numerators
    rnums = [[0.0 for i in range(ranks)] for j in range(examples)] # oracle recall numerators
    pdenoms = [[0.0 for i in range(ranks)] for j in range(examples)] # oracle precision numerators
    rdenoms = [[0.0 for i in range(ranks)] for j in range(examples)] # oracle recall numerators

    avg = [0.0 for i in range(ranks)] # oracle corpus scores
    p = [0.0 for i in range(ranks)]
    r = [0.0 for i in range(ranks)]
    microavg = [0.0 for i in range(ranks)]


    for ex in range(examples):
	scores[ex][0] = s[ex]
        pnums[ex][0] = pre_num[ex]
        rnums[ex][0] = rec_num[ex]
        pdenoms[ex][0] = pre_denom[ex]
        rdenoms[ex][0] = rec_denom[ex]

    avg[0] = sum(s) * 1.0 / examples
    p[0] = sum(pre_num)/sum(pre_denom)
    r[0] = sum(rec_num)/sum(rec_denom)
    microavg[0] = 2.0 * p[0] * r[0] / (p[0] + r[0])

    for rank in range(1,ranks):
	sc, rec_denom, rec_num, pre_denom, pre_num = read_file(rank, directory)
        tot = 0.0
        ptot = 0.0
        rtot = 0.0
        pdtot = 0.0
        rdtot = 0.0
	for ex in range(examples):
            if (scores[ex][rank-1] > sc[ex]): 
		scores[ex][rank] = scores[ex][rank-1]
                pnums[ex][rank] = pnums[ex][rank-1]
                rnums[ex][rank] = rnums[ex][rank-1]
                pdenoms[ex][rank] = pdenoms[ex][rank-1]
                rdenoms[ex][rank] = rdenoms[ex][rank-1]
            else:
                scores[ex][rank] = sc[ex]
                pnums[ex][rank] = pre_num[ex]
                rnums[ex][rank] = rec_num[ex]
                pdenoms[ex][rank] = pre_denom[ex]
                rdenoms[ex][rank] = rec_denom[ex]
            
            tot += scores[ex][rank]
            ptot += pnums[ex][rank]
            rtot += rnums[ex][rank]
            pdtot += pdenoms[ex][rank]
            rdtot += rdenoms[ex][rank]

	avg[rank] = tot / examples
        p[rank] = ptot/pdtot
        r[rank] = rtot/rdtot
        microavg[rank] = 2.0 * p[rank] * r[rank] / (p[rank] + r[rank])
    print "Oracle  \t1-best\t25-best\t50-best\t75-best\t100-best"
    print "Macro F1\t",
    for rank in [0,24,49,74,99]:
	print "%.5f\t" % avg[rank],
    print "\nPrecision\t",
    for rank in [0,24,49,74,99]:
	print "%.5f\t" % p[rank],
    print "\nRecall  \t",
    for rank in [0,24,49,74,99]:
	print "%.5f\t" % r[rank],
    print "\nMicro F1\t",
    for rank in [0,24,49,74,99]:
	print "%.5f\t" % microavg[rank],
    print
    print "debug", avg[0]
    return microavg[-1]

if __name__ == "__main__":
    directory = sys.argv[1]
    calc_oracle_score(directory)

