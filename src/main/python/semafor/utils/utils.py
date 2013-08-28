INF = float("inf")


def ngrams(words, n):
    return zip(*[words[i:] for i in range(n)])


def get_coarse_pos(pos):
    pos = pos.upper()
    if pos == "PRP" or pos == "PRP$" or len(pos) <= 2:
        cpostag = pos
    else:
        cpostag = pos[0:2]
    return cpostag


def _get_path(sources, dests, tokens):
    max_depth = len(tokens) + 1
    best_path, best_len = None, INF
    for source in sources:
        depth = 0
        path = [source, tokens[source.head]]
        while path[-1] not in dests \
                and path[-1] != 0 \
                and len(path) < best_len \
                and depth < max_depth:
            path.append(tokens[path[-1].head])
            depth += 1
        if path[-1] in dests:
            best_path, best_len = path, len(path)
    return best_path


def get_path(target, frame_element, tokens):
    target_tokens = target['tokens']
    frame_element_tokens = frame_element['tokens']
    path, ext = _get_path(target_tokens, frame_element_tokens, tokens), False
    if path is None:
        path, ext = _get_path(frame_element_tokens, target_tokens, tokens), True
    return path, ext


def get_head(span_tokens):
    if len(span_tokens) == 0:
        return None
    idxs = [token.id for token in span_tokens]
    for token in span_tokens:
        if token.head not in idxs:
            return token
    return span_tokens[0]
