#!/usr/bin/env python2.7
"""
Scorer for frame-semantic parsing output.
Requires two JSON files: the gold analysis, and the predictions.

If k-best argument predictions are present, only considers the top-ranked set.

@author: Nathan Schneider (nschneid)
@since: 2013-05-15
"""

from __future__ import print_function, division
import sys, json, codecs, itertools, operator
from pandas import DataFrame


class PRCounter(DataFrame):
    _df = DataFrame(columns=['Numer','PDenom','RDenom','P','R','F','T','N','Acc'])

    def __setitem__(self, k, v):
        if isinstance(v[0],int):
            N, goldSet, predSet = v
        else:
            goldSet, predSet = v
            N = ''
        entry = {
            'Numer': len(goldSet & predSet),
            'PDenom': len(predSet),
            'RDenom': len(goldSet),
            'N': N
        }
        entry['P'] = entry['Numer']/entry['PDenom'] if entry['PDenom'] else float('nan')
        entry['R'] = entry['Numer']/entry['RDenom'] if entry['RDenom'] else float('nan')
        entry['F'] = 2*entry['P']*entry['R']/(entry['P']+entry['R']) if (entry['P']+entry['R']) else float('nan')
        entry['T'] = None if N=='' else N-len(goldSet ^ predSet)/2
        entry['Acc'] = {'':None, 0:float('nan')}[N] if not N else entry['T']/N
        df = DataFrame.from_items([(e,{k:entry[e]}) for e in ['Numer','PDenom','RDenom','P','R','F','T','N','Acc']])
        self._df = self._df.append(df)

    def __str__(self):
        return str(self._df)

    def __add__(self, that):
        newdf = self._df + that._df
        # counts get added, now we need to recompute ratios
        newdf['P'] = newdf['Numer']/newdf['PDenom']
        newdf['R'] = newdf['Numer']/newdf['RDenom']
        newdf['F'] = 2*newdf['P']*newdf['R']/(newdf['P']+newdf['R'])
        newdf['Acc'] = newdf['T']/newdf['N']
        result = PRCounter()
        result._df = newdf
        return result

    def to_string(self,*args,**kwargs):
        return self._df.to_string(*args,**kwargs)

    def to_html(self,*args,**kwargs):
        return self._df.to_html(*args,**kwargs)

    def to_csv(self,*args,**kwargs):
        return self._df.to_csv(*args,**kwargs)


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
    >>> [3, 4, 5, 9, 10, 11]
    >>> Span(3,6, 9,12).subspans()
    [Span(3,6), Span(9,12)]

    Unlike slice objects, Span objects are hashable, so they
    can be used as dict keys/set entries.
    """
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
            elif self._s[-1].stop==start:   # join adjacent subranges
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
            return typ(' '.join(sequence[i] for i in self))
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


def score_sentence(gold, pred):
    assert len(gold['tokens'])==len(pred['tokens']) and ' '.join(gold['tokens'])==' '.join(pred['tokens'])
    
    c = PRCounter()
    num_tokens = len(gold['tokens'])
    predTargetCoverage = set()
    predFrames = {}
    predArgs = {}
    for f in pred['frames']:
        targetSpan = Span(*[i for sp in f['target']['spans'] for i in [sp['start'],sp['end']]])
        predTargetCoverage |= set(targetSpan)
        predFrames[targetSpan] = f['target']['name']
        # ignore all but the top-ranked set of argument predictions
        predArgs[targetSpan] = {
            Span(*[i for sp in elt['spans'] for i in [sp['start'],sp['end']]]): elt['name'] 
            for elt in f['annotationSets'][0]['frameElements']
        }
    goldTargetCoverage = set(range(num_tokens)) # tokens that should in principle be part of a target
    goldTargetSpans = set() # target spans with gold frame annotations, plus other tokens that should in principle belong to a target
    goldFrameTargetCoverage = set() # tokens in target spans with gold frame annotations
    
    wsl = {Span(entry['start'],entry['end']): (entry['name'],entry['text']) for entry in gold['wsl']}
    DATENAMES = ['monday','tuesday','wednesday','thursday','friday','saturday','sunday','january','february','march','april','may','june','july','august','september','october','november','december']
    DATENAMES += [v for d in DATENAMES for v in [d[:3],d[:3]+'.']]
    ner = {Span(entry['start'],entry['end']): (entry['name'],entry['text']) for entry in gold['ner']}
    excluded = set(wsl.keys()) | {sp for sp,(etype,text) in ner.items() if etype!='WEA' and not (etype=='date' and text.lower() in DATENAMES)}
    
    
    goldFrames = {}
    goldArgs = {}
    for f in gold['frames']:
        targetSpan = Span(*[i for sp in f['target']['spans'] for i in [sp['start'], sp['end']]])
        goldTargetSpans.add(targetSpan)
        goldFrameTargetCoverage |= set(targetSpan)
        goldFrames[targetSpan] = f['target']['name']
        goldArgs[targetSpan] = {
            Span(*[i for sp in elt['spans'] for i in [sp['start'],sp['end']]]): elt['name']
            for elt in f['annotationSets'][0]['frameElements']
        }
        if targetSpan in excluded:
            if targetSpan in wsl:  # obviously a bug with WSL
                print('WSL bug:', targetSpan, repr(targetSpan(gold['tokens'],str)), file=sys.stderr)
                excluded.remove(targetSpan)
            else:
                print(f['target']['spans'][0]['text'], {entry['name'] for entry in gold['ner'] if entry['start']==targetSpan.minstart}, file=sys.stderr)
        elif len(targetSpan)>1:
            for sp in excluded:
                if targetSpan.overlaps(sp):
                    print('Target span',targetSpan,repr(targetSpan(gold['tokens'],str)),'overlaps with excluded span',sp,repr(sp(gold['tokens'],str)), file=sys.stderr)
    
    # if True:
    #         gstuff = [(sp,fr,sp(gold['tokens'],str)) for sp,fr in sorted(goldFrames.items(), key=operator.itemgetter(0))]
    #         pstuff = [(sp,fr,sp(pred['tokens'],str)) for sp,fr in sorted(predFrames.items(), key=operator.itemgetter(0))]
    #         if gstuff and pstuff and 'morning' in zip(*gstuff)[2] and 'morning' not in zip(*pstuff)[2]:
    #             print(pstuff, file=sys.stderr)
    #             print(gstuff, file=sys.stderr)
    #             assert False
    
    for span in excluded:
        goldTargetCoverage -= set(span)
    for tkn in goldTargetCoverage:
        if tkn not in goldFrameTargetCoverage:
            goldTargetSpans.add(Span(tkn,tkn+1))
    c['Targets by token'] = num_tokens, goldTargetCoverage, predTargetCoverage
    c['Targets by span'] = goldTargetSpans, set(predFrames.keys())
    correctTargetSpans = set(goldFrames.keys()) & set(predFrames.keys())

    c['Frames with correct targets (ignore P)'] = len(correctTargetSpans), set(goldFrames.items()), set(predFrames.items())

    for span in goldFrames.keys():
        if span not in correctTargetSpans:
            del goldFrames[span]
    for span in predFrames.keys():
        if span not in correctTargetSpans:
            del predFrames[span]
    assert len(goldFrames)==len(predFrames)==len(correctTargetSpans),(goldFrames,predFrames,correctTargetSpans)
    c['Frames (correct targets only)'] = len(correctTargetSpans), set(goldFrames.items()), set(predFrames.items())

    allGoldArgs = set((tspan,arg) for tspan,args in goldArgs.items() for arg in args)
    allPredArgs = set((tspan,arg) for tspan,args in predArgs.items() for arg in args)
    c['Argument spans with correct targets'] = allGoldArgs, allPredArgs
    allGoldArgs = set((tspan,)+arg for tspan,args in goldArgs.items() for arg in args.items())
    allPredArgs = set((tspan,)+arg for tspan,args in predArgs.items() for arg in args.items())
    c['Arguments, labeled, with correct targets'] = allGoldArgs, allPredArgs
    for span in goldArgs.keys():
        if span not in correctTargetSpans:
            del goldArgs[span]
    for span in predArgs.keys():
        if span not in correctTargetSpans:
            del predArgs[span]
    allGoldArgs = set((tspan,arg) for tspan,args in goldArgs.items() for arg in args)
    allPredArgs = set((tspan,arg) for tspan,args in predArgs.items() for arg in args)
    c['Argument spans (correct targets only)'] = allGoldArgs, allPredArgs
    allGoldArgs = set((tspan,)+arg for tspan,args in goldArgs.items() for arg in args.items())
    allPredArgs = set((tspan,)+arg for tspan,args in predArgs.items() for arg in args.items())
    c['Arguments, labeled (correct targets only)'] = allGoldArgs, allPredArgs
    
    # TODO: provided the target is correct, arguments can get credit regardless of the predicted frame label
    # should this be changed/made an option?
    
    return c


if __name__=='__main__':
    goldFP, predFP = sys.argv[1:]
    scores = None
    with codecs.open(goldFP,'r','utf-8') as goldF, codecs.open(predFP,'r','utf-8') as predF:
        for gln in goldF:
            pln = next(predF)
            gold = json.loads(gln)
            pred = json.loads(pln)
            sentscores = score_sentence(gold,pred)
            if scores is None:
                scores = sentscores
            else:
                scores = scores + sentscores
    print(scores.to_string())
    