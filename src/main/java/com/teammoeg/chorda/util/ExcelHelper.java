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

/**
 * Excel/电子表格操作辅助类，提供遍历工作簿行数据、单元格类型转换等功能。
 * 使用Apache POI库处理Excel文件。
 * <p>
 * Excel/spreadsheet operation helper class providing workbook row iteration,
 * cell type conversion and other utilities. Uses Apache POI library for Excel file handling.
 */
public class ExcelHelper {
	/**
	 * 遍历工作簿中所有工作表的数据行（跳过表头），将每行数据以列名到单元格的Map形式传递给消费者。
	 * <p>
	 * Iterate over all data rows (excluding headers) in all sheets of the workbook,
	 * passing each row as a column-name-to-cell Map to the consumer.
	 *
	 * @param book 工作簿 / the workbook
	 * @param cellConsumer 行数据消费者 / the row data consumer
	 */
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
	/**
	 * 将单元格值转换为字符串，支持数字、布尔、公式和文本类型。
	 * <p>
	 * Convert a cell value to a String, supporting numeric, boolean, formula, and text types.
	 *
	 * @param c 单元格 / the cell
	 * @return 字符串值，单元格为null或错误时返回null / the string value, or null if cell is null or error
	 */
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
	/**
	 * 将单元格值转换为数字，支持多种类型的自动转换。
	 * <p>
	 * Convert a cell value to a number, supporting automatic conversion from various types.
	 *
	 * @param c 单元格 / the cell
	 * @return 数值，单元格为null或无法转换时返回0 / the numeric value, or 0 if cell is null or cannot be converted
	 */
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
	/**
	 * 将单元格值转换为布尔值，支持多种类型的自动转换。
	 * <p>
	 * Convert a cell value to a boolean, supporting automatic conversion from various types.
	 *
	 * @param c 单元格 / the cell
	 * @return 布尔值，单元格为null时返回false / the boolean value, or false if cell is null
	 */
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
