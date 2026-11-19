"""Python-only raster chart; ctypes FreeType draws local Chinese glyphs, no Java or compilation."""
from pathlib import Path
import ctypes as C
import ctypes.util, struct, zlib, math, csv, json
ROOT=Path(__file__).resolve().parent.parent
out=ROOT/'build'
config=json.loads((ROOT/'src/main/resources/data/frostedheart/cultivated_trees/spruce.json').read_text())
W,H=2100,1530
pixels=bytearray([255])*(W*H*3)
INK=(31,41,55);MUTED=(90,100,115)
COLORS=[(65,75,90),(210,125,15),(0,155,110),(40,100,225)]
def pixel(x,y,c,alpha=255):
 if 0<=x<W and 0<=y<H:
  at=(y*W+x)*3
  if alpha==255:pixels[at:at+3]=bytes(c)
  else:
   for k in range(3):pixels[at+k]=(pixels[at+k]*(255-alpha)+c[k]*alpha+127)//255

def rect(x,y,w,h,c):
 block=bytes(c)*w
 for yy in range(max(0,y),min(H,y+h)):
  at=(yy*W+x)*3;pixels[at:at+len(block)]=block

def line(x1,y1,x2,y2,c,width=1):
 n=max(1,math.ceil(max(abs(x2-x1),abs(y2-y1))*2))
 for i in range(n+1):
  x=round(x1+(x2-x1)*i/n);y=round(y1+(y2-y1)*i/n)
  for dy in range(-width,width+1):
   for dx in range(-width,width+1):
    if dx*dx+dy*dy<=width*width:pixel(x+dx,y+dy,c)
# FreeType is invoked as a shared library through Python, without compiling anything.
class Generic(C.Structure):_fields_=[('data',C.c_void_p),('finalizer',C.c_void_p)]
class Vector(C.Structure):_fields_=[('x',C.c_long),('y',C.c_long)]
class Bitmap(C.Structure):_fields_=[('rows',C.c_uint),('width',C.c_uint),('pitch',C.c_int),('buffer',C.POINTER(C.c_ubyte)),('num_grays',C.c_ushort),('pixel_mode',C.c_ubyte),('palette_mode',C.c_ubyte),('palette',C.c_void_p)]
class Slot(C.Structure):_fields_=[('library',C.c_void_p),('face',C.c_void_p),('next',C.c_void_p),('glyph_index',C.c_uint),('generic',Generic),('metrics',C.c_long*8),('linearHoriAdvance',C.c_long),('linearVertAdvance',C.c_long),('advance',Vector),('format',C.c_uint),('bitmap',Bitmap),('bitmap_left',C.c_int),('bitmap_top',C.c_int)]
class Face(C.Structure):_fields_=[('num_faces',C.c_long),('face_index',C.c_long),('face_flags',C.c_long),('style_flags',C.c_long),('num_glyphs',C.c_long),('family_name',C.c_char_p),('style_name',C.c_char_p),('num_fixed_sizes',C.c_int),('available_sizes',C.c_void_p),('num_charmaps',C.c_int),('charmaps',C.c_void_p),('generic',Generic),('bbox',C.c_long*4),('units_per_EM',C.c_ushort),('ascender',C.c_short),('descender',C.c_short),('height',C.c_short),('max_advance_width',C.c_short),('max_advance_height',C.c_short),('underline_position',C.c_short),('underline_thickness',C.c_short),('glyph',C.POINTER(Slot))]
ft=C.CDLL(ctypes.util.find_library('freetype'))
ft.FT_Init_FreeType.argtypes=[C.POINTER(C.c_void_p)]
ft.FT_New_Face.argtypes=[C.c_void_p,C.c_char_p,C.c_long,C.POINTER(C.POINTER(Face))]
ft.FT_Set_Pixel_Sizes.argtypes=[C.POINTER(Face),C.c_uint,C.c_uint]
ft.FT_Load_Char.argtypes=[C.POINTER(Face),C.c_ulong,C.c_int]
lib=C.c_void_p();assert ft.FT_Init_FreeType(C.byref(lib))==0
face=C.POINTER(Face)();assert ft.FT_New_Face(lib,b'/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc',0,C.byref(face))==0
cache={}
def glyph(ch,size):
 key=(ch,size)
 if key not in cache:
  assert ft.FT_Set_Pixel_Sizes(face,0,size)==0
  assert ft.FT_Load_Char(face,ord(ch),4)==0
  s=face.contents.glyph.contents;b=s.bitmap
  bitmap=bytes(b.buffer[:b.rows*abs(b.pitch)]) if b.rows else b''
  cache[key]=(s.advance.x/64,s.bitmap_left,s.bitmap_top,b.width,b.rows,b.pitch,bitmap)
 return cache[key]
def text(s,x,y,size=23,c=INK,center=False):
 if center:x-=sum(glyph(ch,size)[0] for ch in s)/2
 for ch in s:
  adv,left,top,w,h,pitch,b=glyph(ch,size)
  for yy in range(h):
   for xx in range(w):
    a=b[yy*pitch+xx]
    if a:pixel(round(x)+left+xx,y-top+yy,c,a)
  x+=adv

def f32(x):return struct.unpack('f',struct.pack('f',x))[0]
def budget(f):
 y=config['yield'];m=config['temperature']['minimum_yield_factor']
 return max(0,math.floor(y['min_height']*f/m if f<=m else y['min_height']+(y['max_height']-y['min_height'])*(f-m)/(1-m)))
