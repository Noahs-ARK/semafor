#!/usr/bin/env python2.7
"""
Scorer for frame-semantic parsing output.
Requires two JSON files: the gold analysis, and the predictions.

If k-best argument predictions are present, only considers the top-ranked set.

Requires a list of core frame elements. In labeled argument scoring, only .5 is awarded to anything not in this list (non-core, core unexpressed, etc.).

TODO:
 - Partial credit for frames via the hierarchy, and (if a related frame is found) their frame elements via the hierarchy.

Known differences from fnSemScore_modified.pl:
 - The target identification in the Perl script is based on which spans have a gold frame label. 
   This is not really correct, as there are many spans that ideally would evoke a frame 
   but that are missing an annotation. So we use the Word Status Layer (WSL) 
   and Named Entity Recognition (NER) layers to decide which words ought to count as targets.
   A system will not be penalized for predicting a frame for a word that is theoretically 
   frame-evoking but has no gold label.

 - We do not specially handle frame elements labeled Path. (If there are multiple Path argument spans 
   for a given frame instance, the Perl script lists them separately, though it concatenates 
   the spans for all other roles.)
   
 - The Perl script ignores annotation layers with a "rank" attribute greater than 1.
   We do not check this attribute.

@author: Nathan Schneider (nschneid)
@since: 2013-05-15
"""
from __future__ import print_function, division
import sys, json, codecs, itertools, math
from collections import Counter
from pandas import DataFrame

DATE_NAMES = [
    'monday',
    'tuesday',
    'wednesday',
    'thursday',
    'friday',
    'saturday',
    'sunday',
    'january',
    'february',
    'march',
    'april',
    'may',
    'june',
    'july',
    'august',
    'september',
    'october',
    'november',
    'december'
]
# add all three-letter abbreviations
DATE_NAMES += [v for d in DATE_NAMES for v in [d[:3], d[:3]+'.']]

with codecs.open('fn1.5_fe_core_status.json', 'r', 'utf-8') as inF:
    CORE_STATUS = json.load(inF)
CORE_POINTS = 1.0
OTHER_CORENESS_POINTS = 0.5

