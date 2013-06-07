#!/usr/bin/env python2.7
"""
Translation of SEMAFOR 3.0's edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter.java into Python.
This uses a whitelist of n-grams extracted from FrameNet gold frame annotations, 
and filters out some of the unigrams according to heuristics.

Argument: path to CoNLL-format file for the input

Legacy output format (default): tab-separated lines like

9_10#true    1#true    11#true    13#true    15#true    0

(underscore-separated token IDs for each target; last column is sentence ID)

Pass -j to produce JSON output.

@author: Nathan Schneider (nschneid@cs.cmu.edu)
@since: 2013-05-25
"""

from __future__ import print_function, division
import codecs
from collections import Counter
import json
import os
import sys

from semafor.formats.malt_to_conll import read_conll
from semafor.targetid.target_scanner import build_target_dicts


TARGETDICT_PATH = os.path.dirname(os.path.abspath(__file__)) + '/target_dict.train.txt'
UNIDICT_PATH = os.path.dirname(os.path.abspath(__file__)) + '/unigram_dict.train.txt'
if not os.path.isfile(TARGETDICT_PATH) or not os.path.isfile(UNIDICT_PATH):
    build_target_dicts(TARGETDICT_PATH, UNIDICT_PATH)

TARGETDICT = Counter()  # dictionary of targets (n-grams) and their counts
POSTARGETDICT = Counter()
with codecs.open(TARGETDICT_PATH, 'r', 'utf-8') as targetdictF:
    for ln in targetdictF:
        ww, targetcount = ln[:-1].split('\t')
        TARGETDICT[ww] = int(targetcount)
        if ' ' not in ww:   # unigram target
            POSTARGETDICT[ww[-1]] += int(targetcount)

UNIDICT = Counter() # dictionary of unigrams and their frequencies
POSDICT = Counter() # POS tag frequencies
with codecs.open(UNIDICT_PATH, 'r', 'utf-8') as unidictF:
    for ln in unidictF:
        w, freq = ln[:-1].split('\t')
        UNIDICT[w] = int(freq)    # relative frequency
        POSDICT[w[-1]] += int(freq)

MAX_LEN = 4 # the maximum length of ngrams we'll look for

FORBIDDEN_WORDS = frozenset('a an as for i it so the with'.split())

# if these words precede "of", "of" should not be discarded
PRECEDING_WORDS_OF = frozenset('''%
                                all
                                face
                                few
                                half
                                majority
                                many
                                member
                                minority
                                more
                                most
                                much
                                none
                                one
                                only
                                part
                                proportion
                                quarter
                                share
                                some
                                third'''.split())

# if these words follow "of", "of" should not be discarded
FOLLOWING_WORDS_OF = frozenset('all group their them us'.split())

# all prepositions should be discarded
LOC_PREPS = frozenset('above against at below beside by in on over under'.split())

TEMPORAL_PREPS = frozenset('after before'.split())
DIR_PREPS = frozenset('into through to'.split())
FORBIDDEN_POS_PREFIXES = frozenset(["PR", "CC", "IN", "TO"])


def get_coarse_pos(pos):
    return pos[0]   # this seems to be what was in the whitelist


def get_segmentation(sentence):
    numTokens = len(sentence)

    # start indices that we haven't used yet
    remainingStartIndices = set(range(numTokens))

    lemmas = [token.lemma + "_" + get_coarse_pos(token.postag.upper()) for token in sentence]

    # look for ngrams, backing off to smaller n
    for n in range(MAX_LEN, 0, -1):
        for start in range(numTokens - n + 1):
            if start not in remainingStartIndices:
                continue
            end = start + n
            ngramSpan = slice(start, end)
            ngramLemmas = ' '.join(lemmas[start:end])
            if n == 1 or ngramLemmas in TARGETDICT:
                # found a good ngram, add it to results, and remove it from startIndices so we don't overlap later
                candidate_target = sentence[ngramSpan]
                #if shouldIncludeToken(candidate_target, sentence):
                yes = False

                if n > 1 and TARGETDICT[ngramLemmas] >= 1:
                    yes = True
                elif n == 1 and (TARGETDICT[ngramLemmas] >= 5 or UNIDICT[ngramLemmas] >= 10):
                    # for unigrams, decide based on TARGETDICT vs. UNIDICT counts
                    if ((TARGETDICT[ngramLemmas] / (UNIDICT[ngramLemmas] or 1) >= 0.5 and
                            not sentence[start].postag.upper().startswith('NNP'))):
                        yes = True
                elif n == 1:
                    # decide based on POSTARGETDICT vs. POSDICT counts
                    pos = sentence[start].postag.upper()
                    if not pos.startswith('NNP'):  # not a proper noun
                        cpos = ngramLemmas[-1]
                        if cpos in POSDICT and POSTARGETDICT[cpos] / POSDICT[cpos] >= 0.5:
                            yes = True

                # special cases

                # auxiliaries
                if n == 1 and ngramLemmas[:-1] == "will_":
                    yes = (ngramLemmas != 'will_M')
                #elif n==1 and sentence[start].postag.upper()=='MD':
                #    yes = True  # modals other than 'will'
                #    # including these modals hurts more than it helps, probably due to inconsistent annotations
                elif n == 1 and ngramLemmas[:-1] == "have_":
                    #if any(1 for t in sentence if t.lemma=='have') and any(1 for t in sentence if t.lemma=='question'):
                    #    assert False,sentence
                    yes = any(1 for t in sentence if t.head == start + 1 and t.deprel.upper() in (
                        'OBJ', 'DOBJ')) # 'have' is a target iff it has an object
                    #elif n==1 and ngramLemmas[:-1]=="of_":
                #    prevToken, nextToken = sentence[start-1], sentence[start+1]
                #    if prevToken.lemma in PRECEDING_WORDS_OF: yes = True
                #    elif nextToken.form.lower() in FOLLOWING_WORDS_OF: yes = True
                #    elif prevToken.postag[:2].upper() in ("JJ", "CD"): yes = True
                #    elif nextToken.postag[:2].upper()=="CD": yes = True
                #    elif ' '.join(t.postag for t in sentence[start+1:start+3]).upper()=="DT CD": yes = True
                #    else: yes = False
                #    #return nextToken.ne.startswith("GPE") or nextToken.ne.startswith("LOCATION") or nextToken.ne.startswith("CARDINAL")
                #    # including certain 'of' uses hurts more than it helps, probably due to inconsistent annotations


                if yes:
                    # further filtering
                    if n == 1:
                        if tuple(map(lambda x: x.lemma, sentence[start:start + 2])) in [('of', 'course'),
                                                                                        ('in', 'particular')]:
                            yes = False
                        if tuple(map(lambda x: x.lemma, sentence[start - 1:start + 1])) in [('of', 'course'),
                                                                                            ('in', 'particular')]:
                            yes = False

                if yes:
                    yield candidate_target
                    remainingStartIndices -= set(range(ngramSpan.start, ngramSpan.stop))


