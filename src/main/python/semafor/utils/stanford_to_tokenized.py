#!/usr/bin/env python
import codecs
import sys
from xml.dom.minidom import parse


def get_tokens(sentence_element):
    token_elements = sentence_element.getElementsByTagName("token")
    return [
        {
            "word": t.getElementsByTagName("word")[0].firstChild.nodeValue,
            "start": int(t.getElementsByTagName("CharacterOffsetBegin")[0].firstChild.nodeValue),
            "end": int(t.getElementsByTagName("CharacterOffsetEnd")[0].firstChild.nodeValue),
        }
        for t in token_elements
    ]


def get_sentences(doc):
    sentence_elements = doc.getElementsByTagName("sentence")
    for sentence in sentence_elements:
        yield get_tokens(sentence)


def main(in_filename, out_filename):
    doc = parse(in_filename)
    with codecs.open(out_filename, 'w', encoding='utf8') as out_file:
        for sentence in get_sentences(doc):
            out_file.write(u" ".join(t["word"] for t in sentence) + u'\n')


if __name__ == "__main__":
    in_filename = sys.argv[1]
    out_filename = sys.argv[2]
    main(in_filename, out_filename)
