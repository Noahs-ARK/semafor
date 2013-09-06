import sys,json
for line in sys.stdin:
    v,s,o = line.split()[-3:]
    args = []
    if s != 'NONE':
        args.append(("subj",s))
    if o != 'NONE':
        args.append(("obj",o))
    tpl = [v, args]
    print json.dumps(tpl)
