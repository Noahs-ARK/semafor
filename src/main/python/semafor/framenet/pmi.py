from itertools import chain, combinations, product
import codecs
import json
from math import log

import networkx as nx
import matplotlib as plt
from nltk import FreqDist

from semafor.framenet.frames import FrameHierarchy

THRESHOLD = 4


def draw_graph(graph):
    pos = nx.graphviz_layout(graph, prog='dot')
    nx.draw(graph, pos, node_color='#A0CBE2', edge_color='#BB0000', width=2, edge_cmap=plt.cm.Blues,
            with_labels=True)


def pmi(a, b):
    return log(pairs[a, b]) - log(pairs.N()) - log(unigrams[a]) - log(unigrams[b]) + 2 * log(
        unigrams.N())


h = FrameHierarchy.load()
# training data contains a bad frame
valid_names = {f.name for f in h._frames.values()}

with codecs.open("../../../training/data/naacl2012/cv.train.sentences.json", encoding="utf8") as train_file:
    train = [json.loads(line) for line in train_file]

unsorted_frames = ([(f['target']['spans'][0]['start'], f['target']['name'])
                    for f in s['frames']] for s in train)
frames = [[name for start, name in sorted(s) if name in valid_names]
          for s in unsorted_frames]
del unsorted_frames
unigrams = FreqDist(chain(*frames))
pairs = FreqDist(chain(*[[tuple(sorted(b)) for b in combinations(f, 2)] for f in frames]))
pmis = FreqDist({
    (a, b): pmi(a, b)
    for a, b in pairs.keys()
    if unigrams[a] >= THRESHOLD and unigrams[b] >= THRESHOLD
})


unigrams_with_ancestors = FreqDist(unigrams)
for u in unigrams:
    for a in h.ancestors(h._frames[u]):
        unigrams_with_ancestors.inc(a.name)