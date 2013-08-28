#!/usr/bin/env python
"""
counts.py

Counts occurrences of features of frame elements for each frame

Author: Sam Thomson (sthomson@cs.cmu.edu)
"""
import codecs
from collections import defaultdict

from itertools import chain
import json
from operator import itemgetter as ig
from nltk import FreqDist
import sys
from semafor.framenet.frames import FrameHierarchy, RelationTypes
from semafor.utils.formats.conll import read_conll
from semafor.utils.utils import get_head, get_coarse_pos

FRAMES_FILENAME = "training/data/naacl2012/cv.train.sentences.json"
CONLL_FILENAME = "training/data/naacl2012/cv.train.sentences.maltparsed.conll"
LOG_FILENAME = "counts_log.txt"
COUNTS_FILENAME = "counts.txt"
MAX_LINES_PER_SECTION = 20

hierarchy = FrameHierarchy.load()
# has_agent_and_patient = [f for f in hierarchy.frames.values()
#                          if any('Patient' in [a.name for a in hierarchy.ancestors(fe) | {fe}]
#                                 for fe in f.frame_elements) and
#                             any('Agent' in [a.name for a in hierarchy.ancestors(fe) | {fe}]
#                                 for fe in f.frame_elements)]


class FrameStats(object):
    def __init__(self):
        self.count = 0
        self.fes = defaultdict(FeStats)  # keyed by frame name


class FeStats(object):
    def __init__(self):
        self.count = 0
        self.heads = defaultdict(FreqDist)
        self.paths = defaultdict(FreqDist)
        self.paths_with_lemma = defaultdict(FreqDist)


def spans_to_idxs(spans):
    return list(chain(*(range(span['start'], span['end']) for span in spans)))


def read_frames(frames_filename, conll_filename):
    with codecs.open(frames_filename, encoding="utf-8") as frames_file:
        sentences = [json.loads(line) for line in frames_file]
    with codecs.open(conll_filename, encoding="utf-8") as conll_file:
        dep_parses = list(read_conll(conll_file, lookup_lemmas=True))
    for (sentence, dep_parse) in zip(sentences, dep_parses):
        sentence['tokens'] = dep_parse
        for frame_dict in sentence['frames']:
            target = frame_dict['target']
            target_token_idxs = spans_to_idxs(target['spans'])
            target['tokens'] = [dep_parse[i] for i in target_token_idxs]
            frame_element_dicts = frame_dict['annotationSets'][0]['frameElements']
            for fe_dict in frame_element_dicts:
                fe_token_idxs = spans_to_idxs(fe_dict['spans'])
                fe_dict['tokens'] = [dep_parse[i] for i in fe_token_idxs]
    return sentences


def get_semantic_head(span_tokens):
    """ Same as get_head, except for '(P NP)' return the 'NP' """
    head = get_head(span_tokens)
    if head is None:
        return None
    if get_coarse_pos(head.postag) not in ("IN", "TO"):
        return head
    children = [t for t in span_tokens if t.head == head.id]
    if len(children) == 1:
        return children[0]
    return head


def get_ancestors(t, tokens):
    ancestors = [t]
    while ancestors[-1].head > 0:
        ancestors.append(tokens[ancestors[-1].head - 1])
    return ancestors


def get_cpos(conll_token):
    cpostag = get_coarse_pos(conll_token.postag)
    if cpostag not in ("IN", "TO"):
        return cpostag
    return conll_token.lemma + "." + cpostag


