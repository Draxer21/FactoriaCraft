import math,random,re
from PIL import Image
from parse import parse

def C(h): h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))+(255,)
def shade(c,f): return tuple(max(0,min(255,int(v*f))) for v in c[:3])+(255,)

# material palettes: base color + detail style
MAT={
 'yellow':dict(base=C('e8a21c'),style='panel'),
 'orange':dict(base=C('e0701e'),style='panel'),
 'steel':dict(base=C('5b636b'),style='plate'),
 'darksteel':dict(base=C('3a4047'),style='plate'),
 'track':dict(base=C('2a2d31'),style='track'),
 'roller':dict(base=C('7d868f'),style='bolt'),
 'chrome':dict(base=C('b9c2c9'),style='smooth'),
 'black':dict(base=C('1e2124'),style='smooth'),
 'seat':dict(base=C('2b2b2f'),style='seat'),
 'red':dict(base=C('c8322b'),style='smooth'),
 'light':dict(base=C('fff2a8'),style='glow'),
 'grate':dict(base=C('6d747b'),style='grate'),
 'glassframe':dict(base=C('2c3238'),style='smooth'),
 'drill':dict(base=C('9aa3ab'),style='spiral'),
 'drilltip':dict(base=C('d4dade'),style='spiral'),
 'hazard':dict(base=C('e8a21c'),style='hazard'),
 'console':dict(base=C('30353b'),style='console'),
 'vent':dict(base=C('e0701e'),style='vent'),
 'tire':dict(base=C('1f2124'),style='track'),
 'load':dict(base=C('7a6248'),style='load'),
 'grille':dict(base=C('2c3238'),style='grate'),
 'green':dict(base=C('4f8a3a'),style='panel'),
 'ylattice':dict(base=C('e8a21c'),style='grate'),
}

