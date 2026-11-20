"""Temperature/yield expectation chart for whole native spruce trees; Python only."""
from cultivated_tree_plot import *
config=json.loads((ROOT/'src/main/resources/data/frostedheart/cultivated_trees/spruce.json').read_text())
def yields(t,kind,grade):
 height=ledger(config,t,kind,grade)
 return height,4*height+8 if height>=config['yield']['max_height'] else 0
assert [yields(20,1,g) for g in range(3)]==[(16,72),(19,84),(23,100)]
assert yields(20,0,0)==(13,60)
assert yields(5,0,0)==(5,0)
assert yields(-55,0,0)==(0,0)
text('栽培云杉：温度与木头产出期望',1050,66,40,center=True)
text('单棵与 2×2 巨型对比 ｜ 按当前源码期望计算，完整保留原版树形',1050,110,24,MUTED,True)
for i,label in enumerate(['无肥','基础','高级','终极']):
 x=610+245*i;line(x,151,x+50,151,COLORS[i],2);text(label,x+65,160,24)
for a,label in enumerate(['增产肥：有效 ×1.2 / ×1.4 / ×1.6','保温肥：降低下限 5 / 10 / 20°C','加速肥：×2 / ×4 / ×8']):
 text(label,410+a*660,213,23,center=True)
text('单棵（1 株）：产量 = H 块',115,262,27)
text('2×2 巨型（4 株合计）：目标期望 = 4H+8 块；未达巨型阈值时记 0',115,829,27)
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
text('公平按株对比：巨型每株平均 = 下行÷4；巨型总产量 = 单棵×4+8；每株平均期望多 2 块；保留原版收尖，单棵实际木头数允许波动。',1050,1469,22,MUTED,True)

png=ROOT/'docs/climate/images/cultivated-spruce-yield-vs-temperature.png'
write_png(png)
with (out/'cultivated-spruce-yield-vs-temperature.csv').open('w') as f:
 writer=csv.writer(f);writer.writerow(['fertilizer_type','grade_0_is_none','temperature_c','single_logs','giant_4_saplings_total_logs','giant_per_sapling_logs','four_independent_singles_logs']);writer.writerows(rows)
print('Generated:',png)
