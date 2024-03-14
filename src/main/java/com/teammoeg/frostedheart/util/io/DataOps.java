package com.teammoeg.frostedheart.util.io;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class DataOps implements DynamicOps<Object> {
	public static final DataOps INSTANCE=new DataOps(false);
	public static final DataOps COMPRESSED=new DataOps(true);

	public DataOps(boolean compress) {
		super();
		this.compress = compress;
	}

	@Override
	public Object empty() {
		return null;
	}

	@Override
	public <U> U convertTo(DynamicOps<U> outOps, Object input) {
		if(input instanceof Byte) {
			return outOps.createByte((byte)input);
		}else if(input instanceof Short) {
			return outOps.createShort((short) input);
		}else if(input instanceof Integer) {
			return outOps.createInt((int) input);
		}else if(input instanceof Long) {
			return outOps.createLong((long) input);
		}else if(input instanceof Float) {
			return outOps.createFloat((float) input);
		}else if(input instanceof Double) {
			return outOps.createDouble((double) input);
		}else if(input instanceof String) {
			return outOps.createString((String) input);
		}else if(input instanceof Map) {
			return outOps.createMap(((Map<Object,Object>)input).entrySet().stream().map(o->Pair.of(this.convertTo(outOps, o.getKey()), this.convertTo(outOps, o.getValue()))));
		}else if(input instanceof List) {
			List<Object> objs=((List<Object>)input);
			Class<?> cls=getElmClass(objs);
			if(cls==Byte.class) {
				return outOps.createByteList(this.getByteBuffer(objs).result().get());
			}else if(cls==Integer.class) {
				return outOps.createIntList(this.getIntStream(objs).result().get());
			}else if(cls==Long.class) {
				return outOps.createLongList(this.getLongStream(objs).result().get());
			}
			return outOps.createList(objs.stream().map(o->this.convertTo(outOps, o)));
		}
		return outOps.empty();
	}
	private <T> DataResult<T> cast(Class<T> type,Object input) {
		if(type.isInstance(input))
			return DataResult.success((T)input);
		return DataResult.error("Not a "+type.getSimpleName());
	}
	public static Class<?> getElmClass(List<Object> objs){
		if(!objs.isEmpty()) {
			Class<?> cls=objs.get(0).getClass();
			for(Object obj:objs) {
				if(!cls.isInstance(obj))
					return null;
			}
			return cls;
		}
		return null;
	}
	@Override
	public DataResult<Number> getNumberValue(Object input) {
		return cast(Number.class,input);
	}

	@Override
	public Object createNumeric(Number i) {
		return i;
	}

	@Override
	public DataResult<String> getStringValue(Object input) {
		return cast(String.class,input);
	}

	@Override
	public Object createString(String value) {
		return value;
	}

	@Override
	public DataResult<Object> mergeToList(Object list, Object value) {
		if(list==null) {
			return DataResult.success(Stream.of(value).collect(Collectors.toList()));
		};
		DataResult<List> ret=cast(List.class,list);
		ret.result().ifPresent(t->t.add(value));
		return (DataResult)ret;
	}

	@Override
	public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
		if(map==null) {
			return DataResult.success(Stream.of(Pair.of(key,value)).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
		};
		DataResult<Map> ret=cast(Map.class,map);
		ret.result().ifPresent(t->t.put(key, value));
		return (DataResult)ret;
	}

	@Override
	public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
		return ((DataResult<Map<Object,Object>>)(DataResult)cast(Map.class,input)).map(t->t.entrySet().stream().map(o->Pair.of(o.getKey(), o.getValue())));
	}

	@Override
	public Object createMap(Stream<Pair<Object, Object>> map) {
		return map.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
	}

	@Override
	public DataResult<Stream<Object>> getStream(Object input) {
		return cast(List.class,input).map(t->t.stream());
	}

	@Override
	public Object createList(Stream<Object> input) {
		return input.collect(Collectors.toList());
	}

	@Override
	public Object remove(Object input, String key) {
		return cast(Map.class,input).result().map(t->t.remove(key)).orElse(null);
	}
	
	public DataResult<byte[]> getByteArray(Object input) {
		DataResult<List> dr=cast(List.class,input);
		if(dr.result().isPresent()) {
			List res=dr.result().get();
			if(getElmClass(res)==Byte.class) {
				int siz=res.size();
				byte[] bs=new byte[siz];
				for(int i=0;i<siz;i++)
					bs[i]=(Byte)res.get(i);
				return DataResult.success(bs);
			}
			return DataResult.error("Not a byte array");
		}
		return DataResult.error("Not a List");
	}
	
	@Override
	public DataResult<ByteBuffer> getByteBuffer(Object input) {
		return getByteArray(input).map(ByteBuffer::wrap);
	}
	
	public DataResult<int[]> getIntArray(Object input) {
		DataResult<List> dr=cast(List.class,input);
		if(dr.result().isPresent()) {
			List res=dr.result().get();
			if(getElmClass(res)==Integer.class) {
				int siz=res.size();
				int[] bs=new int[siz];
				for(int i=0;i<siz;i++)
					bs[i]=(Integer)res.get(i);
				return DataResult.success(bs);
			}
			return DataResult.error("Not a int array");
		}
		return DataResult.error("Not a List");
	}

	@Override
	public DataResult<IntStream> getIntStream(Object input) {
		return getIntArray(input).map(IntStream::of);
	}

	public DataResult<long[]> getLongArray(Object input) {
		DataResult<List> dr=cast(List.class,input);
		if(dr.result().isPresent()) {
			List res=dr.result().get();
			if(getElmClass(res)==Long.class) {
				int siz=res.size();
				long[] bs=new long[siz];
				for(int i=0;i<siz;i++)
					bs[i]=(Long)res.get(i);
				return DataResult.success(bs);
			}
			return DataResult.error("Not a long array");
		}
		return DataResult.error("Not a List");
	}
	
	@Override
	public DataResult<LongStream> getLongStream(Object input) {
		return getLongArray(input).map(LongStream::of);
	}
	boolean compress;
	@Override
	public boolean compressMaps() {
		return compress;
	}
	

}
