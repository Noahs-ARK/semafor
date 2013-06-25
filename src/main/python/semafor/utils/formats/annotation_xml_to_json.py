#!/usr/bin/env python
from itertools import chain
from xml.dom.minidom import parse
import os

#FILENAME = "ANC__110CYL067.xml"
DIRECTORY = "/Users/sam/repo/project/semafor/semafor/training/data/framenet15/fulltext"
FILENAMES = [os.path.join(DIRECTORY, fn) for fn in os.listdir(DIRECTORY)]


def get_text(sentence_node):
    return sentence_node.getElementsByTagName("text")[0].firstChild.wholeText


def get_nts_and_targets(sentence_node):
    text = get_text(sentence_node)
    annotation_sets = sentence_node.getElementsByTagName("annotationSet")
    word_status_layer = [l for l in annotation_sets[0].getElementsByTagName("layer")
                         if "WSL" == l.getAttribute("name")][0]
    nts = []
    for label in word_status_layer.getElementsByTagName("label"):
        if "NT" == label.getAttribute("name"):
            start = int(label.getAttribute("start"))
            end = int(label.getAttribute("end"))
            nts.append((start, end, "NT", text[start:end+1]))
    targets = []
    for annotation_set in annotation_sets[1:]:
        target = [l for l in annotation_set.getElementsByTagName("label")
                  if "Target" == l.getAttribute("name")]
        for t in target:
            start = int(t.getAttribute("start"))
            end = int(t.getAttribute("end"))
            targets.append((start, end, "Target", text[start:end+1]))
    return sorted(nts + targets)


def get_unannotated(sentences):
    unannotated = []
    num_targets = 0
    for i, sentence in enumerate(sentences):
        text = get_text(sentence)
        nts_and_targets = get_nts_and_targets(sentence)
        num_targets += len([x for x in nts_and_targets if "Target" == x[2]])
        missing = set(range(len(text)))
        for nt_or_target in nts_and_targets:
            missing = missing.difference(range(nt_or_target[0], nt_or_target[1] + 1))
            #for char_idx in range(nt_or_target[0], nt_or_target[1] + 1):
            #    missing.remove(char_idx)
        missing_str = ''.join(text[c] for c in missing).strip()
        if len(missing_str):
            unannotated.append((i, text, missing, missing_str.split()))
    return unannotated, num_targets


def get_all_unannotated(filenames):
    all_unannotated = []
    num_sentences = 0
    num_total_targets = 0
    for filename in filenames:
        dom = parse(filename)
        sentences = dom.getElementsByTagName("sentence")
        num_sentences += len(sentences)
        unannotated, num_targets = get_unannotated(sentences)
        all_unannotated.extend(unannotated)
        num_total_targets += num_targets
    return all_unannotated, num_sentences, num_total_targets


if "__main__" == __name__:
    unannotated, num_sentences, num_total_targets = get_all_unannotated(FILENAMES)
    print "num sentences:", num_sentences
    print "num sentences with unannotated:", len(unannotated)
    print "num targets:", num_total_targets
    print "num unannotated tokens:", len(list(chain(*[tokens for i, text, missing, tokens in unannotated])))
