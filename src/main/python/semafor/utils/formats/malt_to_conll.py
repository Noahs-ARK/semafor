#!/usr/bin/env python
"""
conll.py

Converts from MaltParser's output to CoNLL format

Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
import sys
from semafor.utils.formats.read_malt import read_malt
from semafor.utils.formats.conll import ConllToken


def malt_to_conll(malt_tokens):
    """Converts one line of MaltParser's output to CoNLL format"""
    output = []
    for i, token in enumerate(malt_tokens):
        conll_token = ConllToken(
            id=i + 1,
            form=token.form,
            cpostag=token.postag,
            postag=token.postag,
            head=token.head,
            deprel=token.deprel)
        output.append(unicode(conll_token))
    output.append(u'')
    return u'\n'.join(output)


def main(lines):
    for line in lines:
        conll = malt_to_conll(read_malt(line.decode('utf8')))
        print conll.encode('utf8')


if __name__ == "__main__":
    main(sys.stdin)
