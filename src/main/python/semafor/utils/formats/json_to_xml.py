#!/usr/bin/env python
"""
Converts json output to xml

Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
import argparse
import codecs
import json
import re
from xml.dom.minidom import Document


def convert_token_offset_to_char_offset(tokens):
    """ Curried function that converts the given token offsets to character offsets """
    lens = [len(token) for token in tokens]
    # token offsets are 0-indexed, end-exclusive, whereas char offsets are 0-indexed, end-inclusive
    starts = [sum(lens[:i]) + i for i in range(len(tokens))]
    ends = [sum(lens[:i + 1]) + i - 1 for i in range(len(tokens))]

    def offset_converter(start_token, end_token):
        return starts[start_token], ends[end_token - 1]

    return offset_converter


def convert_span(span, name, label_id, offset_converter, doc):
    label = doc.createElement("label")
    start, end = offset_converter(span["start"], span["end"])
    label.setAttribute("ID", str(label_id))
    label.setAttribute("name", name)
    label.setAttribute("start", str(start))
    label.setAttribute("end", str(end))
    label.setAttribute("tokenStart", str(span["start"]))
    label.setAttribute("tokenEnd", str(span["end"]))
    return label


def convert_frame(frame, frame_id, doc, offset_converter):
    """ Converts one frame to an xml Element """
    annotation_set = doc.createElement("annotationSet")
    annotation_set.setAttribute("ID", str(frame_id))
    annotation_set.setAttribute("frameName", frame["target"]["name"])
    layers = doc.createElement("layers")
    annotation_set.appendChild(layers)
    target_layer = doc.createElement("layer")
    target_layer_id = 100 * frame_id + 1
    target_layer.setAttribute("ID", str(target_layer_id))
    target_layer.setAttribute("name", "Target")
    layers.appendChild(target_layer)
    target_labels = doc.createElement("labels")
    target_layer.appendChild(target_labels)
    for span_id, span in enumerate(frame["target"]["spans"]):
        label_id = 100 * target_layer_id + span_id
        label = convert_span(span, "Target", label_id, offset_converter, doc)
        target_labels.appendChild(label)
    fe_layer = doc.createElement("layer")
    fe_layer_id = 100 * frame_id + 2
    fe_layer.setAttribute("ID", str(fe_layer_id))
    fe_layer.setAttribute("name", "FE")
    layers.appendChild(fe_layer)
    fe_labels = doc.createElement("labels")
    fe_layer.appendChild(fe_labels)
    for fe_id, frame_element in enumerate(frame["annotationSets"][0]["frameElements"]):
        for span in frame_element["spans"]:
            label_id = 100 * fe_layer_id + fe_id
            label = convert_span(span, frame_element["name"], label_id, offset_converter, doc)
            fe_labels.appendChild(label)
    return annotation_set


def convert_sentence(sentence_dict, sentence_id, doc):
    """ Converts one sentence to an xml Element """
    sentence_elt = doc.createElement("sentence")
    sentence_elt.setAttribute("ID", str(sentence_id))

    text_node = doc.createElement("text")
    sentence_elt.appendChild(text_node)
    tokens = sentence_dict["tokens"]
    text = doc.createTextNode(u" ".join(tokens))
    text_node.appendChild(text)

    annotation_sets = doc.createElement("annotationSets")
    sentence_elt.appendChild(annotation_sets)

    offset_converter = convert_token_offset_to_char_offset(tokens)
    for i, frame in enumerate(sentence_dict["frames"]):
        frame_id = 100 * sentence_id + i
        annotation_set = convert_frame(frame, frame_id, doc, offset_converter)
        annotation_sets.appendChild(annotation_set)

    return sentence_elt


def parse_to_xml(sentence_dicts):
    """ Parses the json output of Semafor into xml """
    doc = Document()

    corpus = doc.createElement("corpus")
    corpus.setAttribute("name", "ONE")
    doc.appendChild(corpus)

    documents = doc.createElement("documents")
    corpus.appendChild(documents)

    document = doc.createElement("document")
    document.setAttribute("ID", "1")
    document.setAttribute("description", "TWO")
    documents.appendChild(document)

    paragraphs = doc.createElement("paragraphs")
    document.appendChild(paragraphs)

    paragraph = doc.createElement("paragraph")
    paragraph.setAttribute("ID", "2")
    paragraph.setAttribute("documentOrder", "1")
    paragraphs.appendChild(paragraph)

    sentences = doc.createElement("sentences")
    paragraph.appendChild(sentences)

    for i, sentence_dict in enumerate(sentence_dicts):
        sentence = convert_sentence(sentence_dict, i, doc)
        sentences.appendChild(sentence)

    return doc


def main(in_filename, out_filename, tokenized_filename=None):
    """ Parses json output of Semafor into xml and writes it to out_filename """
    with codecs.open(in_filename, encoding="utf8") as json_file:
        sentence_dicts = (json.loads(line) for line in json_file)
        if tokenized_filename:
            def swap_tokens(sentence_dict, tokens):
                sentence_dict["tokens"] = tokens
                return sentence_dict
            with codecs.open(tokenized_filename, encoding="utf8") as tokenized_file:
                tokenized_sentences = (line.split() for line in tokenized_file)
                swapped = (swap_tokens(sd, ts) for sd, ts in zip(sentence_dicts, tokenized_sentences))
                doc = parse_to_xml(swapped)
        else:
            doc = parse_to_xml(sentence_dicts)
    # can't use doc.toprettyxml() because it inserts whitespace into <text> nodes
    # this is an attempt to make it a little more readable by adding newlines in places where it's safe to
    with_newlines = re.sub(r'><', r'>\n<', re.sub(r'(</.*?>)', r'\1\n', doc.toxml()))
    with codecs.open(out_filename, "w", encoding="utf8") as out_file:
        out_file.write(with_newlines)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Reads json output of Semafor from infile, parses it into xml and writes it to outfile. ' +
                    'Optionally replaces the tokens in infile with those from tokenized_file')
    parser.add_argument('infile', help='the input file')
    parser.add_argument('outfile', help='the output file')
    parser.add_argument('--tokenized_file', default=None, help="the tokenized file")
    args = parser.parse_args()
    main(args.infile, args.outfile, args.tokenized_file)
