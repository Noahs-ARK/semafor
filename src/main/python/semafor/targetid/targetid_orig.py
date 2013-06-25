"""
Translation of SEMAFOR 3.0's edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter.java into Python.
This uses a whitelist of n-grams extracted from FrameNet gold frame annotations, 
and filters out some of the unigrams according to heuristics.

Argument: path to CoNLL-format file for the input

Legacy output format (default): tab-separated lines like

9_10#true    1#true    11#true    13#true    15#true    0

(underscore-spearated token IDs for each target; last column is sentence ID)

Pass -j to produce JSON output.

We observe a couple of differences from SEMAFOR's RoteSegmenter:
1) In SEMAFOR 3.0, lemmas were not lowercased before being looked up in the whitelist, 
   so some additional targets are predicted here. This is now fixed in the Java codebase.
2) The Java WordNet library (JWNL) performs aggressive simplification of words 
   during lemmatization, for example stripping out prefixes/suffixes separated by a 
   hyphen or period until it finds a dictionary match. More of those lemmas will be 
   found in the whitelist than when using NLTK's morphy, which only removes 
   inflectional morphology. (For instance, "scud-B missile" matches whitelist entry 
   "scud_N missle_N" with the Java implementation, whereas here only "missle" is 
   marked as a target.) Note that we are using the same whitelist file, which was 
   presumably created with the JWNL lemmatization.

@author: Nathan Schneider (nschneid@cs.cmu.edu)
@since: 2013-05-22
"""

from __future__ import print_function
import sys, os, codecs, json

from semafor.utils.malt_to_conll import read_conll

def get_coarse_pos(pos):
    return pos[0]   # this seems to be what was in the whitelist

with codecs.open(os.path.dirname(os.path.abspath(__file__))+'/semafor3.0_targets_whitelist.txt', 'r', 'utf-8') as whitelistF:
    WHITELIST = frozenset(whitelistF.read().splitlines())

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


def get_segmentation(sentence):
    numTokens = len(sentence)
    
    # start indices that we haven't used yet
    remainingStartIndices = set(range(numTokens))
    
    lemmas = [token.lemma + "_" + get_coarse_pos(token.postag) for token in sentence]

    # look for ngrams, backing off to smaller n
    for n in range(MAX_LEN, 0, -1):
        for start in range(numTokens-n+1):
            if start not in remainingStartIndices:
                continue
            end = start + n
            ngramSpan = slice(start, end)
            ngramLemmas = ' '.join(lemmas[start:end])
            if ngramLemmas in WHITELIST:
                # found a good ngram, add it to results, and remove it from startIndices so we don't overlap later
                candidate_target = sentence[ngramSpan]
                if shouldIncludeToken(candidate_target, sentence):
                    yield candidate_target
                    remainingStartIndices -= set(range(ngramSpan.start, ngramSpan.stop))


def shouldIncludeToken(candidate_target_tokens, sentence):
    """
    Determines whether an ngram should be kept as a target or discarded based on hand-built rules.
    """
    # always include ngrams, n > 1
    if len(candidate_target_tokens)>1: return True

    token = candidate_target_tokens[0]
    idx = token.id-1    # token ID is 1-based
    assert token is sentence[idx]
    # look up the word in pData
    form = token.form.lower()
    pos = token.postag
    lemma = token.lemma
    # look up the preceding and following words in pData, if they exist
    prevToken = sentence[idx-1] if idx-1>=0 else None
    nextToken = sentence[idx+1] if idx+1<len(sentence) else None

    if form in FORBIDDEN_WORDS: return False
    if form in LOC_PREPS: return False
    if form in DIR_PREPS: return False
    if form in TEMPORAL_PREPS: return False
    if pos[:2] in FORBIDDEN_POS_PREFIXES: return False
    # skip "of course" and "in particular"
    if prevToken and (prevToken.form.lower(), form) in [('of','course'), ('in','particular')]: return False
    
    if form=="of":
        if prevToken.lemma in PRECEDING_WORDS_OF: return True
        if nextToken.form.lower() in FOLLOWING_WORDS_OF: return True
        if prevToken.postag[:2] in ("JJ", "CD"): return True
        if nextToken.postag[:2]=="CD": return True
        if nextToken.postag[:2]=="DT":
            if idx+2<len(sentence):
                nextNextToken = sentence[idx+2]
                if nextNextToken.postag[:2]=="CD": return True
        return nextToken.ne.startswith("GPE") or nextToken.ne.startswith("LOCATION") or nextToken.ne.startswith("CARDINAL")
    
    if form=="will": return pos!="MD"
    if lemma=="have": return any(1 for t in sentence if t.head==token.id and t.deprel=='OBJ') # 'have' is a target iff it has an object
    return lemma!="be"

def main(fileP, output_format='legacy' or 'json'):
    with codecs.open(fileP, 'r', 'utf-8') as inF:
        for sentId, sentence in enumerate(read_conll(inF, lookup_lemmas=True)):
            assert None not in sentence
            
            targets = list(get_segmentation(sentence))
            
            if output_format.lower()=='json':
                sentJ = {"tokens": [tkn.form for tkn in sentence], 
                         "frames": []}
                for target in targets:
                    spansJ = []
                    for tkn in sorted(target, key=lambda x: x.id):
                        # group contiguous tokens into spans
                        if spansJ and spansJ[-1]["end"]==tkn.id:
                            spansJ["end"]+=1
                            spansJ["text"]+= ' ' + tkn.form
                        else:
                            spansJ.append({"start": tkn.id-1, "end": tkn.id, "text": tkn.form})
                    sentJ["frames"].append({"target": {"spans": spansJ}})
                print(json.dumps(sentJ))
            else:
                print('\t'.join('_'.join(str(token.id-1) for token in target_tokens)+'#true' for target_tokens in targets) + '\t' + str(sentId))
            # output format: 9_10#true    1#true    11#true    13#true    15#true    0
            # underscore-spearated token IDs for each target; last column is sentence ID
            # (if no targets, will simply be a tab followed by sentence number)

if __name__=='__main__':
    args = sys.argv[1:]
    fmt = 'legacy'
    if args[0]=='-j':
        fmt = 'json'
        args.pop(0)
    fileP, _ = args+[None]
    main(fileP, output_format=fmt)
