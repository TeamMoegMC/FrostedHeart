"""Whole native oak wood expectation, by fertilizer type and grade; Python only."""
from cultivated_tree_plot import *
config=json.loads((ROOT/'src/main/resources/data/frostedheart/cultivated_trees/oak.json').read_text())
text('栽培橡树：温度与总木头产出期望',1050,70,40,center=True)
text('完整原版主干＋枝杈 ｜ 原版生成器抽样校准，允许实际木头数波动',1050,117,25,MUTED,True)
for i,label in enumerate(['无肥','基础','高级','终极']):
 x=610+245*i;line(x,162,x+50,162,COLORS[i],2);text(label,x+65,171,24)
rows=[]
for col,label in enumerate(['增产肥：有效 ×1.2 / ×1.4 / ×1.6','保温肥：降低下限 5 / 10 / 20°C','加速肥：×2 / ×4 / ×8']):
 left=115+660*col;top=280;pw=590;ph=420;maxy=28
 text(label,left+pw/2,238,24,center=True)
 rect(left+int(70/130*pw),top,int(10/130*pw),ph,(222,247,233))
 for value in [0,7,14,21,28]:
  py=top+ph-value/maxy*ph;line(left,py,left+pw,py,(225,229,235),0);text(str(value),left-42,round(py)+7,21,MUTED)
 for k in range(4):
  previous=None
  for i in range(2601):
   t=-55+i*.05;kind=0 if k==0 else col+1;grade=0 if k==0 else k-1
   b=ledger(config,t,kind,grade);value=oak_expected(b)
   xy=(left+(t+55)/130*pw,top+ph-value/maxy*ph)
   if previous:line(*previous,*xy,COLORS[k],1)
   previous=xy;rows.append([col+1,k,round(t,2),b,value])
  for t in [5,35]:
   x=left+(t+55)/130*pw
   for y in range(top,top+ph):
    if (y-top)%9<4:pixel(round(x),y,(170,180,190))
 line(left,top,left,top+ph,INK,0);line(left,top+ph,left+pw,top+ph,INK,0)
 for t in [-50,-25,0,15,35,65,75]:
  x=left+(t+55)/130*pw;line(x,top+ph,x,top+ph+6,INK,0);text(str(t),x,top+ph+36,20,MUTED,True)
 text('温度（°C）',left+pw/2,top+ph+85,25,center=True)
text('纵轴：每株总木头产出期望（块，主干＋全部原版枝杈）；三档均为实线。',1050,850,24,MUTED,True)
text('恒温、空间与光照充足，肥料持续补足；逐随机刻信用取整，零产量温区始终不生成。',1050,896,23,MUTED,True)
text('预算＞6使用原版大型橡树；不裁枝、不补柱。曲线基于原版128种子/尺寸的均值，不是每棵固定产量。',1050,940,22,MUTED,True)
assert [ledger(config,20,1,g) for g in range(3)]==[15,18,22]
assert abs(oak_expected(12)-12)<.01
png=ROOT/'docs/climate/images/cultivated-oak-yield-vs-temperature.png';write_png(png,980)
with (out/'cultivated-oak-yield-vs-temperature.csv').open('w') as f:
 writer=csv.writer(f);writer.writerow(['fertilizer_type','grade_0_is_none','temperature_c','target_expected_logs','reference_expected_logs']);writer.writerows(rows)
print('Generated:',png)
