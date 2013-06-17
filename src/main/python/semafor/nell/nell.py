#!/usr/bin/env Python2.7
"""
Utilities for looking up types of noun phrases in the NELL KB
"""
import codecs
from collections import OrderedDict

import networkx as nx

from semafor.settings import NELL_HIERARCHY_FILENAME, NELL_NOUN_PHRASES_FILENAME

CONFIDENCE_THRESHOLD = 0.91  # chosen by inspecting data


def load_hierarchy(filename=NELL_HIERARCHY_FILENAME):
    graph = nx.MultiDiGraph()
    with codecs.open(filename, encoding="utf8") as in_file:
        for line in in_file:
            if line.strip():
                cat, parent_str = line.split(u"\t")
                parents = parent_str.strip().split(u" ")
                for parent in parents:
                    if parent:
                        graph.add_edge(cat, parent)
    # # trim indirect ancestors
    # for node in graph.nodes():
    #     grandparents = {grandparent
    #                     for _, parent in graph.out_edges(node)
    #                     for _, grandparent in graph.out_edges(parent)}
    #     for grandparent in grandparents:
    #         graph.remove_edge(node, grandparent)
    return graph


def parse_noun_phrase_line(line,
                           threshold=CONFIDENCE_THRESHOLD,
                           ignore_case=False):
    fields = line.split(u"\t")
    token = fields[0]
    if ignore_case:
        token = token.lower()
    types = [x.split(u" ") for x in fields[1:]]
    types = [(t, float(conf)) for t, conf in types
             if float(conf) > threshold]
    return token, types


def load_noun_types(filename=NELL_NOUN_PHRASES_FILENAME, ignore_case=False):
    with codecs.open(filename, encoding="utf8") as in_file:
        noun_types = (parse_noun_phrase_line(line, ignore_case)
                      for line in in_file)
        noun_types = ((phrase, types)
                      for phrase, types in noun_types
                      if types)
        return OrderedDict(sorted(noun_types))

