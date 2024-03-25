package com.teammoeg.frostedheart.util.io.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class DataOps implements DynamicOps<Object> {
	private static class LBuilder implements ListBuilder<Object> {
        private DataResult<List<Object>> list=DataResult.success(new ArrayList<>());
        DataOps ops;
        public LBuilder(final DataOps ops) {
            this.ops = ops;
        }

        @Override
        public LBuilder add(final DataResult<Object> value) {
        	list.apply2stable(List::add, value);
            return this;
        }

        @Override
        public LBuilder add(final Object value) {
        	list.map(t->t.add(value));
            return this;
        }

        @Override
        public DataResult<Object> build(final Object prefix) {
            final DataResult<Object> result = list.flatMap(b -> ops.mergeToList(prefix, b));
            list = DataResult.success(new ArrayList<>(), Lifecycle.stable());
            return result;
        }

        @Override
        public LBuilder mapError(final UnaryOperator<String> onError) {
        	list = list.mapError(onError);
            return this;
        }

        @Override
        public DataOps ops() {
            return ops;
        }

        @Override
        public LBuilder withErrorsFrom(final DataResult<?> result) {
        	list = list.flatMap(r -> result.map(v -> r));
            return this;
        }
    }
	private static class MBuilder implements RecordBuilder<Object>{
		DataOps ops;
		DataResult<Map<Object,Object>> map;
		public MBuilder(DataOps ops) {
			super();
			this.ops = ops;
			map=DataResult.success(new HashMap<>());
		}

		@Override
		public MBuilder add(DataResult<Object> key, DataResult<Object> value) {
			map.ap(key.apply2stable((k, v) -> b -> b.put(k, v), value));
			return this;
		}
		@Override
		public MBuilder add(Object key, DataResult<Object> value) {
			map.apply2stable((o,r)->o.put(key,r), value);
			return this;
		}

		@Override
		public MBuilder add(Object key, Object value) {
			map.map(o->o.put(key, value));
			return this;
		}

		@Override
		public DataResult<Object> build(Object prefix) {
            final DataResult<Object> result = map.flatMap(b -> ops.mergeToMap(prefix, b));
            map = DataResult.success(new HashMap<>(), Lifecycle.stable());
            return result;
		}
       @Override
	public MBuilder mapError(final UnaryOperator<String> onError) {
		map = map.mapError(onError);
	    return this;
	}

        @Override
		public DynamicOps<Object> ops() {
			return ops;
		}

        @Override
        public MBuilder setLifecycle(final Lifecycle lifecycle) {
            map = map.setLifecycle(lifecycle);
            return this;
        }

		@Override
		    public MBuilder withErrorsFrom(final DataResult<?> result) {
		        map.flatMap(v -> result.map(r -> v));
		        return this;
		    }
		
	}
	private static class MLike implements MapLike<Object>{
		Map<Object,Object> map;
		
		public MLike(Map<Object, Object> map) {
			super();
			this.map = map;
		}

		@Override
		public Stream<Pair<Object, Object>> entries() {
			return map.entrySet().stream().map(t->Pair.of(t.getKey(), t.getValue()));
		}

		@Override
		public Object get(Object key) {
			return map.get(key);
		}

		@Override
		public Object get(String key) {
			return map.get(key);
		}
		
	}
	public static final DataOps INSTANCE=new DataOps(false);

	public static final DataOps COMPRESSED=new DataOps(true);

	public static final Object NULLTAG=new Object() {
		public String toString() {
			return "nulltag";
		}
	};
	boolean compress;
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
	public DataOps(boolean compress) {
		super();
		this.compress = compress;
	}

	private <T> DataResult<T> cast(Class<T> type,Object input) {
		if(type.isInstance(input))
			return DataResult.success((T)input);
		return DataResult.error("Not a "+type.getSimpleName());
	}

	@Override
	public boolean compressMaps() {
		return compress;
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
	@Override
	public Object createList(Stream<Object> input) {
		//System.out.println("crlist");
		return input.collect(Collectors.toList());
	}

	@Override
	public Object createMap(Map<Object, Object> map) {
		return new HashMap<>(map);
	}

	@Override
	public Object createMap(Stream<Pair<Object, Object>> map) {
		return map.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
	}

	@Override
	public Object createNumeric(Number i) {
		return i;
	}

	@Override
	public Object createString(String value) {
		return value;
	}


	@Override
	public Object empty() {
		return NULLTAG;
	}

	@Override
	public Object emptyList() {
		return new ArrayList<>();
	}

	@Override
	public Object emptyMap() {
		return new HashMap<>();
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
		}
		return DataResult.error("Not a byte array");
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
			
		}
		return DataResult.error("Not a int array");
	}
	
	@Override
	public DataResult<IntStream> getIntStream(Object input) {
		return getIntArray(input).map(IntStream::of);
	}

	@Override
	public DataResult<Consumer<Consumer<Object>>> getList(Object input) {
		return cast(List.class,input).map(t->t::forEach);
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
			
		}
		return DataResult.error("Not a long array");
	}
	
	@Override
	public DataResult<LongStream> getLongStream(Object input) {
		return getLongArray(input).map(LongStream::of);
	}
	@Override
	public DataResult<MapLike<Object>> getMap(Object input) {
		return cast(Map.class,input).map(o->new MLike(o));
	}
	@Override
	public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(Object input) {
		return ((DataResult<Map<Object,Object>>)(DataResult)cast(Map.class,input))
			.map(s -> (c -> s.entrySet().forEach(p -> c.accept(p.getKey(), p.getValue()))));
	}

	@Override
	public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
		return ((DataResult<Map<Object,Object>>)(DataResult)cast(Map.class,input)).map(t->t.entrySet().stream().map(o->Pair.of(o.getKey(), o.getValue())));
	}

	@Override
	public DataResult<Number> getNumberValue(Object input) {
		return cast(Number.class,input);
	}
	
	@Override
	public Number getNumberValue(Object input, Number defaultValue) {
		return cast(Number.class,input).result().orElse(defaultValue);
	}

	@Override
	public DataResult<Stream<Object>> getStream(Object input) {
		return cast(List.class,input).map(t->t.stream());
	}


	@Override
	public DataResult<String> getStringValue(Object input) {
		return cast(String.class,input);
	}
	@Override
	public ListBuilder<Object> listBuilder() {
		return new LBuilder(this);
	}
	@Override
	public RecordBuilder<Object> mapBuilder() {
		return new MBuilder(this);
	}
	@Override
	public DataResult<Object> mergeToList(Object list, List<Object> values) {
		if(list instanceof List) {
			List<Object> li=(List<Object>) list;
			li.addAll(values);
		}else if(list==NULLTAG||list==null) {
			return DataResult.success(values);
		}
		return DataResult.error("Not a Map or Empty");
	}

	@Override
	public DataResult<Object> mergeToList(Object list, Object value) {
		//System.out.println(list);
		if(list==NULLTAG||list==null) {
			return DataResult.success(Stream.of(value).collect(Collectors.toList()));
		};
		DataResult<List> ret=cast(List.class,list);
		ret.result().ifPresent(t->t.add(value));
		return (DataResult)ret;
	}

	@Override
	public DataResult<Object> mergeToMap(Object map, Map<Object, Object> values) {
		if(map instanceof Map) {
			Map<Object, Object> li=(Map<Object, Object>) map;
			li.putAll(values);
		}else if(map==NULLTAG||map==null) {
			return DataResult.success(values);
		}
		return DataResult.error("Not a Map or Empty");
	}

	@Override
	public DataResult<Object> mergeToMap(Object map, MapLike<Object> values) {
		if(values instanceof MLike) {
			return this.mergeToMap(map, ((MLike)values).map);
		}
		return DynamicOps.super.mergeToMap(map, values);
	}

	@Override
	public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
		if(map==NULLTAG||map==null) {
			return DataResult.success(Stream.of(Pair.of(key,value)).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
		};
		DataResult<Map> ret=cast(Map.class,map);
		ret.result().ifPresent(t->t.put(key, value));
		return (DataResult)ret;
	}
	@Override
	public DataResult<Object> mergeToPrimitive(Object prefix, Object value) {
        if (prefix==NULLTAG||prefix==null) {
            return DataResult.error("Do not know how to append a primitive value " + value + " to " + prefix, value);
        }
        return DataResult.success(value);
	}
	@Override
	public Object remove(Object input, String key) {
		return cast(Map.class,input).result().map(t->t.remove(key)).orElse(NULLTAG);
	}
	
}
