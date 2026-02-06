/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelHelper {
	public static void forEachRowExcludingHeaders(Workbook book,Consumer<Map<String,Cell>> cellConsumer) {
		for(Sheet sh:book) {
			Row header=sh.getRow(0);
			if(header==null) continue;
			Map<Integer,String> headerLines=new HashMap<>();
			for(Cell c:header) {
				headerLines.put(c.getColumnIndex(),getCellValueAsString(c));
			}
			Map<String,Cell> rowCellView=new HashMap<>();
			for(Row r:sh) {
				if(r.getRowNum()==0)continue;
				rowCellView.clear();
				for(Entry<Integer, String> i:headerLines.entrySet()) {
					rowCellView.put(i.getValue(),r.getCell(i.getKey()));
				}
				cellConsumer.accept(rowCellView);
			}
		}
	}
	public static String getCellValueAsString(Cell c) {
		if(c==null)
			return null;
		switch(c.getCellType()) {
		case NUMERIC:return String.valueOf(c.getNumericCellValue());
		case FORMULA:
			switch(c.getCachedFormulaResultType()){
		case NUMERIC:return String.valueOf(c.getNumericCellValue());
		case BOOLEAN:return String.valueOf(c.getBooleanCellValue());
		case STRING:
		case BLANK:return c.getStringCellValue();
		case ERROR:return null;
		}
		case BOOLEAN:return String.valueOf(c.getBooleanCellValue());
		case STRING:
		case BLANK:return c.getStringCellValue();
		case ERROR:return null;
		}
		return null;
	}
	public static double getCellValueAsNumber(Cell c) {
		if(c==null)
			return 0;
		try {
			switch(c.getCellType()) {
			case NUMERIC:return c.getNumericCellValue();
			case FORMULA:
				switch(c.getCachedFormulaResultType()){
			case NUMERIC:return c.getNumericCellValue();
			case BOOLEAN:return c.getBooleanCellValue()?1d:0d;
			case STRING:return Double.parseDouble(c.getStringCellValue());
			case BLANK:return 0;
			case ERROR:return 0;
			}
			case BOOLEAN:return c.getBooleanCellValue()?1d:0d;
			case STRING:return Double.parseDouble(c.getStringCellValue());
			case BLANK:return 0;
			case ERROR:return 0;
			}
		}catch(NumberFormatException ex) {
			
		}
		return 0;
		
	}
	public static boolean getCellValueAsBoolean(Cell c) {
		if(c==null)
			return false;
		
			switch(c.getCellType()) {
			case NUMERIC:return c.getNumericCellValue()>0;
			case FORMULA:
				switch(c.getCachedFormulaResultType()){
			case NUMERIC:return c.getNumericCellValue()>0;
			case BOOLEAN:return c.getBooleanCellValue();
			case STRING:
			case BLANK:return isTrue(c.getStringCellValue());
			case ERROR:return false;
			}
			case BOOLEAN:return c.getBooleanCellValue();
			case STRING:
			case BLANK:return isTrue(c.getStringCellValue());
			case ERROR:return false;
			}
		
		return false;
		
	}
	private static boolean isTrue(String value) {
		if("true".equalsIgnoreCase(value))
			return true;
		if("false".equalsIgnoreCase(value))
			return false;
		try {
			if(Double.valueOf(value)>0)
				return true;
		}catch(NumberFormatException ex) {
			
		}
		return false;
	}
}
