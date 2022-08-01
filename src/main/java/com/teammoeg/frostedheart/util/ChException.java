package com.teammoeg.frostedheart.util;

public class ChException {

    public ChException() {
    }

    public static class 作弊者禁止进入 extends RuntimeException {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public 作弊者禁止进入() {
            super("No Cheats");
            super.setStackTrace(new StackTraceElement[0]);
        }

    }
}
