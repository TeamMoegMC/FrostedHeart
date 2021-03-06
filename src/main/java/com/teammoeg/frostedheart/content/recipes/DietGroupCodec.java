package com.teammoeg.frostedheart.content.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.PacketBuffer;
import top.theillusivec4.diet.api.IDietGroup;
import top.theillusivec4.diet.common.group.DietGroups;

public class DietGroupCodec {
	private static List<IDietGroup> codecs=new ArrayList<>();
	public final static String[] groups=new String[] {"grains","vegetables","plant_oil","proteins","sugars","vitamin"};
	private DietGroupCodec() {
	}
	public static void clearCodec() {
		codecs.clear();
	}
	public static void genCodec() {
		codecs.clear();
		Set<IDietGroup> idgs=DietGroups.get();
		for(int i=0;i<groups.length;i++) {
			for(IDietGroup idg:idgs)
				if(idg.getName().equalsIgnoreCase(groups[i])) {
					codecs.add(idg);
					break;
				}
		}
	}
	public static IDietGroup getGroup(int i) {
		if(codecs.isEmpty())
			genCodec();
		return codecs.get(i);
	}
	public static IDietGroup getGroup(String i) {
		if(codecs.isEmpty())
			genCodec();
		return codecs.get(getId(i));
	}
	public static int getId(IDietGroup idg) {
		if(codecs.isEmpty())
			genCodec();
		return codecs.indexOf(idg);
	}
	public static int getId(String idg) {
		for(int i=0;i<groups.length;i++)
			if(idg.equals(groups[i]))
				return i;
		return -1;
	}
	public static void write(PacketBuffer pb,Map<String,Float> f) {
		pb.writeVarInt(f.size());
		if(!f.isEmpty())
			f.entrySet().forEach(e->{
				pb.writeVarInt(getId(e.getKey()));
				pb.writeFloat(e.getValue());
			});
	}
	public static Map<String,Float> read(PacketBuffer pb) {
		int size=pb.readVarInt();
		Map<String,Float> m=new HashMap<>();
		if(size>0)
			for(int i=0;i<size;i++) {
				m.put(groups[pb.readVarInt()],pb.readFloat());
			}
		return m;
	}
}
