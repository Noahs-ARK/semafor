#!/usr/bin/env Python2.7
"""
Utilities for looking up types of noun phrases in the NELL KB
"""
import codecs
from itertools import chain
from os.path import join

from nltk import ngrams
import sys
from semafor.utils.malt_to_conll import read_conll

from semafor.settings import TRAINING_DATA_DIR
from semafor.nell.nell import load_noun_types, load_hierarchy

infixes = ('train', 'dev', 'test')
in_template = 'naacl2012/cv.%s.sentences.mstparsed.conll'
out_template = 'naacl2012/cv.%s.sentences.nell'
#SENTENCE_FILE = join(TRAINING_DATA_DIR, "naacl2012/cv.train.sentences.mstparsed.conll")  # naacl2012/cv.train.sentences")


def contains_noun(phrase):
    return any(t.postag.lower().startswith("n") for t in phrase)


def lookup_phrases(sentence, noun_types, ignore_case=False):
    phrases = ngrams(sentence, 3) + ngrams(sentence, 2) + ngrams(sentence, 1)
    matches = []
    for phrase in phrases:
        if contains_noun(phrase):
            phrase_str = u' '.join(w.form for w in phrase)
            if ignore_case:
                phrase_str = phrase_str.lower()
            types = noun_types.get(phrase_str)
            if types:
                matches.append((phrase, types))
    return sorted(matches)


def main(lines, out=sys.stdout):
    sentences = read_conll(lines, lookup_lemmas=True)
    sentence_idx = 0
    for sentence in sentences:
        matches = lookup_phrases(sentence, noun_types)
        for tokens, types in matches:
            all_ancestors = set(chain(*[hierarchy.adj[t] for t, c in types]))

            out.write(u"%s\t%s\t%s\n" % (sentence_idx,
                                         '_'.join(str(t.id) for t in tokens),
                                         ' '.join(all_ancestors)))
        sentence_idx += 1


if __name__ == "__main__":
    #sentence_file = sys.argv[1] if len(sys.argv) > 1 else sys.stdin  # SENTENCE_FILE
    noun_types = load_noun_types()
    hierarchy = load_hierarchy()
    for infix in infixes:
        in_file = join(TRAINING_DATA_DIR, in_template % infix)
        out_file = join(TRAINING_DATA_DIR, out_template % infix)
        with codecs.open(in_file, encoding='utf8') as in_file, codecs.open(out_file, 'w', encoding='utf8') as out_file:
            main(in_file, out_file)
