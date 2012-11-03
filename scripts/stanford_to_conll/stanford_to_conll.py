#!/usr/bin/env python
"""
stanford_to_conll.py

Converts from Stanford POS Tagger's output to CoNLL format

Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
from __future__ import with_statement
import sys
from collections import namedtuple

# Specification of the CoNLL format
# (from http://ilk.uvt.nl/conll/ ):
CONLL_FIELDS = (
    # Token counter, starting at 1 for each new sentence
    'id',
    # Word form or punctuation symbol
    'form',
    # Lemma or stem (depending on particular data set) of word form,
    # or an underscore if not available
    'lemma',
    # Coarse-grained part-of-speech tag, where tagset depends on the language.
    'cpostag',
    # Fine-grained part-of-speech tag, where the tagset depends on the language,
    # or identical to the coarse-grained part-of-speech tag if not available
    'postag',
    # Unordered set of syntactic and/or morphological features (depending on the
    # particular language), separated by a vertical bar (|), or an underscore
    # if not available
    'feats',
    # Head of the current token, which is either a value of ID or zero ('0').
    # Note that depending on the original treebank annotation, there may be
    # multiple tokens with an ID of zero
    'head',
    # Dependency relation to the HEAD. The set of dependency relations depends
    # on the particular language. Note that depending on the original treebank
    # annotation, the dependency relation may be meaningfull or simply 'ROOT'
    'deprel',
    # Projective head of current token, which is either a value of ID or zero
    # ('0'), or an underscore if not available. Note that depending on the
    # original treebank annotation, there may be multiple tokens an with ID of
    # zero. The dependency structure resulting from the PHEAD column is
    # guaranteed to be projective (but is not available for all languages),
    # whereas the structures resulting from the HEAD column will be
    # non-projective for some sentences of some languages (but is always
    # available)
    'phead',
    # Dependency relation to the PHEAD, or an underscore if not available. The
    # set of dependency relations depends on the particular language. Note that
    # depending on the original treebank annotation, the dependency relation may
    # be meaningfull or simply 'ROOT'
    'pdeprel'
)
ConllToken = namedtuple('Token', ' '.join(CONLL_FIELDS))
StanfordToken = namedtuple('StanfordToken', 'form tag')


def default_conll_token(**kwargs):
    " Creates a new ConllToken, with unspecified fields filled with '_'s "
    defaults = dict((name, '_') for name in CONLL_FIELDS)
    defaults.update(**kwargs)
    return ConllToken(**defaults)


def str_to_stanford(pair):
    """ Converts a "word_token" string into a StanfordToken(word, token) """
    parts = pair.split('_')
    return StanfordToken(form='_'.join(parts[:-1]), tag=parts[-1])


def stanford_to_conll(line):
    " Converts one line of Stanford POS Tagger's output to CoNLL format """
    output = []
    stanford_tokens = (str_to_stanford(pair) for pair in line.split())
    for i, token in enumerate(stanford_tokens):
        conll_token = default_conll_token(
            id=unicode(i + 1),
            form=token.form,
            cpostag=token.tag,
            postag=token.tag)
        output.append(u'\t'.join(field for field in conll_token))
    output.append(u'')
    return u'\n'.join(output)


def main():
    for line in sys.stdin:
        conll = stanford_to_conll(line.decode('utf8'))
        print conll.encode('utf8')


if __name__ == "__main__":
    main()
