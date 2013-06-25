#!/usr/bin/env python
import sys
from semafor.utils.formats.read_malt import read_malt


def tokenize(line):
    # word/pos/idx_of_parent/dep_label -> word
    tokens = [malt_token.form for malt_token in line]
    return ' '.join(tokens)


if __name__ == "__main__":
    lines = sys.stdin
    for line in lines:
        tokenized = tokenize(read_malt(line.decode('utf8')))
        print tokenized.encode('utf8')
