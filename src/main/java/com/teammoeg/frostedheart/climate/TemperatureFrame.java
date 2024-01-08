package com.teammoeg.frostedheart.climate;

/**
 * A class to represent temperature change, basically like a key frame.
 * A frame class means, the temperature increase in warm or decrease in cold. If it just goes back to calm, the increase or decrease would both be false.
 * It stores hours from now and temperature level it transform to.
 */
public class TemperatureFrame{
	public enum FrameType{
		NOP,
		INCRESING,
		DECREASING,
		SNOWING,
		STORMING,
		RETREATING,
		CLOUDY;
		public boolean isIncresingEvent() {
			return this==INCRESING;
		}
		public boolean isDecresingEvent() {
			return this==DECREASING;
		}
		public boolean isWeatherEvent() {
			return this.ordinal()>=3;
		}
	}
	public final TemperatureFrame.FrameType type;
	public final short dhours;
	public final byte toState;
	public TemperatureFrame(TemperatureFrame.FrameType type, int dhours, byte toState) {
		super();
		this.type=type;
		this.dhours = (short) dhours;
		this.toState = toState;
	}
	public static TemperatureFrame unpack(int val) {
		if(val==0)return null;
		return new TemperatureFrame(val);
	}
	private TemperatureFrame(int packed) {
		super();
		this.type=FrameType.values()[packed&0x7F];
		this.dhours = (short) ((packed>>16)&0xFFFF);
		this.toState = (byte) ((packed>>8)&0xFF);
	}
	public static TemperatureFrame increase(int hour,int to) {
		return new TemperatureFrame(FrameType.INCRESING,hour,(byte)to);
	}
	public static TemperatureFrame decrease(int hour,int to) {
		return new TemperatureFrame(FrameType.DECREASING,hour,(byte)to);
	}
	public static TemperatureFrame weather(int hour,ClimateType type,int to) {
		switch(type) {
		case SNOW_BLIZZARD:
		case BLIZZARD:return blizzard(hour,to); 
		case SUN:return sun(hour,to);
		case SNOW:return snow(hour,to);
		case CLOUDY:return cloud(hour,to);
		default:return calm(hour,to);
		}

	}
	public static TemperatureFrame blizzard(int hour,int to) {
		return new TemperatureFrame(FrameType.STORMING,hour,(byte)to);
	}
	public static TemperatureFrame snow(int hour,int to) {
		return new TemperatureFrame(FrameType.SNOWING,hour,(byte)to);
	}
	public static TemperatureFrame cloud(int hour,int to) {
		return new TemperatureFrame(FrameType.CLOUDY,hour,(byte)to);
	}
	public static TemperatureFrame sun(int hour,int to) {
		return new TemperatureFrame(FrameType.RETREATING,hour,(byte)to);
	}
	public static TemperatureFrame calm(int hour,int to) {
		return new TemperatureFrame(FrameType.NOP,hour,(byte)to);
	}
	public int pack() {
		int ret=0;
		ret|=type.ordinal();
		ret|=0x80;//exist flag
		ret|=toState<<8;
		ret|=dhours<<16;
		return ret;
	}
	/**
	 * Serialize but without hour to reduce network cost
	 * */
	public short packNoHour() {
		short ret=0;
		ret|=type.ordinal();
		ret|=0x80;//exist flag
		ret|=toState<<8;
		return ret;
	}
	@Override
	public String toString() {
		return "[type=" + type + ", dhours="
				+ dhours + ", toState=" + toState + "]";
	}
}