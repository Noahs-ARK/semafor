#!/usr/bin/env python
import codecs
import re
import sys
import nltk

sentence_tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')


def main(in_filename, out_filename):
    with codecs.open(in_filename, encoding='utf8') as in_file, \
            codecs.open(out_filename, 'w', encoding='utf8') as out_file:
        data = in_file.read()
        for sentence in sentence_tokenizer.tokenize(data, realign_boundaries=True):
            if sentence.strip():
                sentence = re.sub("''", " ''  ", sentence)
                sentence = re.sub("``", " `` ", sentence)
                out_file.write(sentence.strip() + u'\n')


if __name__ == "__main__":
    in_filename = sys.argv[1]
    out_filename = sys.argv[2]
    main(in_filename, out_filename)
