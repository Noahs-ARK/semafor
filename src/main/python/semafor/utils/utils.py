def ngrams(words, n):
    return zip(*[words[i:] for i in range(n)])


def get_coarse_pos(pos):
    pos = pos.upper()
    if pos == "PRP" or pos == "PRP$" or len(pos) <= 2:
        cpostag = pos
    else:
        cpostag = pos[0:2]
    return cpostag