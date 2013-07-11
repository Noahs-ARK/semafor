import codecs
import os
import numpy as np

from semafor.settings import SENNA_DATA_DIR


def load_senna(folder=SENNA_DATA_DIR):
    embeddings_txt = os.path.join(folder, "embeddings/embeddings.txt")
    words_filename = os.path.join(folder, "hash/words.lst")
    with open(embeddings_txt) as embeddings_file:
        embeddings = np.genfromtxt(embeddings_file, delimiter=' ', dtype='f8')
    with codecs.open(words_filename, encoding="utf-8") as in_file:
        words = [w.strip() for w in in_file.readlines()]
    return dict(zip(words, embeddings))