def shouldIncludeToken(candidate_target_tokens, sentence):
    """
    Determines whether an ngram should be kept as a target or discarded based on hand-built rules.
    """
    # always include ngrams, n > 1
    if len(candidate_target_tokens) > 1:
        return True

    token = candidate_target_tokens[0]
    idx = token.id - 1    # token ID is 1-based
    assert token is sentence[idx]
    # look up the word in pData
    form = token.form.lower()
    pos = token.postag
    lemma = token.lemma
    # look up the preceding and following words in pData, if they exist
    prevToken = sentence[idx - 1] if idx - 1 >= 0 else None
    nextToken = sentence[idx + 1] if idx + 1 < len(sentence) else None

    if form in FORBIDDEN_WORDS:
        return False
    if form in LOC_PREPS:
        return False
    if form in DIR_PREPS:
        return False
    if form in TEMPORAL_PREPS:
        return False
    if pos[:2] in FORBIDDEN_POS_PREFIXES:
        return False
    # skip "of course" and "in particular"
    if prevToken and (prevToken.form.lower(), form) in [('of', 'course'), ('in', 'particular')]:
        return False

    if form == "of":
        if prevToken.lemma in PRECEDING_WORDS_OF:
            return True
        if nextToken.form.lower() in FOLLOWING_WORDS_OF:
            return True
        if prevToken.postag[:2] in ("JJ", "CD"): return True
        if nextToken.postag[:2] == "CD":
            return True
        if nextToken.postag[:2] == "DT":
            if idx + 2 < len(sentence):
                nextNextToken = sentence[idx + 2]
                if nextNextToken.postag[:2] == "CD":
                    return True
        return nextToken.ne.startswith("GPE") or nextToken.ne.startswith("LOCATION") or nextToken.ne.startswith(
            "CARDINAL")

    if form == "will":
        return pos != "MD"
    if lemma == "have":
        # 'have' is a target iff it has an object
        return any(1 for t in sentence if t.head == token.id and t.deprel == 'OBJ')
    return lemma != "be"


def main(fileP, output_format='legacy' or 'json'):
    with codecs.open(fileP, 'r', 'utf-8') as inF:
        for sentId, sentence in enumerate(read_conll(inF, lookup_lemmas=True)):
            assert None not in sentence

            targets = list(get_segmentation(sentence))

            if output_format.lower() == 'json':
                sentJ = {"tokens": [tkn.form for tkn in sentence],
                         "frames": []}
                for target in targets:
                    spansJ = []
                    for tkn in sorted(target, key=lambda x: x.id):
                        # group contiguous tokens into spans
                        if spansJ and spansJ[-1]["end"] == tkn.id:
                            spansJ[-1]["end"] += 1
                            spansJ[-1]["text"] += ' ' + tkn.form
                        else:
                            spansJ.append({"start": tkn.id - 1, "end": tkn.id, "text": tkn.form})
                    sentJ["frames"].append({"target": {"spans": spansJ}})
                print(json.dumps(sentJ))
            else:
                # output format: 9_10#true    1#true    11#true    13#true    15#true    0
                # underscore-separated token IDs for each target; last column is sentence ID
                # (if no targets, will simply be a tab followed by sentence number)
                print('\t'.join('_'.join(str(token.id - 1) for token in target_tokens) + '#true' for target_tokens in
                                targets) + '\t' + str(sentId))


if __name__ == '__main__':
    args = sys.argv[1:]
    fmt = 'legacy'
    if args[0] == '-j':
        fmt = 'json'
        args.pop(0)
    fileP, _ = args + [None]
    main(fileP, output_format=fmt)