class PRCounter(object):
    COLUMNS = ['Numer', 'PDenom', 'RDenom', 'P', 'R', 'F', 'T', 'N', 'Acc']
    COMPUTE_RATIOS_ON_ADD = False   # if False, the division (compute_ratios()) will be deferred in case denominators are initially 0

    def __init__(self):
        self._df = DataFrame(columns=PRCounter.COLUMNS)

    def __setitem__(self, k, v):
        points = {}
        if isinstance(v[0], int):
            N, gold_set, pred_set = v
            if gold_set or pred_set:
                assert N>0,(N,gold_set,pred_set)
        else:
            N = ''
            gold, pred = v
            pred_set = set(pred.keys()) if isinstance(pred, dict) else pred
            gold_set = set(gold.keys()) if isinstance(gold, dict) else gold
            if isinstance(gold, dict):
                points.update(gold)
                if isinstance(pred, dict):
                    for elt in gold_set & pred_set:
                        assert gold[elt]==pred[elt],(elt,gold[elt],pred[elt])
            if isinstance(pred, dict):
                points.update(pred)
            
        entry = {
            'Numer': sum(points.get(elt,1) for elt in gold_set & pred_set),
            'PDenom': sum(points.get(elt,1) for elt in pred_set),
            'RDenom': sum(points.get(elt,1) for elt in gold_set),
            'N': N
        }
        entry['P'] = entry['Numer'] / entry['PDenom'] if entry['PDenom'] else float('nan')
        entry['R'] = entry['Numer'] / entry['RDenom'] if entry['RDenom'] else float('nan')
        entry['F'] = 2 * entry['P'] * entry['R'] / (entry['P'] + entry['R']) if (entry['P'] + entry['R']) else float('nan')
        if N=='':
            entry['T'] = None
            entry['Acc'] = None
        else:
            if len(gold_set)==len(pred_set)==N:
                entry['T'] = entry['Numer']
            else:
                tp = entry['Numer']
                fp = len(pred_set-gold_set)
                fn = len(gold_set-pred_set)
                entry['T'] = N-fp-fn
            assert entry['T']>=0,(entry,gold_set,pred_set)
            entry['Acc'] = float('nan') if N==0 else entry['T'] / N
        df = DataFrame.from_items([(e, {k: entry[e]}) for e in PRCounter.COLUMNS])
        self._df = self._df.append(df)

    def __str__(self):
        return str(self._df)

    def __add__(self, that):
        # ensure all rows are present for both tables, filling in 0 if necessary
        # (otherwise the empty rows will be treated as if they contain NaN when adding)
        
        me = self._df
        you = that._df
        for row in me.index:
            if row not in that._df.index:
                you = you.append(DataFrame.from_items([(e, {row: '' if me[e][row]=='' else 0}) for e in PRCounter.COLUMNS]))
        for row in you.index:
            if row not in self._df.index:
                me = me.append(DataFrame.from_items([(e, {row: '' if you[e][row]=='' else 0}) for e in PRCounter.COLUMNS]))
        
        # add counts
        new_df = me + you
        
        result = PRCounter()
        result._df = new_df
        if self.COMPUTE_RATIOS_ON_ADD: # recompute ratios
            self.compute_ratios()
        return result
    
    def compute_ratios(self):
        '''
        new_df['P'] = new_df['Numer'] / new_df['PDenom']
            new_df['R'] = new_df['Numer'] / new_df['RDenom']
            denom = (new_df['P'] + new_df['R'])
            new_df['F'] = 2 * new_df['P'] * new_df['R'] / denom[denom>0]
            new_df['Acc'] = new_df['T'] / new_df['N']
        '''
        df = self._df
        
        # if denominators are 0, set them to NaN
        for c in ('PDenom','RDenom'):
            for r,v in enumerate(df[c]):
                if v==0:
                    df[c][r] = float('nan')
        
        
        df['P'] = df['Numer'] / df['PDenom']
        df['R'] = df['Numer'] / df['RDenom']
        denom = (df['P'] + df['R'])
        df['F'] = 2 * df['P'] * df['R'] / denom[denom>0]
        df['Acc'] = df['T'] / df['N']

    def to_string(self, *args, **kwargs):
        return self._df.to_string(*args, **kwargs)

    def to_html(self, *args, **kwargs):
        return self._df.to_html(*args, **kwargs)

    def to_csv(self, *args, **kwargs):
        return self._df.to_csv(*args, **kwargs)


class Span(object):
    """
    Encodes one or more ranges of indices, and provides for
    iteration over those indices.

    >>> s = Span(3,6)
    >>> 5 in s
    True
    >>> list(s)
    [3, 4, 5]
    >>> s.encompasses(Span(3,4))
    True
    >>> s + Span(0,3)
    Span(0,6)
    >>> list(Span(3,6, 9,12))
    [3, 4, 5, 9, 10, 11]
    >>> Span(3,6, 9,12).subspans()
    [Span(3,6), Span(9,12)]

    Unlike slice objects, Span objects are hashable, so they
    can be used as dict keys/set entries.
    """
    
    JOIN_ADJACENT_SUBRANGES = False
    
    def __init__(self, *args):
        if not args or len(args)%2!=0:
            raise Exception('Span() constructor must have a positive even number of arguments: '+repr(args))
        self._s = []
        first = True
        for start,stop in sorted(zip(args[::2],args[1::2])):
            if first:
                first = False
            elif self.overlaps(Span(start,stop)):
                raise Exception('Span() constructor must not contain overlapping ranges: '+repr(args))
            elif self.JOIN_ADJACENT_SUBRANGES and self._s[-1].stop==start:   # join adjacent subranges
                if self._s[-1].stop==start:
                    self._s[-1] = slice(self._s[-1].start,stop)
                continue
            self._s.append(slice(start,stop))

    def __add__(self, that):
        if len(self._s)!=1 or len(that._s)!=1: assert False
        if self._s[0].stop==that._s[0].start: return Span(self._s[0].start,that._s[0].stop)
        elif that._s[0].stop==self._s[0].start: return Span(that._s[0].start,self._s[0].stop)
        raise Exception('Cannot add non-adjacent spans: '+repr(self)+' + ' + repr(that))
        #TODO: allow adding so as to produce multiple spans

    def __lt__(self, that):
        return self.minstart<that.minstart or self.maxstop<that.maxstop

    def __eq__(self, that):
        return self._s==that._s

    def __hash__(self):
        return sum(17*s.stop+s.start for s in self._s)

    def __repr__(self):
        return 'Span({})'.format(', '.join('{},{}'.format(s.start,s.stop) for s in self._s))

    def __iter__(self):
        return itertools.chain(*[iter(range(s.start,s.stop)) for s in self._s])

    def __len__(self):
        return len(set(self))
    
    def __call__(self, sequence, typ=list or str):
        if typ in (str,unicode):
            try:
                return unicode(u' '.join(sequence[i] for i in self))
            except:
                raise Exception('Span out of bounds for sequence: {!r}, {!r}'.format(self,sequence))
        return typ(sequence[i] for i in self)

    def encompasses(self, that):
        return set(that)<=set(self)

    def overlaps_partially(self, that):
        return bool(set(that)-set(self) and set(self)-set(that))

    def overlaps(self,that):
        return bool(set(that)&set(self))

    def contiguous(self):
        return self.maxstop-self.minstart==len(self)
    
    def subspans(self):
        return [Span(s.start,s.stop) for s in self._s]

    @property
    def minstart(self):
        return min(s.start for s in self._s)

    @property
    def maxstop(self):
        return max(s.stop for s in self._s)