def get_path(source, dest, tokens):
    source_ancestors = get_ancestors(source, tokens)
    dest_ancestors = get_ancestors(dest, tokens)
    uniq_source = ["{0}<-[{1}]--".format(get_cpos(x), x.deprel)
                   for x in source_ancestors
                   if x not in dest_ancestors]
    uniq_dest = ["--[{0}]->{1}".format(x.deprel, get_cpos(x))
                 for x in dest_ancestors
                 if x not in source_ancestors]
    if len(uniq_source) == len(source_ancestors):
        return "NO_PATH"
    common = source_ancestors[len(uniq_source)]
    common_pos = get_cpos(common)
    uniq_source_with_lemmas = [
        "{0}<-[{1}]--".format(x.lemma + "." + get_coarse_pos(x.postag), x.deprel)
        for x in source_ancestors
        if x not in dest_ancestors
    ]
    uniq_dest_with_lemmas = [
        "--[{0}]->{1}".format(x.deprel, x.lemma + "." + get_coarse_pos(x.postag))
        for x in dest_ancestors
        if x not in source_ancestors
    ]
    common_with_lemma = common.lemma + "." + get_coarse_pos(common.postag)
    result = "".join(uniq_source + [common_pos] + list(reversed(uniq_dest)))
    result_with_lemmas = "".join(uniq_source_with_lemmas +
                                 [common_with_lemma] +
                                 list(reversed(uniq_dest_with_lemmas)))
    return result, result_with_lemmas


def to_dict(obj):
    if isinstance(obj, dict):
        return {to_dict(key): to_dict(val) for key, val in obj.iteritems()}
    if isinstance(obj, (FeStats, FrameStats)):
        return to_dict(obj.__dict__)
    return obj


def counts_by_frame(sentences, out=sys.stdout):
    stats = defaultdict(FrameStats)
    for sentence in sentences:
        dep_parse = sentence['tokens']
        out.write(" ".join(t.form for t in dep_parse) + "\n")
        for frame_dict in sentence['frames']:
            target = frame_dict['target']
            frame_name = target['name']
            if frame_name not in hierarchy.frames:
                continue
            frame = hierarchy.frames[frame_name]
            ancestors = hierarchy.ancestors(frame)
            target_tokens = target['tokens']
            target_head = get_head(target_tokens)
            cpostag = get_coarse_pos(target_head.postag)
            lu = "_".join(t.lemma for t in target_tokens) + "." + cpostag
            out.write("\t{0: <28}\t{1} ({2})\n".format(lu, frame_name, ", ".join(a.name for a in ancestors)))
            for f in ancestors | {frame}:
                # stats[f.name].lus.inc(lu)
                stats[f.name].count += 1
            for fe_dict in frame_dict['annotationSets'][0]['frameElements']:
                fe_name = fe_dict['name']
                if fe_name not in [x.name for x in frame.frame_elements]:
                    sys.stderr.write("fe_name not in frame.frame_elements!: {0} {1}\n".format(fe_name, frame.name))
                    continue
                frame_element = [x for x in frame.frame_elements if x.name == fe_name][0]
                fe_ancestors = hierarchy.ancestors(frame_element)
                # fe_head = get_head(fe_dict['tokens'])
                fe_head = get_semantic_head(fe_dict['tokens'])
                path, with_lemmas = get_path(target_head, fe_head, dep_parse)
                fe_cpostag = get_coarse_pos(fe_head.postag)
                fe_lemma_and_pos = fe_head.lemma + "." + fe_cpostag
                for fe in fe_ancestors | {frame_element}:
                    ancestor_frame = hierarchy.parents(fe, RelationTypes.FRAME_ELEMENT)[0]
                    fe_stats = stats[ancestor_frame.name].fes[fe.name]
                    fe_stats.count += 1
                    fe_stats.heads[cpostag].inc(fe_lemma_and_pos)
                    fe_stats.paths[cpostag].inc(path)
                    fe_stats.paths_with_lemma[cpostag].inc(with_lemmas)
                out.write("\t\t{0: <24}\t{1: <24}\t{2}\t{3}\n".format(fe_name, fe_lemma_and_pos, path, with_lemmas))
        out.write("\n")
    return stats