def ledger(t,kind,grade):
 t=f32(t);tc=config['temperature'];mn=tc['min']-(5*[1,2,4][grade] if kind==2 else 0);mx=tc['max']
 if t<mn:q=.5*max(0,1-f32(mn-t)/30)
 elif t>mx:q=.5*max(0,1-f32(t-mx)/30)
 elif t<15:q=f32(.5+f32(f32(.5*f32(t-mn))/f32(15-mn)))
 elif t>25:q=f32(.5+f32(f32(.5*f32(mx-t))/10))
 else:q=1
 if q<=0:return 0,0
 if kind==1:q*=1+([2,3,4][grade]-1)*.2
 step=[32,64,128][grade] if kind==3 else 16
 credit=(256//step)*math.floor(step*q+.5)
 if budget(credit/256)<1:
  inc=math.floor(step*q+.5)
  if inc==0:return credit,0
  # Min-height reference is 5, so the first whole trunk requires 26 fixed-point credits.
  credit+=math.ceil((26-credit)/inc)*inc
 return credit,budget(credit/256)

def yields(t,kind,grade):
 credit,height=ledger(t,kind,grade)
 return height,4*height+8 if height>=config['yield']['max_height'] else 0
assert [yields(20,1,g) for g in range(3)]==[(16,72),(19,84),(23,100)]
assert yields(20,0,0)==(13,60)
assert yields(5,0,0)==(5,0)
assert yields(-55,0,0)==(0,0)
text('栽培云杉：温度与木头产出期望',1050,66,40,center=True)
text('单棵与 2×2 巨型对比 ｜ 按当前源码计算，非游戏实测',1050,110,24,MUTED,True)
for i,label in enumerate(['无肥','基础','高级','终极']):
 x=610+245*i;line(x,151,x+50,151,COLORS[i],2);text(label,x+65,160,24)
for a,label in enumerate(['增产肥：有效 ×1.2 / ×1.4 / ×1.6','保温肥：降低下限 5 / 10 / 20°C','加速肥：×2 / ×4 / ×8']):
 text(label,410+a*660,213,23,center=True)
text('单棵（1 株）：产量 = H 块',115,262,27)
text('2×2 巨型（4 株合计）：产量 = 4H+8 块；未达巨型阈值时记 0',115,829,27)
rows=[]
for row,(top,maxy,yticks) in enumerate([(290,32,[0,8,16,24,32]),(855,128,[0,32,64,96,128])]):
 ph=350;pw=590
 for col in range(3):
  left=115+660*col
  rect(left+int(70/130*pw),top,int(10/130*pw),ph,(222,247,233))
  for yy in yticks:
   py=top+ph-yy/maxy*ph
   line(left,py,left+pw,py,(225,229,235),0);text(str(yy),left-43,round(py)+7,20,MUTED)
  for t in [5,35]:
   x=left+int((t+55)/130*pw)
   for yy in range(top,top+ph):
    if (yy-top)%9<4:pixel(x,yy,(170,180,190))
  for k in range(4):
   previous=None
   for i in range(2601):
    t=-55+i*.05;single,giant=yields(t,0 if k==0 else col+1,0 if k==0 else k-1)
    assert giant == 0 or giant == single*4+8
    value=[single,giant][row]
    xy=(left+(t+55)/130*pw,top+ph-value/maxy*ph)
    if previous:line(*previous,*xy,COLORS[k],1)
    previous=xy
    if row==0:rows.append([col+1,k,round(t,2),single,giant,giant/4,4*single])
  line(left,top,left,top+ph,INK,0);line(left,top+ph,left+pw,top+ph,INK,0)
  for t in [-50,-25,0,15,35,65,75]:
   x=left+(t+55)/130*pw;line(x,top+ph,x,top+ph+6,INK,0);text(str(t),x,top+ph+35,20,MUTED,True)
  text('温度（°C）',left+pw/2,top+ph+80,25,center=True)
text('上下两行纵轴均为产量（木头块数），刻度不同；巨型为四株合计，不是每株产量。',1050,1343,24,MUTED,True)
text('恒温、光照及空间充足、肥料持续补足，四株信用一致；逐随机刻记账取整，云杉原版树形无横向木头枝杈。',1050,1385,22,MUTED,True)
text('巨型触发条件为 H≥13；未触发时可单干回退，下行只统计实际巨型产出。绿色底色：理想温区 15～25°C。',1050,1427,22,MUTED,True)
text('公平按株对比：巨型每株平均 = 下行÷4；巨型总产量 = 单棵×4+8；每株平均严格多 2 块，已计入原版顶部三格补齐。',1050,1469,22,MUTED,True)
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
header=struct.pack('>IIBBBBB',W,H,8,2,0,0,0)
raw=b''.join(b'\0'+pixels[y*W*3:(y+1)*W*3] for y in range(H))
png=ROOT/'docs/climate/images/cultivated-spruce-yield-vs-temperature.png'
png.parent.mkdir(parents=True,exist_ok=True)
png.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',header)+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
with (out/'cultivated-spruce-yield-vs-temperature.csv').open('w') as f:
 writer=csv.writer(f);writer.writerow(['fertilizer_type','grade_0_is_none','temperature_c','single_logs','giant_4_saplings_total_logs','giant_per_sapling_logs','four_independent_singles_logs']);writer.writerows(rows)
print('Generated:',png)
print('Reference checks passed; Python only, no compilation or game tests.')