def paint_face(img,x0,y0,w,h,mat,face,seed):
    if w<=0 or h<=0: return
    m=MAT[mat];b=m['base'];st=m['style']
    rnd=random.Random(seed)
    f={'top':1.12,'bottom':0.7,'long':1.0,'end':0.86}[face]
    base=shade(b,f)
    px=img.load()
    for y in range(h):
        for x in range(w):
            c=base
            n=rnd.uniform(-0.035,0.035)
            c=shade(c,1+n)
            X,Y=x0+x,y0+y
            if st=='panel':
                if face in('long','end') and y==0 and h>2: c=shade(base,1.18)
                elif face in('long','end') and y==h-1 and h>2: c=shade(base,0.72)
                elif face in('top','bottom') and (x==0 or y==0 or x==w-1 or y==h-1) and w>3 and h>3: c=shade(base,0.85)
                if face=='long' and w>=10 and h>=4 and x%8==4 and 1<=y<=h-2: c=shade(base,0.78)  # panel seams
                if face=='long' and w>=10 and h>=4 and x%8==6 and y==1: c=C('2a2d31')  # rivet
            elif st=='plate':
                if (x==0 or x==w-1 or y==0 or y==h-1) and w>2 and h>2: c=shade(base,0.8)
                if w>=6 and h>=4 and (x%6==2 and y%4==1): c=shade(base,1.25)
            elif st=='track':
                if face=='top' or face=='bottom':
                    c=shade(base,1.25) if x%3==0 else base
                elif face=='long':
                    if y==0 or y==h-1: c=shade(base,1.3)
                    elif x%3==0: c=shade(base,0.7)
                    elif y==h//2: c=shade(base,1.12)
                else:
                    c=shade(base,1.25) if y%2==0 else base
            elif st=='bolt':
                if (x==0 or y==0 or x==w-1 or y==h-1): c=shade(base,0.75)
                if w>=3 and h>=3 and x==w//2 and y==h//2: c=C('2a2d31')
            elif st=='seat':
                if y%2==0 and face!='bottom': c=shade(base,1.15)
            elif st=='glow':
                c=shade(base,1.0 if face!='bottom' else 0.8)
                if (x==0 or x==w-1 or y==0 or y==h-1) and w>2 and h>2: c=C('3a4047')
            elif st=='grate':
                c=shade(base,0.55) if (x%2==1 and y%2==1) else base
            elif st=='spiral':
                if face in('long','end') or face in ('top','bottom'):
                    if (x+y)%4==0: c=shade(base,0.62)
                    elif (x+y)%4==1: c=shade(base,1.18)
            elif st=='hazard':
                if face=='end' or face=='long':
                    c=C('1e2124') if ((x+y)//2)%2==0 else base
                if y==0: c=shade(base,1.15)
            elif st=='console':
                if face=='top' and 1<=y<=h-2 and x%2==1 and 0<x<w-1:
                    c=[C('4fd16b'),C('e2c43a'),C('d8473c')][(x//2+y)%3]
            elif st=='load':
                k=rnd.random()
                c=shade(base,0.7) if k<0.2 else (shade(base,1.25) if k>0.85 else base)
                if k>0.97: c=C('8d8a85')
            elif st=='vent':
                if face=='long' and 1<=y<=h-2 and x%3!=0 and 1<x<w-2: c=C('2a2d31') if y%2==1 else shade(base,0.8)
                elif face in('long','end') and y==0: c=shade(base,1.18)
            px[X,Y]=c

def paint_box(img,u,v,w,h,d,mat,seed):
    W,H,D=[math.ceil(t) for t in (w,h,d)]
    # MC box UV: top strip at v (two faces), sides strip at v+D
    paint_face(img,u+D,v,W,D,mat,'top',seed)
    paint_face(img,u+D+W,v,W,D,mat,'bottom',seed+1)
    paint_face(img,u,v+D,D,H,mat,'end',seed+2)
    paint_face(img,u+D,v+D,W,H,mat,'long',seed+3)
    paint_face(img,u+D+W,v+D,D,H,mat,'end',seed+4)
    paint_face(img,u+2*D+W,v+D,W,H,mat,'long',seed+5)

def build(java,matfn,outpng):
    src,groups=parse(java)
    # region size for each group
    regs={}
    for g in groups:
        g['mat']=matfn(g)
        dims=tuple(g['boxes'][0])
        rw=max(math.ceil(2*(d+w)) for w,h,d in g['boxes'])
        rh=max(math.ceil(d+h) for w,h,d in g['boxes'])
        key=(g['mat'],tuple(g['boxes']))
        g['key']=key;g['rw']=rw;g['rh']=rh
        regs.setdefault(key,(rw,rh,g['boxes'],g['mat']))
    items=sorted(regs.items(),key=lambda kv:(-kv[1][1],-kv[1][0]))
    for TW,TH in [(128,128),(256,128),(256,256)]:
        pos={};x=y=0;row=0;ok=True
        for k,(rw,rh,_,_) in items:
            if rw>TW: ok=False;break
            if x+rw>TW: x=0;y+=row;row=0
            if y+rh>TH: ok=False;break
            pos[k]=(x,y);x+=rw;row=max(row,rh)
        if ok: break
    assert ok
    img=Image.new('RGBA',(TW,TH),(0,0,0,0))
    for i,(k,(rw,rh,boxes,mat)) in enumerate(items):
        u,v=pos[k]
        w,h,d=boxes[0]
        paint_box(img,u,v,w,h,d,mat,i*10)
    img.save(outpng)
    # rewrite java
    out=src
    for g in sorted(groups,key=lambda g:-g['span'][0]):
        u,v=pos[g['key']]
        a,b=g['span']
        out=out[:a]+f'texOffs({u}, {v})'+out[b:]
    out=re.sub(r'LayerDefinition\.create\(mesh, \d+, \d+\)',f'LayerDefinition.create(mesh, {TW}, {TH})',out)
    open(java,'w').write(out)
    print(java,TW,TH,len(items),'regions')

def exc_mat(g):
    p=g['part'];b=g['boxes'][0]
    if p in('left_track','right_track'): return 'track'
    if 'cleat' in p: return 'darksteel'
    if 'roller' in p: return 'roller'
    if p in('undercarriage','turntable','cab_floor'): return 'darksteel'
    if p=='counterweight': return 'hazard'
    if p in('chassis','cab_rear_panel','cab_roof','loader_arm_left','loader_arm_right','boom_base','boom_arm'): return 'yellow'
    if 'post' in p: return 'yellow'
    if p.startswith('windshield'): return 'glassframe'
    if p.startswith('seat'): return 'seat'
    if 'joystick' in p and 'knob' not in p: return 'black'
    if p=='joystick_knobs': return 'red'
    if p=='exhaust': return 'steel'
    if p=='exhaust_cap': return 'black'
    if p=='steps': return 'grate'
    if p=='work_lights' or p.startswith('headlight'): return 'light'
    if p=='bucket':
        if b==(2.0,2.0,30.0) or b==(3.0,1.0,2.0): return 'chrome'
        return 'steel'
    if p=='hydraulic_cylinder': return 'darksteel'
    if p=='hydraulic_rod': return 'chrome'
    raise Exception(p)

def tun_mat(g):
    p=g['part'];b=g['boxes'][0]
    if p in('left_track','right_track'): return 'track'
    if 'roller' in p: return 'roller'
    if p in('lower_hull','cab_floor','drill_mount'): return 'darksteel'
    if p=='engine_housing': return 'vent'
    if p=='rear_deck': return 'hazard'
    if p=='seat': return 'seat'
    if p in('cab_back','cab_roof_rear'): return 'orange'
    if p.startswith('cab_post') or p.startswith('cab_front'): return 'orange'
    if p=='console': return 'console'
    if p=='lights' or p.startswith('headlight'): return 'light'
    if p=='drill_head':
        if b==(14.0,12.0,12.0): return 'darksteel'
        if b==(4.0,14.0,14.0): return 'steel'
        if b==(5.0,5.0,5.0): return 'drilltip'
        return 'drill'
    if 'auger' in p: return 'chrome'
    raise Exception(p)

def truck_mat(g):
    p=g['part']
    if p.endswith('_track'): return 'track'
    if p.startswith('roller'): return 'roller'
    if p in('chassis',): return 'darksteel'
    if p in('cab_lower','cab_back','cab_roof','cab_post_left','cab_post_right'): return 'yellow'
    if p=='steering': return 'black'
    if p=='hood': return 'grille'
    if p=='bumper': return 'chrome'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    if p=='exhaust': return 'steel'
    if p=='bed': return 'steel'
    if p in('bed_left','bed_right','bed_front','tailgate'): return 'orange'
    if p=='load': return 'load'
    raise Exception(p)

def wagon_mat(g):
    p=g['part']
    if p.endswith('_track'): return 'track'
    if p=='chassis': return 'darksteel'
    if p=='bin_floor': return 'steel'
    if p.startswith('bin_'): return 'orange'
    if p in('tow_bar','rear_hitch'): return 'chrome'
    if p=='load': return 'load'
    raise Exception(p)

def paver_mat(g):
    p=g['part']
    if p=='front_roller': return 'steel'
    if p in('roller_yoke','body','roof'): return 'yellow'
    if p=='rear_wheels': return 'track'
    if p=='seat': return 'seat'
    if p=='steering': return 'black'
    if p.startswith('post_'): return 'darksteel'
    if p=='exhaust': return 'steel'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def harvester_mat(g):
    p=g['part']
    if p=='header': return 'hazard'
    if p=='reel': return 'darksteel'
    if p.startswith('reel_arm'): return 'steel'
    if p in('body','grain_tank','cab_roof'): return 'green'
    if p.endswith('_track'): return 'track'
    if p=='unloader': return 'steel'
    if p=='seat': return 'seat'
    if p=='steering': return 'black'
    if p.startswith('cab_post'): return 'darksteel'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def crane_mat(g):
    p=g['part']
    if p.endswith('_track'): return 'track'
    if p.startswith('roller'): return 'roller'
    if p in('chassis','turntable','cab_floor'): return 'darksteel'
    if p in('house','cab_roof'): return 'yellow'
    if p=='counterweight': return 'hazard'
    if p=='boom_mount': return 'darksteel'
    if p=='boom': return 'ylattice'
    if p=='boom_tip': return 'steel'
    if p=='cable': return 'black'
    if p=='hook': return 'chrome'
    if p=='exhaust': return 'steel'
    if p=='seat': return 'seat'
    if p=='console': return 'console'
    if p.startswith('cab_post'): return 'darksteel'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def bulldozer_mat(g):
    p=g['part']
    if p.endswith('_track'): return 'track'
    if p.startswith('roller'): return 'roller'
    if p in('deck',): return 'darksteel'
    if p in('hood','cab_roof'): return 'yellow'
    if p=='grille': return 'grille'
    if p=='exhaust': return 'steel'
    if p=='seat': return 'seat'
    if p=='controls': return 'console'
    if p.startswith('cab_post'): return 'darksteel'
    if p=='blade': return 'yellow'
    if p=='blade_edge': return 'chrome'
    if p.startswith('push_arm') or p.startswith('ripper'): return 'darksteel'
    if p.startswith('lift_ram'): return 'chrome'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def tanker_mat(g):
    p=g['part']
    if p.endswith('_track'): return 'track'
    if p.startswith('roller'): return 'roller'
    if p=='chassis': return 'darksteel'
    if p in('cab_lower','cab_back','cab_roof','cab_post_left','cab_post_right'): return 'orange'
    if p=='steering': return 'black'
    if p=='hood': return 'grille'
    if p=='bumper': return 'chrome'
    if p=='exhaust': return 'steel'
    if p in('tank','tank_top'): return 'chrome'
    if p=='tank_band': return 'hazard'
    if p in('hatch','walkway'): return 'darksteel'
    if p=='pump': return 'steel'
    if p=='hose_reel': return 'black'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def tower_mat(g):
    p=g['part']
    if p=='base': return 'darksteel'
    if p in('tower','jib_inner','jib_outer','counter_jib','peak'): return 'ylattice'
    if p in('slewing','cab_floor','trolley'): return 'darksteel'
    if p=='seat': return 'seat'
    if p=='console': return 'console'
    if p.startswith('cab_post'): return 'darksteel'
    if p=='cab_roof': return 'yellow'
    if p=='counterweight': return 'hazard'
    if p=='cable': return 'black'
    if p=='hook': return 'chrome'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def forester_mat(g):
    p=g['part']
    if p.endswith('_track'): return 'track'
    if p.startswith('roller'): return 'roller'
    if p=='deck': return 'darksteel'
    if p in('engine','cab_roof','boom'): return 'green'
    if p=='exhaust': return 'steel'
    if p=='seat': return 'seat'
    if p=='controls': return 'console'
    if p.startswith('cab_post') or p=='boom_post': return 'darksteel'
    if p=='head': return 'hazard'
    if p.startswith('grapple'): return 'steel'
    if p in('saw','saw_cross','saw_square','saw_diamond'): return 'chrome'
    if p=='saw_tooth_': return 'darksteel'
    if p=='saw_hub': return 'black'
    if p=='saw_arm': return 'steel'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def loco_mat(g):
    p=g['part']
    if p in('frame','bogie_rear','bogie_front','cab_floor'): return 'darksteel'
    if p.startswith('roller'): return 'roller'
    if p in('hood','cab_wall_back','cab_roof'): return 'red'
    if p=='hood_stripe': return 'hazard'
    if p=='stack': return 'black'
    if p=='cowcatcher': return 'grille'
    if p=='seat': return 'seat'
    if p=='controls': return 'console'
    if p.startswith('cab_post'): return 'darksteel'
    if p.startswith('headlight'): return 'light'
    if p.startswith('tail_light'): return 'red'
    raise Exception(p)

def drone_mat(g):
    p=g['part']
    if p=='body': return 'orange'
    if p.startswith('arm'): return 'darksteel'
    if p=='camera': return 'black'
    if p.startswith('leg'): return 'steel'
    if p.startswith('motor'): return 'black'
    if p.startswith('rotor'): return 'chrome'
    raise Exception(p)


def heli_mat(g):
    p=g['part']
    if p.startswith('skid') or p.startswith('strut'): return 'darksteel'
    if p in('cabin_floor','cabin_back','cabin_roof','side_left','side_right','nose','tail_boom','tail_fin'): return 'red'
    if p.startswith('post'): return 'glassframe'
    if p=='seat': return 'seat'
    if p=='controls': return 'console'
    if p in('engine',): return 'steel'
    if p=='mast': return 'darksteel'
    if p in('rotor','rotor_cross','tail_rotor'): return 'black'
    if p=='winch_cable': return 'black'
    if p=='winch_hook': return 'chrome'
    if p.startswith('headlight'): return 'light'
    if p=='tail_light': return 'red'
    raise Exception(p)

def sub_mat(g):
    p=g['part']
    if p in('hull','hull_top','tower','nose'): return 'yellow'
    if p in('keel','tower_rim','fin_left','fin_right','rudder','shaft'): return 'darksteel'
    if p=='stripe': return 'hazard'
    if p=='periscope': return 'steel'
    if p=='propeller': return 'chrome'
    if p=='drill': return 'drill'
    if p=='drill_tip': return 'drilltip'
    if p.startswith('headlight'): return 'light'
    raise Exception(p)

T='src/main/resources/assets/factorycraft/textures/entity/'
J='src/main/java/com/factorycraft/'
build(J+'ExcavatorModel.java',exc_mat,T+'excavator.png')
build(J+'TunnelerModel.java',tun_mat,T+'tunneler.png')

build(J+'DumpTruckModel.java',truck_mat,T+'dump_truck.png')
build(J+'CargoWagonModel.java',wagon_mat,T+'cargo_wagon.png')
build(J+'PaverModel.java',paver_mat,T+'paver.png')
build(J+'HarvesterModel.java',harvester_mat,T+'harvester.png')
build(J+'CraneModel.java',crane_mat,T+'crane.png')
build(J+'BulldozerModel.java',bulldozer_mat,T+'bulldozer.png')
build(J+'TankerModel.java',tanker_mat,T+'tanker.png')
build(J+'TowerCraneModel.java',tower_mat,T+'tower_crane.png')
build(J+'ForesterModel.java',forester_mat,T+'forester.png')
build(J+'LocomotiveModel.java',loco_mat,T+'locomotive.png')
build(J+'DroneModel.java',drone_mat,T+'drone.png')