def print_stats(stats, out=sys.stdout):
    for frame, frame_stats in sorted(stats.items(), key=lambda (k, v): v.count, reverse=True):
        out.write("{0}\t{1}\n".format(frame, frame_stats.count))
        # out.write("\tLexical Units:\n")
        # for lu, count in sorted(frame_stats.lus.items(), key=ig(1), reverse=True):
        #     out.write("\t\t%s\t%s\n" % (lu, count))
        out.write("\tFrame Elements:\n")
        for fe, fe_stats in sorted(frame_stats.fes.items(), key=lambda (k, v): v.count, reverse=True):
            out.write("\t\t%s\t%s\n" % (fe, fe_stats.count))
            out.write("\t\t\tHeads:" + "\n")
            for cpostag, counts in sorted(fe_stats.heads.items(), key=lambda (k, v): v.N(), reverse=True)[:3]:
                out.write("\t\t\t\t%s:\n" % cpostag)
                for head, count in counts.items()[:MAX_LINES_PER_SECTION]:
                    out.write("\t\t\t\t\t%s\t%s\n" % (head, count))
                if len(counts) > MAX_LINES_PER_SECTION:
                    out.write("\t\t\t\t\t...\n")
            if len(fe_stats.heads) > MAX_LINES_PER_SECTION:
                out.write("\t\t\t\t...\n")
            out.write("\t\t\tPaths:" + "\n")
            for cpostag, counts in sorted(fe_stats.paths.items(), key=lambda (k, v): v.N(), reverse=True)[:3]:
                out.write("\t\t\t\t%s:\n" % cpostag)
                for path, count in counts.items()[:MAX_LINES_PER_SECTION]:
                    out.write("\t\t\t\t\t%s\t%s\n" % (path, count))
                if len(counts) > MAX_LINES_PER_SECTION:
                    out.write("\t\t\t\t\t...\n")
            if len(fe_stats.paths) > MAX_LINES_PER_SECTION:
                out.write("\t\t\t\t...\n")
            out.write("\t\t\tPaths with lemmas:" + "\n")
            for cpostag, counts in sorted(fe_stats.paths_with_lemma.items(), key=ig(1), reverse=True)[:3]:
                out.write("\t\t\t\t%s:\n" % cpostag)
                for path, count in counts.items()[:MAX_LINES_PER_SECTION]:
                    out.write("\t\t\t\t\t%s\t%s\n" % (path, count))
            if len(fe_stats.paths_with_lemma) > MAX_LINES_PER_SECTION:
                out.write("\t\t\t\t...\n")
        out.write("\n")


# def get_high_precision_paths(sentences):
#     stats = defaultdict(FrameStats)
#     for sentence in sentences:
#         dep_parse = sentence['tokens']
#         for frame_dict in sentence['frames']:
#             target = frame_dict['target']
#             frame_name = target['name']
#             target_tokens = target['tokens']
#             target_head = get_head(target_tokens)
#             lu = "_".join(t.lemma for t in target_tokens) + "." + get_coarse_pos(target_head.postag)
#             stats[frame_name].lus.inc(lu)
#             for frame_element in frame_dict['annotationSets'][0]['frameElements']:
#                 fe_name = frame_element['name']
#                 fe_head = get_head(frame_element['tokens'])
#                 path, with_lemmas = get_path(target_head, fe_head, dep_parse)
#                 fe_lemma_and_pos = fe_head.lemma + "." + get_coarse_pos(fe_head.postag)
#                 fe_stats = stats[frame_name].fes[fe_name]
#                 fe_stats.count += 1
#                 fe_stats.heads.inc(fe_lemma_and_pos)
#                 fe_stats.paths.inc(path)
#                 fe_stats.paths_with_lemma.inc(with_lemmas)
#     return stats


def main():
    sentences = read_frames(FRAMES_FILENAME, CONLL_FILENAME)
    with codecs.open(LOG_FILENAME, "w", encoding="utf-8") as log_file:
        stats = counts_by_frame(sentences, out=log_file)
    with codecs.open(COUNTS_FILENAME, "w", encoding="utf-8") as out_file:
        print_stats(stats, out=out_file)


if __name__ == "__main__":
    main()
