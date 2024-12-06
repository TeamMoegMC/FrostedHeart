/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart;

import java.util.Arrays;

public class FHVersion {
    private enum EqualState {
        lt(true, false),
        eq(true, true),
        gt(true, true),
        und(false, true);

        final boolean isValid;

        final boolean isLater;
        static EqualState of(int min) {
            if (min > 0) return gt;
            if (min < 0) return lt;
            return eq;
        }

        EqualState(boolean isValid, boolean isLater) {
            this.isValid = isValid;
            this.isLater = isLater;
        }

    }

    private static class MajorVersion {
        private static final MajorVersion empty = new MajorVersion(null);
        private int[] vers;

        public static MajorVersion parse(String vers) {
            if (vers.isEmpty())
                return empty;
            String[] main = vers.split("\\.");
            if (main.length == 0) return empty;
            int[] majors = new int[main.length];
            int i = -1;
            for (String s : main) {
                try {
                    majors[++i] = Integer.parseInt(s);
                } catch (Exception e) {
                    majors[i] = s.hashCode();
                }
            }
            return new MajorVersion(majors);
        }

        public MajorVersion(int[] vers) {
            this.vers = vers;
        }

        public boolean isEmpty() {
            return vers == null;
        }

        private EqualState laterThan(MajorVersion other) {
            if (vers == null) return (other == null || other.vers == null) ? EqualState.und : EqualState.lt;
            if (other == null || other.vers == null) return EqualState.gt;
            int len = Math.min(vers.length, other.vers.length);
            for (int i = 0; i < len; i++) {
                if (vers[i] > other.vers[i])
                    return EqualState.gt;
                else if (vers[i] < other.vers[i])
                    return EqualState.lt;
            }
            return EqualState.of(vers.length - other.vers.length);
        }

        @Override
        public String toString() {
            String[] joined = new String[vers.length];
            int i = -1;
            for (int in : vers)
                joined[++i] = String.valueOf(in);
            if (vers != null)
                return String.join(".", joined);
            return "Empty";
        }
    }


    private enum SubType {//stable>hf>empty>rc>pre
        pre,
        rc,
        hf,
        stable;

        public static EqualState compare(SubType v1, SubType v2) {
            if (v1 == null)
                return v2 == stable || v2 == hf ? EqualState.lt : (v2 == null ? EqualState.eq : EqualState.gt);
            if (v2 == null)
                return v1 == stable || v1 == hf ? EqualState.gt : EqualState.lt;
            return EqualState.of(v1.ordinal() - v2.ordinal());
        }

        public static EqualState compareNoEq(SubType v1, SubType v2) {
            if (v1 == null)
                return v2 == stable || v2 == hf ? EqualState.lt : EqualState.gt;
            if (v2 == null)
                return v1 == stable || v1 == hf ? EqualState.gt : EqualState.lt;
            return EqualState.of(v1.ordinal() - v2.ordinal());
        }
    }

    private static class SubVersion {
        String subtype;
        MajorVersion subversion;

        public static SubVersion parse(String v) {
            StringBuilder st = new StringBuilder();
            int i;
            for (i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (c >= '0' && c <= '9') break;//number start
                st.append(c);
            }
            MajorVersion ver;
            if (i != v.length()) {
                ver = MajorVersion.parse(v.substring(i));
            } else ver = MajorVersion.empty;
            return new SubVersion(st.toString(), ver);
        }

        public SubVersion(String subtype, MajorVersion subversion) {
            this.subtype = subtype;
            this.subversion = subversion;
        }

        public SubType getType() {
            for (SubType type : SubType.values()) {
                if (type.name().equalsIgnoreCase(subtype))
                    return type;
            }
            return null;
        }

        public EqualState laterThan(SubVersion other) {
            SubType ttype = getType();
            SubType otype = other.getType();
            if (ttype == otype) {
                return subversion.laterThan(other.subversion);
            }
            return SubType.compareNoEq(ttype, otype);
        }

        @Override
        public String toString() {
            return "-" + subtype + subversion.toString();
        }
    }

    public static final FHVersion empty = new FHVersion();
    private MajorVersion majors = MajorVersion.empty;
    private SubVersion[] minors;

    private String original = "";

    public static FHVersion parse(String vers) {
        if (vers.isEmpty())
            return empty;
        String[] verss = vers.split("-");
        MajorVersion majors = MajorVersion.parse(verss[0]);
        SubVersion[] svers = null;
        if (verss.length > 1)
            svers = new SubVersion[verss.length - 1];
        for (int j = 1; j < verss.length; j++) {
            svers[j - 1] = SubVersion.parse(verss[j]);
        }
        return new FHVersion(majors, svers, vers);
    }

    private FHVersion() {
    }

    public FHVersion(MajorVersion majors, SubVersion[] minors, String original) {
        this.majors = majors;
        this.minors = minors;
        this.original = original;
    }

    public String getOriginal() {
        return original;
    }

    public boolean isEmpty() {
        return majors == MajorVersion.empty && minors == null;
    }

    public boolean laterThan(FHVersion other) {
        EqualState maj = majors.laterThan(other.majors);
        if (!maj.isValid) return true;
        if (maj != EqualState.eq) {
            return maj.isLater;
        }
        if (minors == null) {
            if (other.minors != null)
                return SubType.pre.equals(other.minors[0].getType());
            return true;
        }
        if (other.minors == null)
            return !SubType.pre.equals(minors[0].getType());
        int len = Math.min(minors.length, other.minors.length);
        for (int i = 0; i < len; i++) {
            EqualState es = minors[i].laterThan(other.minors[i]);
            if (es != EqualState.eq)
                return es.isLater;
        }
        if (other.minors.length > minors.length) {
            return SubType.pre.equals(other.minors[minors.length].getType());
        } else return other.minors.length >= minors.length || !SubType.pre.equals(minors[other.minors.length].getType());
    }

    @Override
    public String toString() {
        return "ver " + majors + Arrays.toString(minors);
    }
}
