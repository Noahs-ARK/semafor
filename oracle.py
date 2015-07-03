import sys, os.path

extn = "thBest.argid.predict.xml"

def read_file(num_ranks, directory):
    fscores = []
    rec_denom = []
    rec_num = []
    pre_denom = []
    pre_num = []
    
    for rank in range(num_ranks):
	f=open(directory + str(rank) + extn,"r")
        lines = [l.strip().split("\t") for l in f][1:]
        if "Scored" in lines[-1][0]:
            lines = lines[:-1]
        f.close()

        fscores.append([float(line[-1]) for line in lines]) 
        rec_num.append([float(line[1].split("(")[1].split("/")[0]) for line in lines])
        rec_denom.append([float(line[1].split("/")[1][:-1]) for line in lines])

        pre_num.append([float(line[2].split("(")[1].split("/")[0]) for line in lines])
        pre_denom.append([float(line[2].split("/")[1][:-1]) for line in lines])
    return fscores, rec_denom, rec_num, pre_denom, pre_num

def calc_all_oracle_scores(directory):
    num_ranks = len([f for f in os.listdir(directory) if os.path.isfile(os.path.join(directory, f))])
    return calc_oracle(directory, num_ranks)

def calc_oracle(directory, num_ranks):

    fscores, rec_denom, rec_num, pre_denom, pre_num = read_file(num_ranks, directory)
    num_examples = len(fscores[0])
    print num_examples, "examples", num_ranks, "ranks"
    print "Oracle\tMacro \tPrecision  \tRecall \tF1-Score\tTP \tTP+FP \tTP+FN"
    
    numrankstotry = num_ranks/25

    rank = 1
    while (rank <= num_ranks):
            
        avg, p, r, microavg, tpn, tpd, trn, trd = do_micro_avg(fscores, rec_denom, rec_num, pre_denom, pre_num, rank)
        print "%d\t%.5f\t%.5f \t%.5f\t%.5f \t%.1f\t%.1f\t%.1f" % (rank, avg, p, r, microavg, tpn, tpd, trd)
        #avg, p, r, microavg = do_micro_avg(fscores, rec_denom, rec_num, pre_denom, pre_num, rank)
	#print "%d\t%.5f\t%.5f \t%.5f\t%.5f" % (rank, avg, p, r, microavg)
        if rank == 1:
            rank = 25
        else:
            rank += 25
    avg, p, r, microavg, tpn, tpd, trn, trd = do_micro_avg(fscores, rec_denom, rec_num, pre_denom, pre_num, num_ranks)
    print "%d\t%.5f\t%.5f \t%.5f\t%.5f \t%.1f\t%.1f\t%.1f" % (num_ranks, avg, p, r, microavg, tpn, tpd, trd)
    print
    return microavg

def do_micro_avg(fscores, rec_denom, rec_num, pre_denom, pre_num, num_ranks):
    num_examples = len(fscores[0])
    totpn = 0.0 
    totpd = 0.0
    totrn = 0.0
    totrd = 0.0
    totf = 0.0
	
    for ex in range(num_examples):
        maxf = float("-inf")
        maxpn = 0.0
        maxpd = 0.0
        maxrn = 0.0
        maxrd = 0.0
	for rank in range(num_ranks):
            if maxf < fscores[rank][ex]:
		maxf = fscores[rank][ex]
                maxpn = pre_num[rank][ex]
		maxpd = pre_denom[rank][ex]
                maxrn = rec_num[rank][ex]
                maxrd = rec_denom[rank][ex]
        totpn += maxpn
        totpd += maxpd
        totrn += maxrn
        totrd += maxrd
        totf += maxf

    avg = totf * 1.0 / num_examples
    p = totpn * 1.0 /totpd
    r = totrn * 1.0 /totrd
    microavg = 2.0 * p * r / (p + r)
    return avg, p, r, microavg, totpn, totpd, totrn, totrd
#    print "debug", avg[0]
#    return microavg[-1]

if __name__ == "__main__":
    directory = sys.argv[1]
    if (len(sys.argv) == 2):
        calc_all_oracle_scores(directory)
    else:
        num_ranks = int(sys.argv[2])
	calc_oracle(directory, num_ranks)
