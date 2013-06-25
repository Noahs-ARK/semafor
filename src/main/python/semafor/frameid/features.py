#!/usr/bin/env python2.7
"""
Extracts features for each target

@author Sam Thomson (sthomson@cs.cmu.edu)
"""
from __future__ import print_function, division
import codecs
from itertools import chain
import json
import os
import sys

from nltk.probability import FreqDist

from semafor.settings import TRAINING_DATA_DIR
from semafor.utils.formats.conll import read_conll


GOLD_FILENAME = os.path.join(TRAINING_DATA_DIR, "naacl2012/cv.%s.sentences.json")
DEP_PARSED_FILENAME = os.path.join(TRAINING_DATA_DIR, "naacl2012/cv.%s.sentences.turboparsed.lemmatized.conll")

TOKEN_JOIN = u'|'.join
INTRA_TOKEN_JOIN = u'_'.join


def get_children(sentence, head):
    return [t for t in sentence if t.head == head.id]


def extract_features(sentence, target):
    features = FreqDist()
    # convert to 0-indexed
    sentence = [t.zero_indexed() for t in sentence]

    # features for each word in the sentence
    for token in sentence:
        features.inc(u"sTP:%s" % INTRA_TOKEN_JOIN((token.form, token.cpostag)))
        features.inc(u"sLP:%s" % INTRA_TOKEN_JOIN((token.lemma, token.cpostag)))

    # features for each word in the target
    for token in target:
        features.inc(u"tTP:%s" % INTRA_TOKEN_JOIN((token.form, token.cpostag)))
        features.inc(u"tLP:%s" % INTRA_TOKEN_JOIN((token.lemma, token.cpostag)))

    # syntactic features
    head = get_heuristic_head(target)
    children = get_children(sentence, head)
    subcat = [child.deprel.upper() for child in children]
    # unordered set of arc labels of children
    features.inc(u"d:%s" % TOKEN_JOIN(sorted(set(subcat))))
    # ordered list of arc labels of children
    if head.cpostag == "V":
        # TODO(smt): why exclude "sub"?
        subcat = [deprel for deprel in subcat
                  if deprel != "SUB" and deprel != "P" and deprel != "CC"]
        features.inc(u"sC:%s" % TOKEN_JOIN(subcat))

    if head.head < len(sentence):
        parent = sentence[head.head]
        features.inc(u"pP:%s" % parent.cpostag.upper())
    else:
        features.inc(u"pP:NULL")
    features.inc(u"pL:%s" % head.deprel.upper())

    return features


def get_heuristic_head(span):
    if len(span) == 1:
        return span[0]
    span_idxs = set(t.id for t in span)
    tokens_with_external_parents = [t for t in span if t.head not in span_idxs]
    first_token = tokens_with_external_parents[0]
    if first_token.postag.upper().startswith("V"):
        return first_token
    last_token = tokens_with_external_parents[-1]
    if last_token.postag.upper().startswith("J"):
        return last_token
    if first_token.postag.upper().startswith("N") and "of" in [t.form for t in span]:
        return first_token
    return last_token


def extract_gold_data_points(sentence_dict, conll_tokens):
    for frame in sentence_dict['frames']:
        target = frame['target']
        frame_name = target['name']
        span_dict = target['spans'][0]
        target_tokens = conll_tokens[span_dict['start']:span_dict['end']]
        features = extract_features(conll_tokens, target_tokens)
        yield dict(features, frame=frame_name)


def main(gold_sentences, conll_parses, out_file=sys.stdout):
    dps = chain(*(extract_gold_data_points(sentence, conll_tokens)
                  for sentence, conll_tokens in zip(gold_sentences, conll_parses)))
    # all_fields = set(chain(*(dp.keys() for dp in dps)))
    # dict_writer = DictWriter(out_file, all_fields, delimiter="\t")
    # dict_writer.writeheader()
    # for dp in dps:
    #     dict_writer.writerow(dp)
    # write features in sparse triple format
    out_file.write(u"target_id\tfeature_name\tvalue\n")
    for i, dp in enumerate(dps):
        for feature_name, value in dp.items():
            out_file.write(u"%s\t%s\t%s\n" % (i, feature_name, value))


if __name__ == "__main__":
    split_name = sys.argv[1]
    out_filename = sys.argv[2]
    assert (split_name in ('train', 'dev', 'test'))
    with codecs.open(DEP_PARSED_FILENAME % split_name, encoding='utf8') as dep_file:
        conll_parses = list(read_conll(dep_file, lookup_lemmas=True))
    with codecs.open(GOLD_FILENAME % split_name, encoding='utf8') as gold_file:
        gold_sentences = [json.loads(line) for line in gold_file if line]
    with codecs.open(out_filename, 'w', encoding='utf8') as out_file:
        main(gold_sentences, conll_parses, out_file=out_file)
