package com.teammoeg.frostedheart.util.io.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.teammoeg.frostedheart.util.io.SerializeUtil;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;

public class ObjectWriter {
	private static class TypedValue{
		int type;
		Object value;
		public TypedValue(int type, Object value) {
			super();
			this.type = type;
			this.value = value;
		}
		public TypedValue(int type) {
			super();
			this.type = type;
			this.value=DataOps.NULLTAG;
		}
	}
	public ObjectWriter() {
	}
	public static TypedValue getTyped(Object input) {
		if(input instanceof Byte) {
			return new TypedValue(1,input);
		}else if(input instanceof Short) {
			return new TypedValue(2,input);
		}else if(input instanceof Integer) {
			return new TypedValue(3,input);
		}else if(input instanceof Long) {
			return new TypedValue(4,input);
		}else if(input instanceof Float) {
			return new TypedValue(5,input);
		}else if(input instanceof Double) {
			return new TypedValue(6,input);
		}else if(input instanceof String) {
			return new TypedValue(7,input);
		}else if(input instanceof Map) {
			return new TypedValue(8,input);
		}else if(input instanceof List) {
			Class<?> cls=DataOps.getElmClass(((List<Object>)input));
			if(cls==Byte.class) {
				return new TypedValue(9,input);
			}else if(cls==Integer.class) {
				return new TypedValue(10,input);
			}else if(cls==Long.class) {
				return new TypedValue(11,input);
			}else if(cls==String.class) {
				return new TypedValue(12,input);
			}else if(cls==Map.class) {
				return new TypedValue(13,input);
			}else {
				return new TypedValue(14,input);
			}
		}else {
			return new TypedValue(0,input);
		}
	}
	public static void writeTyped(PacketBuffer pb,TypedValue input) {
		switch(input.type) {
		case 1:pb.writeByte((Byte)input.value);break;
		case 2:pb.writeShort((Short) input.value);break;
		case 3:pb.writeVarInt((Integer) input.value);break;
		case 4:pb.writeLong((Long) input.value);break;
		case 5:pb.writeFloat((Float) input.value);break;
		case 6:pb.writeDouble((Double) input.value);break;
		case 7:pb.writeString((String) input.value);break;
		case 8:SerializeUtil.writeEntry(pb, ((Map<Object,Object>)input.value),(t,p)->{
			TypedValue key   = getTyped(t.getKey());
			TypedValue value = getTyped(t.getValue());
			pb.writeByte((key.type<<4)+value.type);
			writeTyped(pb,key);
			writeTyped(pb,value);
		});break;
		case 9:byte[] bs=DataOps.INSTANCE.getByteArray(input).result().get();
		pb.writeByteArray(bs);break;
		case 10:SerializeUtil.writeList(pb, ((List<Integer>)input.value), (t,p)->p.writeVarInt(t));break;
		case 11:SerializeUtil.writeList(pb, ((List<Long>)input.value), (t,p)->p.writeLong(t));break;
		case 12:SerializeUtil.writeList(pb, ((List<String>)input.value), (t,p)->p.writeString(t));break;
		case 13:SerializeUtil.writeList(pb, ((List<Map>)input.value), (t,p)->writeTyped(p,new TypedValue(8,t)));break;
		case 14:{
			List<Object> obj=(List<Object>) input.value;
			List<TypedValue> typed=obj.stream().map(o->getTyped(o)).collect(Collectors.toList());
			pb.writeVarInt(typed.size());
			
			if(typed.size()%2==1)
				typed.add(new TypedValue(0));
			for(int i=0;i<(typed.size())/2;i++) {
	        	pb.writeByte((typed.get(i*2).type<<4)+typed.get(i*2+1).type);
	        }
	        typed.forEach(t->writeTyped(pb,t));
		}break;
		}
	}
    public static Object readWithType(int type,PacketBuffer pb) {
    	switch(type) {
    	case 1:return pb.readByte();
    	case 2:return pb.readShort();
    	case 3:return pb.readVarInt();
    	case 4:return pb.readLong();
    	case 5:return pb.readFloat();
    	case 6:return pb.readDouble();
    	case 7:return pb.readString();
    	case 8:return SerializeUtil.readEntry(pb, new HashMap<>(),(p,c)->{
    		int byt=pb.readByte();
    		Object key=readWithType((byt>>4)&15,pb);
    		Object value=readWithType(byt&15,pb);
    		c.accept(key, value);
    	})
    	;
    	case 9:return pb.readByteArray();
    	case 10:return SerializeUtil.readList(pb, PacketBuffer::readVarInt);
    	case 11:return SerializeUtil.readList(pb, PacketBuffer::readLong);
    	case 12:return SerializeUtil.readList(pb, PacketBuffer::readString);
    	case 13:return SerializeUtil.readList(pb, p->readWithType(8,p));
    	case 14:{
			List<Object> obj=new ArrayList<>();
			int size=pb.readVarInt();
			ByteBuf crnbytes=pb.readBytes((size+1)/2);
			for(int i=0;i<size;i++) {
				if(i%2==1) {
					obj.add(readWithType(crnbytes.getByte(i/2)&15,pb));
				}else {
					obj.add(readWithType(crnbytes.getByte(i/2)>>4,pb));
				}
			}
			return obj;
		}
    	}
    	return DataOps.NULLTAG;
    }
    public static void writeObject(PacketBuffer pb,Object input) {
    	TypedValue value=getTyped(input);
    	pb.writeByte(value.type);
    	writeTyped(pb,value);
    }
    public static Object readObject(PacketBuffer pb) {
    	return readWithType(pb.readByte(),pb);
    }
}
