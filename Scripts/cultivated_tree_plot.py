"""Python-only raster chart; ctypes FreeType draws local Chinese glyphs, no Java or compilation."""
from pathlib import Path
import ctypes as C
import ctypes.util, struct, zlib, math, csv, json
ROOT=Path(__file__).resolve().parent.parent
out=ROOT/'build'
out.mkdir(exist_ok=True)
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


def write_png(path, height=H):
 def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
 header=struct.pack('>IIBBBBB',W,height,8,2,0,0,0)
 raw=b''.join(b'\0'+pixels[y*W*3:(y+1)*W*3] for y in range(height))
 path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',header)+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))

def f32(x):return struct.unpack('f',struct.pack('f',x))[0]
def budget(config,f):
 y=config['yield'];m=config['temperature']['minimum_yield_factor']
 return max(0,math.floor(y['min_height']*f/m if f<=m else y['min_height']+(y['max_height']-y['min_height'])*(f-m)/(1-m)))
def ledger(config,t,kind,grade):
 tc=config['temperature'];t=f32(t);m=tc['minimum_yield_factor']
 mn=f32(tc['min']-(tc['preserved_degrees_per_grade']*[1,2,4][grade] if kind==2 else 0));mx=tc['max'];width=f32(mx-tc['min'])
 if t<mn:q=m*max(0,1-f32(mn-t)/width)
 elif t>mx:q=m*max(0,1-f32(t-mx)/width)
 elif t<tc['ideal_min']:q=f32(m+f32(f32(f32(1-m)*f32(t-mn))/f32(tc['ideal_min']-mn)))
 elif t>tc['ideal_max']:q=f32(m+f32(f32(f32(1-m)*f32(mx-t))/f32(mx-tc['ideal_max'])))
 else:q=1
 if q<=0:return 0
 if kind==1:q*=1+([2,3,4][grade]-1)*.2
 step=[32,64,128][grade] if kind==3 else 16
 total=4*config['growth']['progress_per_stage']*16
 gain=math.floor(step*q+.5);credit=(total//step)*gain
 if budget(config,credit/total)<1:
  if gain==0:return 0
  threshold=math.ceil(total*m/config['yield']['min_height'])
  credit+=math.ceil((threshold-credit)/gain)*gain
 return budget(config,credit/total)

REFERENCE={}
for row in csv.DictReader((ROOT/'docs/climate/validation/native-tree-yield-reference.csv').open()):
 REFERENCE.setdefault(row['shape'],{})[int(row['height'])]=float(row['mean'])
KNOTS=[1,2,4,6,8,12,20,32]
def native_mean(shape,h):
 values=REFERENCE[shape]
 if h<=32:return values[h]
 return values[32]+(h-32)*(values[32]-values[31])
def minimum_logs(shape):
 return native_mean(shape,4) if shape in ['fancy_oak','acacia','cherry','mangrove','tall_mangrove'] else 1

def native_expected(shape,target):
 if shape in ['oak','birch','spruce','jungle_tree_no_vine']:return target
 if shape in ['mega_pine','mega_spruce']:return target
 knots=list(range(1,17))+[20,24,32] if shape=='fancy_oak' else KNOTS
 means=[native_mean(shape,h) for h in knots]
 i=next((j for j in range(1,len(knots)) if target<=means[j]),len(knots)-1)
 h=knots[i-1]+(target-means[i-1])*(knots[i]-knots[i-1])/(means[i]-means[i-1])
 low=max(1,math.floor(h))
 def modeled(n):
  j=next((j for j in range(1,len(knots)) if n<=knots[j]),len(knots)-1)
  return means[j-1]+(n-knots[j-1])*(means[j]-means[j-1])/(knots[j]-knots[j-1])
 span=modeled(low+1)-modeled(low)
 chance=max(0,min(1,(target-modeled(low))/span)) if span>0 else 0
 return native_mean(shape,low)*(1-chance)+native_mean(shape,low+1)*chance

def oak_expected(b):
 if b<=0:return 0
 if b<minimum_logs('fancy_oak'):return b
 fancy=native_expected('fancy_oak',b)
 return fancy if b>6 else .9*b+.1*fancy
