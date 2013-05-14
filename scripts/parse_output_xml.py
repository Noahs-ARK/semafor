#!/usr/bin/env python
"""
Parse the xml output of Semafor into json.
The text of each span is added (even though it is technically redundant), to
increase readability.

The json format is:

{
  "tokens": [ "My", "kitchen", "no", "longer", "smells", "." ],
  "frames": [
    {
      "target": {
        "start": 1,
        "end": 2,
        "name": "Building_subparts",
        "text": "kitchen"
      },
      "annotationSets": [
        {
          "frameElements": [
            {
              "start": 1,
              "end": 2,
              "name": "Building_part",
              "text": "kitchen"
            },
            {
              "start": 0,
              "end": 1,
              "name": "Whole",
              "text": "My"
            }
          ],
          "score": 18.21350901550272,
          "rank": 0
        }
      ]
    },
    ...
  ]
}
Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
from itertools import chain
from json import dumps
import sys
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
    target_label = target_layer.getElementsByTagName('label')
    target = parse_label(target_label[0], text) if target_label else {}
    target['name'] = name
    # extract the frame elements
    frame_element_layer = [l for l in layers
                           if l.getAttribute('name') == "FE"][0]
    frame_element_labels = [l for l in frame_element_layer.getElementsByTagName('label')
                            if l.getAttribute("itype").lower() not in ("ini", "dni", "cni", "inc")]
    return {
        "target": target,
        "annotationSets": [{
            "frameElements": [parse_label(fe, text) for fe in frame_element_labels]
        }]
    }


def get_text(sentence_node):
    return sentence_node.getElementsByTagName("text")[0].firstChild.wholeText


def parse_sentence(sentence_elt):
    """ Parses one sentence tag in the xml file """
    # extract the text of the sentence
    text = get_text(sentence_elt)
    # extract the frame annotations
    annotation_sets = sentence_elt.getElementsByTagName('annotationSet')
    frame_sets = [s for s in annotation_sets if s.getAttribute("frameName")]
    layers = sentence_elt.getElementsByTagName("layer")
    wsl = [l.getElementsByTagName("label") for l in layers if "wsl" == l.getAttribute("name").lower()]
    ner = [l.getElementsByTagName("label") for l in layers if "ner" == l.getAttribute("name").lower()]
    pos = [l.getElementsByTagName("label") for l in layers if "pos" == l.getAttribute("name").lower()]
    frames = [parse_annotation_set(annotation_set, text)
              for annotation_set in frame_sets]
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
    dom = parseString(xml_string)
    return [parse_sentence(sentence)
            for sentence in dom.getElementsByTagName('sentence')]


def parse_to_json(xml_string):
    """ Parses the xml output of Semafor into json """
    sentences = parse_to_dicts(xml_string)
    return u'\n'.join(dumps(sentence) for sentence in sentences)


def main(filename):
    """ Parses the xml output of Semafor into json and print it """
    with open(filename) as xml_file:
        xml_text = xml_file.read()
    print parse_to_json(xml_text)


if __name__ == "__main__":
    # reads xml from the given file, and prints the resulting json to stdout
    main(sys.argv[1])
