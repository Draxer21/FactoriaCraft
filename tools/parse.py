import re,sys,json
def parse(path):
    src=open(path).read()
    toks=[]
    for m in re.finditer(r'addOrReplaceChild\("([a-z_]+)"|texOffs\((\d+),\s*(\d+)\)|addBox\(([^)]*)\)',src):
        if m.group(1): toks.append(('part',m.group(1),m.start()))
        elif m.group(2): toks.append(('tex',(int(m.group(2)),int(m.group(3))),m.start(),m.end()))
        else: toks.append(('box',m.group(4),m.start()))
    groups=[];part=None;cur=None
    for t in toks:
        if t[0]=='part': part=t[1]
        elif t[0]=='tex':
            cur={'part':part,'old':t[1],'span':(t[2],t[3]),'boxes':[]};groups.append(cur)
        else:
            a=[float(x.strip().rstrip('F').replace('x - 1.5','0').replace('x','0') or 0) if re.match(r'^[-\d. F]+$',x.strip()) else 0 for x in t[1].split(',')]
            vals=[x.strip() for x in t[1].split(',')]
            w,h,d=[float(v.rstrip('F')) for v in vals[3:6]]
            cur['boxes'].append((w,h,d))
    return src,groups
if __name__=='__main__':
    for p in sys.argv[1:]:
        _,g=parse(p)
        for x in g: print(x['part'],x['old'],x['boxes'])