def span_from_element(element):
    return Span(*[i for sp in element['spans'] for i in [sp['start'], sp['end']]])


def get_predictions_by_span(frames):
    target_coverage = set()
    frame_names = {}
    arguments = {}
    for frame in frames:
        target_span = span_from_element(frame['target'])
        target_coverage |= set(target_span)
        frame_names[target_span] = frame['target'].get('name')  # None if no frame name (just evaluating target ID)
        # ignore all but the top-ranked set of argument predictions
        if 'annotationSets' in frame:
            arguments[target_span] = {
                span_from_element(fe): fe['name']
                for fe in frame['annotationSets'][0]['frameElements']
            }
        else:
            arguments[target_span] = {}
    return target_coverage, frame_names, arguments


def get_non_targets(gold_sentence):
    # index by span
    wsl = {
        Span(entry['start'], entry['end']): (entry['name'].lower(), entry['text'])
        for entry in gold_sentence['wsl']
    }
    ner = {
        Span(entry['start'], entry['end']): (entry['name'].lower(), entry['text'])
        for entry in gold_sentence['ner']
    }
    poses = {
        Span(entry['start'], entry['end']): entry['name'].upper()
        for entry in gold_sentence['pos']
    }
    # "WEA" (weapons) and "DATE" (dates) are the only named entity types that evoke frames
    excluded_ner_spans = {
        sp for sp, (ner_type, text) in ner.items()
        if ner_type != 'wea' and not (ner_type == 'date' and text.lower() in DATE_NAMES)
    }
    excluded_spans = set(wsl.keys()) | excluded_ner_spans
    return wsl, excluded_spans, poses


def _arg_points(frame_name, arg_name, non_core_score=OTHER_CORENESS_POINTS):
    return CORE_POINTS if CORE_STATUS[frame_name][arg_name]=='Core' else non_core_score

