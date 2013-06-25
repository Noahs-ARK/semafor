#!/usr/bin/env python
from collections import namedtuple

MaltToken = namedtuple('MaltToken', 'form postag head deprel')


def read_malt(line):
    # last field is a useless number
    line = line.strip().split(u'\t')[0]
    line = line.split(u" ")
    # word/pos/idx_of_parent/dep_label
    tokens = [MaltToken(*word_pos_parent_label.split(u'/'))
              for word_pos_parent_label in line]
    return tokens
