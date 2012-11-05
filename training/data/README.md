This is a README about the full text annotations used for training SEMAFOR 2.0 
on FrameNet 1.5 full text annotations
Dipanjan Das 
dipanjan@cs.cmu.edu
2/18/2012
==============================================================================

1) Of interest are the *.tokenized files which I have automatically tokenized using the Penn Treebank conventions.

2) Parsed files are *.all.lemma.tags. The format these files follow is:
   i) Each line contains a sentence with annotations. 
   ii) All tokens per line are tab separated.
   iii) The first token is the number of words in the sentence (n).
   iv) After that come n words.
   v) Then come n POS tags.
   vi) The third series of n tokens correspond to dependency tree labels for each word's syntactic parent.
   vii) The fourth series of n tokens correspond to the index of each syntactic parent (0 is the dummy word, 1 is the first word, and so on).
   viii) The fifth series of n tokens are '0'-s. These were there for providing the capability of using NE tags, but right now we don't use them.
   ix) The final series of n tokens are lemmas for each word, computed using WordNet.

3) The full text annotations in FrameNet are in *.frame.elements. The format of these files is:
   i) Each line corresponds to one predicate-argument structure; again tokens are tab separated.
   ii) The first token counts the number of roles and the frame. E.g., if there are 2 roles, this number will be 3.
   iii) The second token is the frame.
   iv) The third token is the lexical unit.
   v) The fourth token is the token number of the actual target in the sentence (token numbers start with 0).
      If the target has multiple words, the token numbers will be series of numbers separated by _.
   vi) The fifth token is the actual form of the target in the sentence.
   vii) The sixth token is the sentence number in the corresponding *.all.lemma.tags file in which this predicate-argument 
        structure appears. Again sentence numbers start from 0.
   viii) After that come role and span pairs. If the span contains more than one word, the span is denoted by start:end, 
         where start is the index of the first word in the span, and end is the last one. The word indices again start from 0.