def score_sentence(gold, predicted, errors, verbose=False):
    #TODO
    #assert len(gold['tokens']) == len(predicted['tokens']) and ' '.join(gold['tokens']) == ' '.join(predicted['tokens'])
    sentence_stats = PRCounter()
    num_tokens = len(gold['tokens'])
    pred_target_coverage, pred_frames, pred_args = get_predictions_by_span(predicted['frames'])
    # tokens that should in principle be part of a target
    gold_target_coverage = set(range(num_tokens))
    # target spans with gold frame annotations, plus other tokens that should in principle belong to a target
    gold_target_spans = set()
    # tokens in target spans with gold frame annotations
    gold_frame_target_coverage = set()

    wsl, excluded_spans, poses = get_non_targets(gold)

    gold_frames = {}
    gold_args = {}
    for frame in gold['frames']:
        target = frame['target']
        target_span = span_from_element(target)
        gold_target_spans.add(target_span)
        gold_frame_target_coverage |= set(target_span)
        gold_frames[target_span] = target['name']
        gold_args[target_span] = {
            span_from_element(elt): elt['name']
            for elt in frame['annotationSets'][0]['frameElements']
        }
        if target_span in excluded_spans:
            if target_span in wsl:  # obviously a bug with WSL
                print('WSL bug:', target_span, repr(target_span(gold['tokens'], str)), file=sys.stderr)
                excluded_spans.remove(target_span)
            else:
                print(target['spans'][0]['text'],
                      {
                          entry['name'] for entry in gold['ner']
                          if entry['start'] == target_span.minstart
                      },
                      file=sys.stderr)
        elif len(target_span) > 1:
            for sp in excluded_spans:
                if target_span.overlaps(sp):
                    print('Target span',target_span,repr(target_span(gold['tokens'],str)),'overlaps with excluded span',sp,repr(sp(gold['tokens'],str)), file=sys.stderr)

    for span in excluded_spans:
        gold_target_coverage -= set(span)
    for tkn in gold_target_coverage:
        if tkn not in gold_frame_target_coverage:
            gold_target_spans.add(Span(tkn, tkn + 1))
    
    sentence_stats['Targets by token'] = num_tokens, gold_target_coverage, pred_target_coverage
    sentence_stats['Targets by span'] = gold_target_spans, set(pred_frames.keys())
    
    for sp in gold_target_spans-set(pred_frames.keys()):
        # missed target
        pos = ''
        if len(sp)==1:  # record POS tag
            pos = poses.get(sp,'?')
            errors['miss'][pos] += 1
            pos = '_'+pos
        errors['miss'][sp(gold['tokens'],str)+pos] += 1
    for sp in set(pred_frames.keys())-gold_target_spans:
        # extra target
        pos = ''
        if len(sp)==1:  # record POS tag
            pos = poses.get(sp,'?')
            errors['extra'][pos] += 1
            pos = '_'+pos
        errors['extra'][sp(gold['tokens'],str)+pos] += 1
    
    if any(pred_frames.values()) or any(gold_frames.values()):
        
        sentence_stats['Frames with correct targets (ignore P)'] = set(gold_frames.items()), set(pred_frames.items())
        
        gold_tot, pred_tot = {}, {}
        
        if any(pred_args.values()) or any(gold_args.values()):
        
            # this is an unlabeled measure, so we do not distinguish between core and non-core; 
            # every span is 1 point
            all_gold_args = {(tspan,aspan) for tspan,args in gold_args.items() for aspan in args}
            all_pred_args = {(tspan,aspan) for tspan,args in pred_args.items() for aspan in args}
            sentence_stats['Argument spans with correct targets'] = all_gold_args, all_pred_args
                    
            all_gold_args = {(tspan,gold_frames[tspan],aspan) for tspan,args in gold_args.items() for aspan in args}
            all_pred_args = {(tspan,pred_frames[tspan],aspan) for tspan,args in pred_args.items() for aspan in args}
            sentence_stats['Argument spans with correct targets and frames'] = all_gold_args, all_pred_args
            
            all_gold_args = {(tspan,gold_frames[tspan],aspan,aname): _arg_points(gold_frames[tspan], aname) for tspan,args in gold_args.items() for aspan,aname in args.items()}
            all_pred_args = {(tspan,pred_frames[tspan],aspan,aname): _arg_points(pred_frames[tspan], aname) for tspan,args in pred_args.items() for aspan,aname in args.items()}
            sentence_stats['Arguments, labeled, with correct targets and frames'] = all_gold_args, all_pred_args
        
            gold_tot, pred_tot = dict(all_gold_args), dict(all_pred_args)
            
            all_gold_args = {(tspan,gold_frames[tspan],aspan,aname): _arg_points(gold_frames[tspan], aname, 0) for tspan,args in gold_args.items() for aspan,aname in args.items()}
            all_pred_args = {(tspan,pred_frames[tspan],aspan,aname): _arg_points(pred_frames[tspan], aname, 0) for tspan,args in pred_args.items() for aspan,aname in args.items()}
            sentence_stats['Core arguments, labeled, with correct targets and frames'] = all_gold_args, all_pred_args
        
        gold_tot.update({x: 1 for x in set(gold_frames.items())})
        pred_tot.update({x: 1 for x in set(pred_frames.items())})
        
        # TODO: partial credit frame/frame element matching (issues include: if the gold and pred FE are related 
        # through the hierarchy but only one is core, should 1 or .5 points be awarded?)
        
        
        # now remove everything that involved a target for which we have no gold frame
        
        correct_target_spans = set(gold_frames.keys()) & set(pred_frames.keys())
        
        for span in gold_frames.keys():
            if span not in correct_target_spans:
                del gold_frames[span]
        for span in pred_frames.keys():
            if span not in correct_target_spans:
                del pred_frames[span]
        assert len(gold_frames)==len(pred_frames)==len(correct_target_spans),(gold_frames,pred_frames,correct_target_spans)
        sentence_stats['Frames (correct targets only)'] = len(correct_target_spans), set(gold_frames.items()), set(pred_frames.items())
        
        if any(pred_args.values()) or any(gold_args.values()):
        
            for span in gold_args.keys():
                if span not in correct_target_spans:
                    del gold_args[span]
            for span in pred_args.keys():
                if span not in correct_target_spans:
                    del pred_args[span]
            
            all_gold_args = {(tspan,aspan) for tspan,args in gold_args.items() for aspan in args}
            all_pred_args = {(tspan,aspan) for tspan,args in pred_args.items() for aspan in args}
            sentence_stats['Argument spans (correct targets only)'] = all_gold_args, all_pred_args
            
            all_gold_args = {(tspan,gold_frames[tspan],aspan) for tspan,args in gold_args.items() for aspan in args}
            all_pred_args = {(tspan,pred_frames[tspan],aspan) for tspan,args in pred_args.items() for aspan in args}
            sentence_stats['Argument spans (correct targets and frames only)'] = all_gold_args, all_pred_args
            
            all_gold_args = {(tspan,gold_frames[tspan],aspan,aname): _arg_points(gold_frames[tspan], aname) for tspan,args in gold_args.items() for aspan,aname in args.items()}
            all_pred_args = {(tspan,pred_frames[tspan],aspan,aname): _arg_points(pred_frames[tspan], aname) for tspan,args in pred_args.items() for aspan,aname in args.items()}
            sentence_stats['Arguments, labeled (correct targets and frames only)'] = all_gold_args, all_pred_args
            
            all_gold_args = {(tspan,gold_frames[tspan],aspan,aname): _arg_points(gold_frames[tspan], aname, 0) for tspan,args in gold_args.items() for aspan,aname in args.items()}
            all_pred_args = {(tspan,pred_frames[tspan],aspan,aname): _arg_points(pred_frames[tspan], aname, 0) for tspan,args in pred_args.items() for aspan,aname in args.items()}
            sentence_stats['Core arguments, labeled (correct targets and frames only)'] = all_gold_args, all_pred_args
            
        if verbose:
            print('gold_tot', gold_tot, file=sys.stderr)
            print('pred_tot', pred_tot, file=sys.stderr)
        sentence_stats['Total frames and labeled arguments'] = gold_tot, pred_tot
    # TODO: provided the target is correct, arguments can get credit regardless of the predicted frame label
    # should this be changed/made an option?
    #print(sentence_stats.to_string())
    #assert False
    return sentence_stats


