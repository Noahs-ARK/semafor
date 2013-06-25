#!/usr/bin/env python
"""
conll.py

Converts from MaltParser's output to CoNLL format

Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
import codecs
import os
from semafor.settings import TRAINING_DATA_DIR
from semafor.utils.wordnet import get_lemma


# Specification of the CoNLL format
# (from http://ilk.uvt.nl/conll/ )
class ConllFields(object):
    # Token counter, starting at 1 for each new sentence
    id = 0
    # Word form or punctuation symbol
    form = 1
    # Lemma or stem (depending on particular data set) of word form,
    # or an underscore if not available
    lemma = 2
    # Coarse-grained part-of-speech tag, where tagset depends on the language.
    cpostag = 3
    # Fine-grained part-of-speech tag, where the tagset depends on the language,
    # or identical to the coarse-grained part-of-speech tag if not available
    postag = 4
    # Unordered set of syntactic and/or morphological features (depending on the
    # particular language), separated by a vertical bar (|), or an underscore
    # if not available
    feats = 5
    # Head of the current token, which is either a value of ID or zero ('0').
    # Note that depending on the original treebank annotation, there may be
    # multiple tokens with an ID of zero
    head = 6
    # Dependency relation to the HEAD. The set of dependency relations depends
    # on the particular language. Note that depending on the original treebank
    # annotation, the dependency relation may be meaningfull or simply 'ROOT'
    deprel = 7
    # Projective head of current token, which is either a value of ID or zero
    # ('0'), or an underscore if not available. Note that depending on the
    # original treebank annotation, there may be multiple tokens an with ID of
    # zero. The dependency structure resulting from the PHEAD column is
    # guaranteed to be projective (but is not available for all languages),
    # whereas the structures resulting from the HEAD column will be
    # non-projective for some sentences of some languages (but is always
    # available)
    phead = 8
    # Dependency relation to the PHEAD, or an underscore if not available. The
    # set of dependency relations depends on the particular language. Note that
    # depending on the original treebank annotation, the dependency relation may
    # be meaningfull or simply 'ROOT'
    pdeprel = 9

    @staticmethod
    def all_fields():
        fields = [(idx, name)
                  for name, idx in ConllFields.__dict__.items()
                  if isinstance(idx, int)]
        return [name for idx, name in sorted(fields)]


def blank_to_none(field):
    return field if field not in ("_", "-") else None


class ConllToken(object):
    def __init__(self, id, form, lemma=None, cpostag=None, postag=None,
                 feats=None, head=None, deprel=None, phead=None, pdeprel=None):
        self.id = int(id)
        self.form = form
        self.lemma = lemma
        self.cpostag = cpostag
        self.postag = postag
        self.feats = feats
        self.head = int(head) if head is not None else head
        self.deprel = deprel
        self.phead = int(phead) if phead is not None else phead
        self.pdeprel = pdeprel

    def zero_indexed(self):
        """ Convert indices to zero-based instead of one-based """
        return ConllToken(
            self.id - 1,
            self.form,
            self.lemma,
            self.cpostag,
            self.postag,
            self.feats,
            self.head - 1 if self.head is not None else None,
            self.deprel,
            self.phead - 1 if self.phead is not None else None,
            self.pdeprel
        )

    @staticmethod
    def from_line(line):
        parts = line.split(u'\t')
        return ConllToken(id=parts[ConllFields.id],
                          form=parts[ConllFields.form],
                          lemma=blank_to_none(parts[ConllFields.lemma]),
                          cpostag=blank_to_none(parts[ConllFields.cpostag]),
                          postag=blank_to_none(parts[ConllFields.postag]),
                          feats=blank_to_none(parts[ConllFields.feats]),
                          head=blank_to_none(parts[ConllFields.head]),
                          deprel=blank_to_none(parts[ConllFields.deprel]),
                          phead=blank_to_none(parts[ConllFields.phead]),
                          pdeprel=blank_to_none(parts[ConllFields.pdeprel]))

    def __unicode__(self):
        fields = [getattr(self, name) for name in ConllFields.all_fields()]
        return u"\t".join(u"_" if f is None else unicode(f) for f in fields)

    def __repr__(self):
        fields = [(name, getattr(self, name)) for name in ConllFields.all_fields()]
        return u"ConllToken(%s)" % u', '.join("%s=%s" % (x, y)
                                              for (x, y) in fields
                                              if y is not None)


def read_conll(lines, lookup_lemmas=False):
    """
    If no lemma is present and lookup_lemmas is True, consults WordNet by
    calling get_lemma().
    """
    result = []
    for line in lines:
        line = line.strip()
        if line == '':
            yield result
            result = []
        else:
            token = ConllToken.from_line(line)
            if lookup_lemmas:
                token.lemma = get_lemma(token.form, token.postag)
            result.append(token)
    if result:
        yield result


def add_lemmas(in_file, out_file):
    conll_parses = read_conll(in_file, lookup_lemmas=True)
    for parse in conll_parses:
        for token in parse:
            out_file.write(unicode(token) + u"\n")
        out_file.write(u"\n")


if __name__ == "__main__":
    for split_name in ("train", "dev", "test"):
        in_filename = os.path.join(TRAINING_DATA_DIR, "naacl2012/cv.%s.sentences.turboparsed.conll") % split_name
        out_filename = os.path.join(TRAINING_DATA_DIR, "naacl2012/cv.%s.sentences.turboparsed.lemmatized.conll") % split_name
        with codecs.open(in_filename, encoding="utf8") as in_file, \
                codecs.open(out_filename, "w", encoding="utf8") as out_file:
            add_lemmas(in_file, out_file)
