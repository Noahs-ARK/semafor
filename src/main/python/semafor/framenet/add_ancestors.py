#!/usr/bin/env Python2.7
"""
Adds ancestors for every frame element
"""
import codecs
from copy import deepcopy
import json
import sys

from semafor.framenet.frames import load_hierarchy, RelationTypes


def get_frame_for(fe, hierarchy):
    return hierarchy.parents(fe, RelationTypes.FRAME_ELEMENT)[0].name


def add_ancestors(sentence, hierarchy):
    sentence = deepcopy(sentence)
    for frame_dict in sentence['frames']:
        target = frame_dict['target']
        name = target['name']
        frame = hierarchy.frames[name]
        target['ancestors'] = [f.name for f in hierarchy.ancestors(frame)]
        for annotation_set in frame_dict['annotationSets']:
            for frame_element_dict in annotation_set['frameElements']:
                fe_name = frame_element_dict['name']
                frame_element = [fe for fe in frame.frame_elements if fe.name == fe_name][0]
                ancestors = hierarchy.ancestors(frame_element)
                frame_element_dict['ancestors'] = [
                    {
                        'name': fe.name,
                        'frame': get_frame_for(fe, hierarchy)
                    }
                    for fe in ancestors if fe.name != fe_name
                ]
    return sentence


def main(in_filename, out_filename, hierarchy):
    with codecs.open(in_filename, encoding='utf8') as in_file, \
            codecs.open(out_filename, 'w', encoding='utf8') as out_file:
        for line in in_file:
            sentence = json.loads(line)
            with_ancestors = add_ancestors(sentence, hierarchy)
            out_file.write(json.dumps(with_ancestors) + u"\n")


if "__main__" == __name__:
    in_filename = sys.argv[1]
    out_filename = sys.argv[2]
    hierarchy = load_hierarchy()
    main(in_filename, out_filename, hierarchy)