if __name__=='__main__':
    gold_filename, pred_filename = sys.argv[1:]
    scores = None
    with codecs.open(gold_filename, 'r', 'utf-8') as gold_file, codecs.open(pred_filename, 'r', 'utf-8') as pred_file:
        errors = {'miss': Counter(), 'extra': Counter()}
        for sentNum,(gold_line,pred_line) in enumerate(zip(gold_file, pred_file)):
            sent_scores = score_sentence(json.loads(gold_line), json.loads(pred_line), errors, verbose=(sentNum==366))
            if sentNum==366:
                assert False,errors
            # sentence statistics format matches that of fnSemScore_modified.pl
            if 'Total frames and labeled arguments' in sent_scores._df.index:
                tot = sent_scores._df['Total frames and labeled arguments':'Total frames and labeled arguments']
                print('Sentence ID={sentNum}: Recall={t.R[0]:0.5f} ({t.Numer[0]:0.1f}/{t.RDenom[0]:0.1f}) Precision={t.P[0]:0.5f} ({t.Numer[0]:0.1f}/{t.PDenom[0]:0.1f}) Fscore={t.F[0]:0.5f}'.format(sentNum=sentNum, t=tot), file=sys.stderr)
            if scores is None:
                scores = sent_scores
            else:
                scores = scores + sent_scores
    #print(errors, file=sys.stderr)
    scores.compute_ratios()
    print(scores.to_string())
    