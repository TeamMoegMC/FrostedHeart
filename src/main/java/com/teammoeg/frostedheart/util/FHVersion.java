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
		boolean isValid;
		boolean isLater;
		private EqualState(boolean isValid, boolean isLater) {
			this.isValid = isValid;
			this.isLater = isLater;
		}
		
	}
	private static class SubVersion{
		String subtype;
		int subversion;
		public SubVersion(String subtype, int subversion) {
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
				return EqualState.of(subversion-other.subversion);
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
			int ver;
			if(i!=v.length()) {
				try {
					ver=Integer.parseInt(v.substring(i));
				}catch(Exception ex){
					ver=v.substring(i).hashCode();
				}
			}else ver=0;
			return new SubVersion(st.toString(),ver);
		}
		@Override
		public String toString() {
			return "-" + subtype + "." + subversion;
		}
	}
	private int[] majors;
	private SubVersion[] minors;
	private String original="";
	public FHVersion(int[] majors, SubVersion[] minors, String original) {
		this.majors = majors;
		this.minors = minors;
		this.original = original;
	}
	public static final FHVersion empty=new FHVersion();
	private FHVersion() {}
	public boolean isEmpty() {
		return majors==null&&minors==null;
	}
	private EqualState majorLaterThan(FHVersion other) {
		if(majors==null)return EqualState.und;
		if(other.majors==null)return EqualState.und;
		int len=Math.min(majors.length,other.majors.length);
		for(int i=0;i<len;i++) {
			if(majors[i]>other.majors[i])
				return EqualState.gt;
			else if(majors[i]<other.majors[i])
				return EqualState.lt; 
		}
		return EqualState.of(majors.length-other.majors.length);
	}
	public boolean laterThan(FHVersion other) {
		EqualState maj=majorLaterThan(other);
		if(!maj.isValid)return true;
		if(maj!=EqualState.eq) {
			return maj.isLater;
		}
		if(minors==null) {
			if(other.minors!=null)
				return false;
			return true;
		}
		if(other.minors==null)
			return true;
		int len=Math.min(minors.length,other.minors.length);
		for(int i=0;i<len;i++) {
			EqualState es=minors[i].laterThan(other.minors[i]);
			if(es!=EqualState.eq)
				return es.isLater;
		}
		return minors.length>=other.minors.length;
	}
	public static FHVersion parse(String vers) {
		if(vers.isEmpty())
			return empty;
		String[] verss=vers.split("-");
		String[] main=verss[0].split("\\.");
		int[] majors=new int[main.length];
		int i=-1;
		for(String s:main) {
			try {
				majors[++i]=Integer.parseInt(s);
			}catch(Exception e) {
				majors[i]=s.hashCode();
			}
		}
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
		return "FHVersion [ver" + Arrays.toString(majors) + " " + Arrays.toString(minors)+"]";
	}
}
