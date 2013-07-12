#!/usr/bin/env python2.7
"""
Extracts features for each unigram

@author Sam Thomson (sthomson@cs.cmu.edu)
"""
from __future__ import print_function, division
from csv import DictWriter
import codecs
from itertools import chain
import json
import os
import sys
#from nltk import FreqDist

from semafor.scoring.frameparseval import DATE_NAMES
from semafor.utils.wordnet import get_lemma
from semafor.utils.malt_to_conll import ConllToken, ConllFields, read_conll

TRAIN_DATA_DIR = "/Users/sam/repo/project/semafor/semafor/training/data/naacl2012"
GOLD_FILENAME = os.path.join(TRAIN_DATA_DIR, "cv.%s.sentences.json")
DEP_PARSED_FILENAME = os.path.join(TRAIN_DATA_DIR, "cv.%s.sentences.maltparsed.conll")

LEFT_ANCHOR_STR = "^^^^"
RIGHT_ANCHOR_STR = "$$$$"
LEFT_ANCHOR = ConllToken(**{f: LEFT_ANCHOR_STR for f in ConllFields.all_fields()})
RIGHT_ANCHOR = ConllToken(**{f: RIGHT_ANCHOR_STR for f in ConllFields.all_fields()})

TOKEN_JOIN = u'|'.join
INTRATOKEN_JOIN = u':'.join

#FEATURES = ("lemma", "pos", "lemma_pos", "lemma_trigram", "pos_trigram", "lemma_pos_trigram")
#FEATURES = ("prev_lemma", "prev_pos", "lemma", "pos", "next_lemma", "next_pos")
#Features = namedtuple("Features", FEATURES)
#DataPoint = namedtuple("DataPoint", ("is_target", "features"))

def get_non_target_token_idxs(gold_sentence):
    non_target_token_idxs = set()
    # anything in 'wsl' is a non-target
    for entry in gold_sentence['wsl']:
        non_target_token_idxs.update(range(entry['start'], entry['end']))
    # "WEA" (weapons) and "DATE" (dates) are the only named entity types that evoke frames
    for entry in gold_sentence['ner']:
        ner_type = entry['name'].lower()
        if ner_type != 'wea' and not (ner_type == 'date' and entry['text'].lower() in DATE_NAMES):
            non_target_token_idxs.update(range(entry['start'], entry['end']))
    # targets are always targets (overrides 'wsl' or 'ner info)
    for frame in gold_sentence.get('frame', ()):
        for span in frame['spans']:
            non_target_token_idxs.difference_update(range(span['start'], span['end']))
    return non_target_token_idxs


def extract_features(conll_tokens):
    for t in conll_tokens:
        t.lemma = get_lemma(t.form, t.postag)
        t.cpostag = get_coarse_pos(t.postag)
    with_walls = [LEFT_ANCHOR] + conll_tokens + [RIGHT_ANCHOR]
    trigrams = ngrams(with_walls, 3)
    for trigram in trigrams:
        before, this, after = trigram
        yield {
            'lemma_%s' % this.lemma: True,
            'pos_%s' % this.cpostag: True,
            'prev_lemma_%s' % before.lemma: True,
            'prev_pos_%s' % before.cpostag: True,
            'next_lemma_%s' % after.lemma: True,
            'next_pos_%s' % after.cpostag: True,
        }


def extract_gold_data_points(sentence, conll_tokens):
    non_target_token_idxs = get_non_target_token_idxs(sentence)
    is_target = [(i not in non_target_token_idxs) for i in range(len(conll_tokens))]
    features = extract_features(conll_tokens)
    # return [DataPoint(is_target=t, features=f) for t, f in zip(is_target, features)]
    return [dict(f, is_target=t) for t, f in zip(is_target, features)]


def main(gold_sentences, conll_parses, out_file=sys.stdout):
    dps = list(chain(*[extract_gold_data_points(sentence, conll_tokens)
                       for sentence, conll_tokens in zip(gold_sentences, conll_parses)]))
    all_fields = list(chain(*(dp.keys() for dp in dps)))
    #counts = FreqDist(all_fields)
    dict_writer = DictWriter(out_file, all_fields, delimiter="\t")
    dict_writer.writeheader()
    for dp in dps:
        dict_writer.writerow(dp)


if __name__ == "__main__":
    split_name = sys.argv[1]
    assert(split_name in ('train', 'dev', 'test'))
    with codecs.open(DEP_PARSED_FILENAME % split_name, encoding='utf8') as dep_file:
        conll_parses = list(read_conll(dep_file))
    with codecs.open(GOLD_FILENAME % split_name, encoding='utf8') as gold_file:
        gold_sentences = [json.loads(line) for line in gold_file if line]
    main(gold_sentences, conll_parses)
