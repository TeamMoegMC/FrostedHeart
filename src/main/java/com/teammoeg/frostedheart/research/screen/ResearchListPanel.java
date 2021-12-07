package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

public class ResearchListPanel extends Panel {
    Logger LOGGER = new Logger() {
        @Override
        public void catching(Level level, Throwable t) {

        }

        @Override
        public void catching(Throwable t) {

        }

        @Override
                public void debug(Marker marker, Message msg) {

        }

        @Override
        public void debug(Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void debug(Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void debug(Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void debug(Marker marker, CharSequence message) {

        }

        @Override
        public void debug(Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void debug(Marker marker, Object message) {

        }

        @Override
        public void debug(Marker marker, Object message, Throwable t) {

        }

        @Override
        public void debug(Marker marker, String message) {

        }

        @Override
        public void debug(Marker marker, String message, Object... params) {

        }

        @Override
        public void debug(Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void debug(Marker marker, String message, Throwable t) {

        }

        @Override
        public void debug(Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void debug(Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void debug(Message msg) {

        }

        @Override
        public void debug(Message msg, Throwable t) {

        }

        @Override
        public void debug(MessageSupplier msgSupplier) {

        }

        @Override
        public void debug(MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void debug(CharSequence message) {

        }

        @Override
        public void debug(CharSequence message, Throwable t) {

        }

        @Override
        public void debug(Object message) {

        }

        @Override
        public void debug(Object message, Throwable t) {

        }

        @Override
        public void debug(String message) {

        }

        @Override
        public void debug(String message, Object... params) {

        }

        @Override
        public void debug(String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void debug(String message, Throwable t) {

        }

        @Override
        public void debug(Supplier<?> msgSupplier) {

        }

        @Override
        public void debug(Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void debug(String message, Object p0) {

        }

        @Override
        public void debug(String message, Object p0, Object p1) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void entry() {

        }

        @Override
        public void entry(Object... params) {

        }

        @Override
        public void error(Marker marker, Message msg) {

        }

        @Override
        public void error(Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void error(Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void error(Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void error(Marker marker, CharSequence message) {

        }

        @Override
        public void error(Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void error(Marker marker, Object message) {

        }

        @Override
        public void error(Marker marker, Object message, Throwable t) {

        }

        @Override
        public void error(Marker marker, String message) {

        }

        @Override
        public void error(Marker marker, String message, Object... params) {

        }

        @Override
        public void error(Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void error(Marker marker, String message, Throwable t) {

        }

        @Override
        public void error(Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void error(Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void error(Message msg) {

        }

        @Override
        public void error(Message msg, Throwable t) {

        }

        @Override
        public void error(MessageSupplier msgSupplier) {

        }

        @Override
        public void error(MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void error(CharSequence message) {

        }

        @Override
        public void error(CharSequence message, Throwable t) {

        }

        @Override
        public void error(Object message) {

        }

        @Override
        public void error(Object message, Throwable t) {

        }

        @Override
        public void error(String message) {

        }

        @Override
        public void error(String message, Object... params) {

        }

        @Override
        public void error(String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void error(String message, Throwable t) {

        }

        @Override
        public void error(Supplier<?> msgSupplier) {

        }

        @Override
        public void error(Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void error(Marker marker, String message, Object p0) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void error(String message, Object p0) {

        }

        @Override
        public void error(String message, Object p0, Object p1) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void exit() {

        }

        @Override
        public <R> R exit(R result) {
            return null;
        }

        @Override
        public void fatal(Marker marker, Message msg) {

        }

        @Override
        public void fatal(Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void fatal(Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void fatal(Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void fatal(Marker marker, CharSequence message) {

        }

        @Override
        public void fatal(Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void fatal(Marker marker, Object message) {

        }

        @Override
        public void fatal(Marker marker, Object message, Throwable t) {

        }

        @Override
        public void fatal(Marker marker, String message) {

        }

        @Override
        public void fatal(Marker marker, String message, Object... params) {

        }

        @Override
        public void fatal(Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void fatal(Marker marker, String message, Throwable t) {

        }

        @Override
        public void fatal(Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void fatal(Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void fatal(Message msg) {

        }

        @Override
        public void fatal(Message msg, Throwable t) {

        }

        @Override
        public void fatal(MessageSupplier msgSupplier) {

        }

        @Override
        public void fatal(MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void fatal(CharSequence message) {

        }

        @Override
        public void fatal(CharSequence message, Throwable t) {

        }

        @Override
        public void fatal(Object message) {

        }

        @Override
        public void fatal(Object message, Throwable t) {

        }

        @Override
        public void fatal(String message) {

        }

        @Override
        public void fatal(String message, Object... params) {

        }

        @Override
        public void fatal(String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void fatal(String message, Throwable t) {

        }

        @Override
        public void fatal(Supplier<?> msgSupplier) {

        }

        @Override
        public void fatal(Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void fatal(String message, Object p0) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public Level getLevel() {
            return null;
        }

        @Override
        public <MF extends MessageFactory> MF getMessageFactory() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void info(Marker marker, Message msg) {

        }

        @Override
        public void info(Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void info(Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void info(Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void info(Marker marker, CharSequence message) {

        }

        @Override
        public void info(Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void info(Marker marker, Object message) {

        }

        @Override
        public void info(Marker marker, Object message, Throwable t) {

        }

        @Override
        public void info(Marker marker, String message) {

        }

        @Override
        public void info(Marker marker, String message, Object... params) {

        }

        @Override
        public void info(Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void info(Marker marker, String message, Throwable t) {

        }

        @Override
        public void info(Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void info(Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void info(Message msg) {

        }

        @Override
        public void info(Message msg, Throwable t) {

        }

        @Override
        public void info(MessageSupplier msgSupplier) {

        }

        @Override
        public void info(MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void info(CharSequence message) {

        }

        @Override
        public void info(CharSequence message, Throwable t) {

        }

        @Override
        public void info(Object message) {

        }

        @Override
        public void info(Object message, Throwable t) {

        }

        @Override
        public void info(String message) {

        }

        @Override
        public void info(String message, Object... params) {

        }

        @Override
        public void info(String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void info(String message, Throwable t) {

        }

        @Override
        public void info(Supplier<?> msgSupplier) {

        }

        @Override
        public void info(Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void info(Marker marker, String message, Object p0) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void info(String message, Object p0) {

        }

        @Override
        public void info(String message, Object p0, Object p1) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isEnabled(Level level) {
            return false;
        }

        @Override
        public boolean isEnabled(Level level, Marker marker) {
            return false;
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isFatalEnabled() {
            return false;
        }

        @Override
        public boolean isFatalEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return false;
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return false;
        }

        @Override
        public void log(Level level, Marker marker, Message msg) {

        }

        @Override
        public void log(Level level, Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void log(Level level, Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void log(Level level, Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void log(Level level, Marker marker, CharSequence message) {

        }

        @Override
        public void log(Level level, Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void log(Level level, Marker marker, Object message) {

        }

        @Override
        public void log(Level level, Marker marker, Object message, Throwable t) {

        }

        @Override
        public void log(Level level, Marker marker, String message) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object... params) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Throwable t) {

        }

        @Override
        public void log(Level level, Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void log(Level level, Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void log(Level level, Message msg) {

        }

        @Override
        public void log(Level level, Message msg, Throwable t) {

        }

        @Override
        public void log(Level level, MessageSupplier msgSupplier) {

        }

        @Override
        public void log(Level level, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void log(Level level, CharSequence message) {

        }

        @Override
        public void log(Level level, CharSequence message, Throwable t) {

        }

        @Override
        public void log(Level level, Object message) {

        }

        @Override
        public void log(Level level, Object message, Throwable t) {

        }

        @Override
        public void log(Level level, String message) {

        }

        @Override
        public void log(Level level, String message, Object... params) {

        }

        @Override
        public void log(Level level, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void log(Level level, String message, Throwable t) {

        }

        @Override
        public void log(Level level, Supplier<?> msgSupplier) {

        }

        @Override
        public void log(Level level, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void log(Level level, String message, Object p0) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void printf(Level level, Marker marker, String format, Object... params) {

        }

        @Override
        public void printf(Level level, String format, Object... params) {

        }

        @Override
        public <T extends Throwable> T throwing(Level level, T t) {
            return null;
        }

        @Override
        public <T extends Throwable> T throwing(T t) {
            return null;
        }

        @Override
        public void trace(Marker marker, Message msg) {

        }

        @Override
        public void trace(Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void trace(Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void trace(Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void trace(Marker marker, CharSequence message) {

        }

        @Override
        public void trace(Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void trace(Marker marker, Object message) {

        }

        @Override
        public void trace(Marker marker, Object message, Throwable t) {

        }

        @Override
        public void trace(Marker marker, String message) {

        }

        @Override
        public void trace(Marker marker, String message, Object... params) {

        }

        @Override
        public void trace(Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void trace(Marker marker, String message, Throwable t) {

        }

        @Override
        public void trace(Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void trace(Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void trace(Message msg) {

        }

        @Override
        public void trace(Message msg, Throwable t) {

        }

        @Override
        public void trace(MessageSupplier msgSupplier) {

        }

        @Override
        public void trace(MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void trace(CharSequence message) {

        }

        @Override
        public void trace(CharSequence message, Throwable t) {

        }

        @Override
        public void trace(Object message) {

        }

        @Override
        public void trace(Object message, Throwable t) {

        }

        @Override
        public void trace(String message) {

        }

        @Override
        public void trace(String message, Object... params) {

        }

        @Override
        public void trace(String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void trace(String message, Throwable t) {

        }

        @Override
        public void trace(Supplier<?> msgSupplier) {

        }

        @Override
        public void trace(Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void trace(String message, Object p0) {

        }

        @Override
        public void trace(String message, Object p0, Object p1) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public EntryMessage traceEntry() {
            return null;
        }

        @Override
        public EntryMessage traceEntry(String format, Object... params) {
            return null;
        }

        @Override
        public EntryMessage traceEntry(Supplier<?>... paramSuppliers) {
            return null;
        }

        @Override
        public EntryMessage traceEntry(String format, Supplier<?>... paramSuppliers) {
            return null;
        }

        @Override
        public EntryMessage traceEntry(Message message) {
            return null;
        }

        @Override
        public void traceExit() {

        }

        @Override
        public <R> R traceExit(R result) {
            return null;
        }

        @Override
        public <R> R traceExit(String format, R result) {
            return null;
        }

        @Override
        public void traceExit(EntryMessage message) {

        }

        @Override
        public <R> R traceExit(EntryMessage message, R result) {
            return null;
        }

        @Override
        public <R> R traceExit(Message message, R result) {
            return null;
        }

        @Override
        public void warn(Marker marker, Message msg) {

        }

        @Override
        public void warn(Marker marker, Message msg, Throwable t) {

        }

        @Override
        public void warn(Marker marker, MessageSupplier msgSupplier) {

        }

        @Override
        public void warn(Marker marker, MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void warn(Marker marker, CharSequence message) {

        }

        @Override
        public void warn(Marker marker, CharSequence message, Throwable t) {

        }

        @Override
        public void warn(Marker marker, Object message) {

        }

        @Override
        public void warn(Marker marker, Object message, Throwable t) {

        }

        @Override
        public void warn(Marker marker, String message) {

        }

        @Override
        public void warn(Marker marker, String message, Object... params) {

        }

        @Override
        public void warn(Marker marker, String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void warn(Marker marker, String message, Throwable t) {

        }

        @Override
        public void warn(Marker marker, Supplier<?> msgSupplier) {

        }

        @Override
        public void warn(Marker marker, Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void warn(Message msg) {

        }

        @Override
        public void warn(Message msg, Throwable t) {

        }

        @Override
        public void warn(MessageSupplier msgSupplier) {

        }

        @Override
        public void warn(MessageSupplier msgSupplier, Throwable t) {

        }

        @Override
        public void warn(CharSequence message) {

        }

        @Override
        public void warn(CharSequence message, Throwable t) {

        }

        @Override
        public void warn(Object message) {

        }

        @Override
        public void warn(Object message, Throwable t) {

        }

        @Override
        public void warn(String message) {

        }

        @Override
        public void warn(String message, Object... params) {

        }

        @Override
        public void warn(String message, Supplier<?>... paramSuppliers) {

        }

        @Override
        public void warn(String message, Throwable t) {

        }

        @Override
        public void warn(Supplier<?> msgSupplier) {

        }

        @Override
        public void warn(Supplier<?> msgSupplier, Throwable t) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }

        @Override
        public void warn(String message, Object p0) {

        }

        @Override
        public void warn(String message, Object p0, Object p1) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {

        }

        @Override
        public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {

        }
    }

    public static final int RESEARCH_WIDTH = 200, RESEARCH_HEIGHT = 18;
    public static final int RES_ICON_WIDTH = 16, RES_ICON_HEIGHT = 16;
    public static final int RES_PANEL_WIDTH = 80;

    public ResearchScreen researchScreen;
    public PanelScrollBar scroll;
    public ResearchList rl;
    public ResearchListPanel(ResearchScreen panel) {
        super(panel);
        researchScreen = panel;
    }
    public static class ResearchList extends Panel{
    	public ResearchScreen researchScreen;
		public ResearchList(ResearchListPanel panel) {
			super(panel);
			researchScreen=panel.researchScreen;
			this.setWidth(RESEARCH_WIDTH);
		}

		@Override
		public void addWidgets() {
	        int offset = 0;

	        for (Research r:FHResearch.getResearchesForRender(this.researchScreen.selectedCategory)) {
	        	
	            ResearchButton button = new ResearchButton(this, r);
	            add(button);
	            button.setPos(0,offset);
	            offset += (RESEARCH_HEIGHT + 4);
	        }
	        this.setHeight(offset);
		}

		@Override
		public void alignWidgets() {
		}
    	
    }
    public static class ResearchButton extends Button {

        Research research;
        ResearchList listPanel;

        public ResearchButton(ResearchList panel, Research research) {
            super(panel, research.getName(), ItemIcon.getItemIcon(research.getIcon()));
            this.research = research;
            this.listPanel =  panel;
            setSize(RESEARCH_WIDTH, RESEARCH_HEIGHT);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            listPanel.researchScreen.selectResearch(research);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(research.getDesc());
        }

        @Override
        public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			super.drawBackground(matrixStack, theme, x, y, w, h);
            //theme.drawHorizontalTab(matrixStack, x, y, w, h, categoryPanel.researchScreen.selectedCategory == category);
			
			this.drawIcon(matrixStack, theme, x + 2, y + 2, RES_ICON_WIDTH, RES_ICON_HEIGHT);
//            theme.drawHorizontalTab(matrixStack, x, y, w, h, listPanel.researchScreen.selectedResearch == research);
//            this.drawIcon(matrixStack, theme, x + 2, y + 2, RES_ICON_WIDTH, RES_ICON_HEIGHT);
            theme.drawString(matrixStack, research.getName(), x + RES_ICON_WIDTH + 4, y + RES_ICON_HEIGHT /2 - 4);
        }
    }

    @Override
    public void addWidgets() {
    	rl=new ResearchList(this);
    	scroll=new PanelScrollBar(this,rl);
    	add(rl);
    	add(scroll);
    	scroll.setX(RESEARCH_WIDTH);
    	scroll.setSize(10,height);
    }

    @Override
    public void alignWidgets() {

    }

    @Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawPanelBackground(matrixStack, x, y, w, h);
	}

	@Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
    }
}

