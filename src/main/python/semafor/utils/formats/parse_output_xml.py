#!/usr/bin/env python
"""
Parse the xml output of Semafor (or full-text annotations) into json.
The text of each span is added (even though it is technically redundant), to
increase readability.

Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
from collections import defaultdict
from itertools import chain
from json import dumps
import sys
import codecs
from xml.dom.minidom import parseString


def convert_char_offset_to_word_offset(text, start, end):
    """ Converts the given character offsets to word offsets """
    start_word = text[:start].count(' ')
    end_word = start_word + text[start:end].count(' ') + 1
    return start_word, end_word


def parse_label(label, text):
    """ Parse a Target or FE label """
    # the name of the Frame or Frame Element
    name = label.getAttribute('name')
    # the character at which the span starts
    start = int(label.getAttribute('start'))
    # the character at which the span ends
    end = int(label.getAttribute('end'))
    # the text of the span, included to increase readability
    span_text = text[start:end+1]
    # convert char offsets to word offsets
    start_word, end_word = convert_char_offset_to_word_offset(text, start, end)
    return {
        "name": name,
        "start": start_word,
        "end": end_word,
        "text": span_text
    }


def parse_annotation_set(annotation_set_elt, text):
    """ Parses annotation of one frame for one sentence """
    # extract the name of the frame
    name = annotation_set_elt.getAttribute('frameName')
    layers = annotation_set_elt.getElementsByTagName('layer')
    # extract the target of the frame
    target_layer = [l for l in layers if l.getAttribute('name') == "Target"][0]
    target_spans = [parse_label(label, text) for label in target_layer.getElementsByTagName('label')]
    for span in target_spans:
        span.pop("name")
    target = {
        'name': name,
        'spans': sorted(target_spans, key=lambda x: x["start"]),
    }
    # extract the frame elements
    frame_element_layers = [l for l in layers
                           if l.getAttribute('name') == "FE" and l.getAttribute('rank') in ("","1")]
    frame_element_labels = [parse_label(l, text) for layer in frame_element_layers for l in layer.getElementsByTagName('label')
                            if l.getAttribute("itype").lower() not in ("ini", "dni", "cni", "inc")]
    frame_elements_by_role = defaultdict(list)
    for label in frame_element_labels:
        frame_elements_by_role[label.pop("name")].append(label)
    return {
        "target": target,
        "annotationSets": [{
            "frameElements": [{
                "name": name,
                "spans": sorted(spans, key=lambda x: x["start"])
            } for name, spans in frame_elements_by_role.items()]
        }]
    }


def get_text(sentence_node):
    return sentence_node.getElementsByTagName("text")[0].firstChild.wholeText


def parse_sentence(sentence_elt, i=None):
    """ Parses one sentence tag in the xml file """
    # extract the text of the sentence
    text = get_text(sentence_elt)
    # extract the frame annotations
    annotation_sets = sentence_elt.getElementsByTagName('annotationSet')
    frame_sets = [s for s in annotation_sets if s.getAttribute("frameName")]
    layers = sentence_elt.getElementsByTagName("layer")
    wsl = [l.getElementsByTagName("label") for l in layers if "wsl" == l.getAttribute("name").lower()]
    ner = [l.getElementsByTagName("label") for l in layers if "ner" == l.getAttribute("name").lower()]
    pos = [l.getElementsByTagName("label") for l in layers if "penn" == l.getAttribute("name").lower()]
    frames = (parse_annotation_set(annotation_set, text)
              for annotation_set in frame_sets)
    frames = [frame for frame in frames if frame['target']['spans']]  # filter out frames with no target
    result = dict(sentence_elt.attributes.items())
    result.update({
        "tokens": text.split(),
        "frames": frames,
        "wsl": [parse_label(l, text) for l in chain(*wsl)],
        "ner": [parse_label(l, text) for l in chain(*ner)],
        "pos": [parse_label(l, text) for l in chain(*pos)],
    })
    return result


def parse_to_dicts(xml_string):
    """ Parses the xml output of Semafor into a dict """
    dom = parseString(xml_string.encode('utf-8'))
    return [parse_sentence(sentence,i)
            for i,sentence in enumerate(dom.getElementsByTagName('sentence'))]


def parse_to_json(xml_string):
    """ Parses the xml output of Semafor into json """
    sentences = parse_to_dicts(xml_string)
    return u'\n'.join(dumps(sentence) for sentence in sentences)


def main(filename):
    """ Parses the xml output of Semafor into json and print it """
    with codecs.open(filename, encoding='utf-8') as xml_file:
        xml_text = xml_file.read()
    print parse_to_json(xml_text)


if __name__ == "__main__":
    # reads xml from the given file, and prints the resulting json to stdout
    main(sys.argv[1])
