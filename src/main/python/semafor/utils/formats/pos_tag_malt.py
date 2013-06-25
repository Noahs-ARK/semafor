#!/usr/bin/env python
import sys
from semafor.utils.read_malt import read_malt


def pos_tag(line):
    # word/pos/idx_of_parent/dep_label -> word_pos
    tags = [u"%s_%s" % (malt_token.form, malt_token.postag)
            for malt_token in line]
    return u' '.join(tags)


if __name__ == "__main__":
    for line in sys.stdin:
        pos = pos_tag(read_malt(line.decode('utf8')))
        print pos.encode('utf8')
