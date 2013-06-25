"""
Simple, unlexicalized heuristics for target identification.

Argument: path to CoNLL-format file for the input

Legacy output format (default): tab-separated lines like

9_10#true    1#true    11#true    13#true    15#true    0

(underscore-spearated token IDs for each target; last column is sentence ID)

Pass -j to produce JSON output.


@author: Nathan Schneider (nschneid@cs.cmu.edu)
@since: 2013-05-25
"""

from __future__ import print_function
import sys, os, codecs, json

from semafor.utils.malt_to_conll import read_conll

def get_coarse_pos(pos):
    return pos[0]   # this seems to be what was in the whitelist


FORBIDDEN_POS_PREFIXES = frozenset(["PR", "CC", "IN", "TO", "RP", "NNP", "DT", "MD", "POS"])


def get_segmentation(sentence):
    numTokens = len(sentence)
    
    # start indices that we haven't used yet
    remainingStartIndices = set(range(numTokens))
    
    lemmas = [token.lemma + "_" + get_coarse_pos(token.postag) for token in sentence]

    # look for ngrams, backing off to smaller n
    for n in range(1, 0, -1):
        for start in range(numTokens-n+1):
            if start not in remainingStartIndices:
                continue
            end = start + n
            ngramSpan = slice(start, end)
            ngramLemmas = ' '.join(lemmas[start:end])
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
    if not pos.isalnum(): return False
    lemma = token.lemma
    # look up the preceding and following words in pData, if they exist
    prevToken = sentence[idx-1] if idx-1>=0 else None
    nextToken = sentence[idx+1] if idx+1<len(sentence) else None

    if any(pos.startswith(pref) for pref in FORBIDDEN_POS_PREFIXES): return False
    
    #if form=='/':
    #    assert False,pos
    
    # skip "of course" and "in particular"
    if prevToken and (prevToken.form.lower(), form) in [('of','course'), ('in','particular')]: return False
    
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
                        #print(tkn.form, file=sys.stderr)
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
