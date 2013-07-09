from nltk.corpus import wordnet as wn

WN_POSTAGS = {
    'j': wn.ADJ,
    'v': wn.VERB,
    'n': wn.NOUN,
    'r': wn.ADV,
}
CONTRACTIONS = {    # see https://en.wikipedia.org/wiki/Contraction_%28grammar%29#English
    "'m": 'be',
    "'re": 'be',
    "'ve": 'have',
    "'ll": 'will',
    "'ll've": 'will_have',
    "n't": 'not',
    "'em": 'them',
    "'im": 'him',
    "'t": 'it',
    "o'": 'of'
}   # also 's if a verb or noun (see below)
# ignoring 'd because it is ambiguous between "had", "would", and "did"

def get_lemma(form, pos):
    """
    Lowercases the provided word form and consults WordNet for the lemma, 
    subject to the provided POS tag
    """ 
    form = form.lower()
    wn_pos = WN_POSTAGS.get(pos[0].lower(), wn.NOUN)
    if form=="'s":
        if wn_pos==wn.VERB: return 'be' # probably "is" (less commonly: "does" or "has")
        elif pos=='PRP': return 'us'
    return CONTRACTIONS.get(form) or wn.morphy(form, wn_pos) or form
