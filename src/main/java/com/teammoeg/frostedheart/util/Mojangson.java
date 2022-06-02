package com.teammoeg.frostedheart.util;

import java.util.Iterator;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CollectionNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.FloatNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.nbt.ShortNBT;
import net.minecraft.nbt.StringNBT;

public class Mojangson {
	
	private Mojangson() {
	}
	public static String writeNBT(INBT nbt) {
		if(nbt==null)return null;
		switch(nbt.getId()) {
		case 1:return ((ByteNBT)nbt).getByte()+"b";
		case 2:return ((ShortNBT)nbt).getShort()+"s";
		case 3:return String.valueOf(((IntNBT)nbt).getInt());
		case 4:return ((LongNBT)nbt).getLong()+"l";
		case 5:return ((FloatNBT)nbt).getFloat()+"f";
		case 6:return String.valueOf(((DoubleNBT)nbt).getDouble());
		case 8:return quoteString(((StringNBT)nbt).getString());
		case 9:{//List
			Iterator<INBT> it=((ListNBT)nbt).iterator();
			StringBuilder sb=new StringBuilder("[");
			if(it.hasNext())
				sb.append(writeNBT(it.next()));
			while(it.hasNext()) {
				sb.append(",");
				sb.append(writeNBT(it.next()));
			}
			sb.append(']');
			return sb.toString();
		}
		case 10:{//Compound;
			Iterator<String> it=((CompoundNBT)nbt).keySet().iterator();
			StringBuilder sb=new StringBuilder("{");
			if(it.hasNext())
				sb.append(writeKV((CompoundNBT) nbt,it.next()));
			while(it.hasNext()) {
				sb.append(",");
				sb.append(writeKV((CompoundNBT) nbt,it.next()));
			}
			sb.append('}');
			return sb.toString();
		}
		case 7:return "[B;"+writeArray((ByteArrayNBT)nbt)+"]";//Byte Array
		case 11:return "[I;"+writeArray((ByteArrayNBT)nbt)+"]";//Int Array
		case 12:return "[L;"+writeArray((ByteArrayNBT)nbt)+"]";//Long Array
		}
		return null;
	}
	public static <T extends INBT> String writeArray(CollectionNBT<T> ar) {
		Iterator<T> it=ar.iterator();
		StringBuilder sb=new StringBuilder();
		if(it.hasNext())
			sb.append(writeNBT(it.next()));
		while(it.hasNext()) {
			sb.append(",");
			sb.append(writeNBT(it.next()));
		}
		return sb.toString();
	}
	public static String writeKV(CompoundNBT nbt,String k) {
		return quoteString(k)+":"+writeNBT(nbt.get(k));
	}
	public static String quoteString(String in) {
		if(!in.contains("\""))
			return "\""+in+"\"";
		if(!in.contains("'"))
			return "'"+in+"'";
		return "\""+in.replaceAll("\"","\\\"")+"\"";
		
	}
}
