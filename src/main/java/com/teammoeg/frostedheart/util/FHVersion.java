package com.teammoeg.frostedheart.util;

import java.util.Arrays;

public class FHVersion {
	private enum SubType{
		pre,
		rc,
		stable,
		hf
	}
	private enum EqualState{
		lt(true,false),
		eq(true,true),
		gt(true,true),
		und(false,true);
		static EqualState of(int min) {
			if(min>0)return gt;
			if(min<0)return lt;
			return eq;
		}
		final boolean isValid;
		final boolean isLater;
		private EqualState(boolean isValid, boolean isLater) {
			this.isValid = isValid;
			this.isLater = isLater;
		}
		
	}
	private static class SubVersion{
		String subtype;
		MajorVersion subversion;
		public SubVersion(String subtype,MajorVersion subversion) {
			this.subtype = subtype;
			this.subversion = subversion;
		}
		public SubType getType() {
			for(SubType type:SubType.values()) {
				if(type.name().equalsIgnoreCase(subtype))
					return type;
			}
			return null;
		}
		public EqualState laterThan(SubVersion other) {
			SubType ttype=getType();
			SubType otype=other.getType();
			if(ttype==otype) {
				return subversion.laterThan(other.subversion);
			}
			return ttype==null?EqualState.gt:(otype==null?EqualState.lt:EqualState.of(ttype.ordinal()-otype.ordinal()));
		}
		public static SubVersion parse(String v) {
			StringBuilder st=new StringBuilder();
			int i;
			for(i=0;i<v.length();i++) {
				char c=v.charAt(i);
				if(c>='0'&&c<='9')break;//number start
				st.append(c);
			}
			MajorVersion ver;
			if(i!=v.length()) {
				ver=MajorVersion.parse(v.substring(i));
			}else ver=MajorVersion.empty;
			return new SubVersion(st.toString(),ver);
		}
		@Override
		public String toString() {
			return "-" + subtype + subversion.toString();
		}
	}
	private static class MajorVersion{
		private static final MajorVersion empty=new MajorVersion(null);
		private int[] vers;
		public MajorVersion(int[] vers) {
			this.vers = vers;
		}
		public boolean isEmpty() {
			return vers==null;
		}
		@Override
		public String toString() {
			String[] joined=new String[vers.length];
			int i=-1;
			for(int in:vers)
				joined[++i]=String.valueOf(in);
			if(vers!=null)
				return String.join(".",joined);
			return "Empty";
		}
		private EqualState laterThan(MajorVersion other) {
			if(vers==null)return (other==null||other.vers==null)?EqualState.und:EqualState.lt;
			if(other==null||other.vers==null)return EqualState.gt;
			int len=Math.min(vers.length,other.vers.length);
			for(int i=0;i<len;i++) {
				if(vers[i]>other.vers[i])
					return EqualState.gt;
				else if(vers[i]<other.vers[i])
					return EqualState.lt; 
			}
			return EqualState.of(vers.length-other.vers.length);
		}
		public static MajorVersion parse(String vers) {
			if(vers.isEmpty())
				return empty;
			String[] main=vers.split("\\.");
			if(main.length<=0)return empty;
			int[] majors=new int[main.length];
			int i=-1;
			for(String s:main) {
				try {
					majors[++i]=Integer.parseInt(s);
				}catch(Exception e) {
					majors[i]=s.hashCode();
				}
			}
			return new MajorVersion(majors);
		}
	}
	private MajorVersion majors;
	private SubVersion[] minors;
	private String original="";
	public FHVersion(MajorVersion majors, SubVersion[] minors, String original) {
		this.majors = majors;
		this.minors = minors;
		this.original = original;
	}
	public static final FHVersion empty=new FHVersion();
	private FHVersion() {}
	public boolean isEmpty() {
		return majors==null&&minors==null;
	}
	public boolean laterThan(FHVersion other) {
		EqualState maj=majors.laterThan(other.majors);
		if(!maj.isValid)return true;
		if(maj!=EqualState.eq) {
			return maj.isLater;
		}
		if(minors==null) {
			if(other.minors!=null)
				return SubType.pre.equals(other.minors[0].getType());
			return true;
		}
		if(other.minors==null)
			return !SubType.pre.equals(minors[0].getType());
		int len=Math.min(minors.length,other.minors.length);
		for(int i=0;i<len;i++) {
			EqualState es=minors[i].laterThan(other.minors[i]);
			if(es!=EqualState.eq)
				return es.isLater;
		}
		if(other.minors.length>minors.length) {
			if(SubType.pre.equals(other.minors[minors.length].getType()))
				return true;
			return false;
		}else if(other.minors.length<minors.length&&SubType.pre.equals(minors[other.minors.length].getType()))
			return false;
		return true;
	}
	public static FHVersion parse(String vers) {
		if(vers.isEmpty())
			return empty;
		String[] verss=vers.split("-");
		MajorVersion majors=MajorVersion.parse(verss[0]);
		SubVersion[] svers=null;
		if(verss.length>1)
		svers=new SubVersion[verss.length-1];
		for(int j=1;j<verss.length;j++) {
			svers[j-1]=SubVersion.parse(verss[j]);
		}
		return new FHVersion(majors,svers,vers);
	}
	public String getOriginal() {
		return original;
	}
	@Override
	public String toString() {
		return "FHVersion [ver " + String.valueOf(majors) +Arrays.toString(minors)+"]";
	}
}
