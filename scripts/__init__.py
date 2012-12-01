#!/usr/bin/env python
import sys


def convert(lines):
    for line in lines:
        # last field is some useless number
        line = line.strip().split()[:-1]
        # word/pos/idx_of_parent/dep_label
        tokens = [word_pos_parent_label.split('/')[0]
                  for word_pos_parent_label in line]
        yield ' '.join(tokens)


if __name__ == "__main__":
    lines = sys.stdin
    for line in convert(lines):
        print line
