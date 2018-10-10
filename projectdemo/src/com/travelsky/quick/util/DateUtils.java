package com.travelsky.quick.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:日期工具类
 * @author MaRuifu 2016年5月9日下午4:25:58
 * @version 0.1
 */
public final class DateUtils {
	private static DateUtils utils = new DateUtils();
	private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);
	/**
	 * 构造方法
	 */
	private DateUtils() {}
	/**
	 * 
	 * @return 
	 * DateUtils    返回类型 
	 *
	 */
	public static DateUtils getInstance() {
		return utils;
	}

	
	
	/**
	 * 将Timestamp类型的日期转换为系统参数定义的格式的字符串。
	 * 
	 * @param aTsDatetime
	 *            需要转换的日期。
	 * @return  String 转换后符合给定格式的日期字符串
	 */
	public  String format(Date aTsDatetime) {
		return format(aTsDatetime, "yyyy-MM-dd");
	}
	
	/**
	 * 将Date类型的日期转换为系统参数定义的格式的字符串。
	 * 
	 * @param aTsDatetime Date
	 * @param asPattern String
	 * @return String
	 */
	public  String format(Date aTsDatetime, String asPattern) {
		if (aTsDatetime == null || asPattern == null){
			return null;
		}
		SimpleDateFormat dateFromat = new SimpleDateFormat();
		dateFromat.applyPattern(asPattern);
		return dateFromat.format(aTsDatetime);
	}
	/**
	 * 使用给定Pattern解析date
	 * @param date String
	 * @param pattern 参考java API SimpleDateFormat
	 * @return Calendar
	 * @throws ParseException 
	 */
	public Calendar parseDate(String date, String pattern) throws ParseException {
		DateFormat format = new SimpleDateFormat(pattern);
		Calendar calendar = Calendar.getInstance();
		
		calendar.setTime(format.parse(date));
		
		return calendar;
	}
	
	/**
	 * 使用给定的pattern格式化calendar
	 * @param c Calendar
	 * @param pattern 参考java API SimpleDateFormat
	 * @return String
	 */
	public String formatDate(Calendar c, String pattern) {
		DateFormat format = new SimpleDateFormat(pattern);
		return format.format(c.getTime());
	}
	
	/**
	 * 使用给定的pattern格式化date
	 * @param date Date
	 * @param pattern String
	 * @return String
	 */
	public String formatDate(Date date, String pattern) {
		DateFormat format = new SimpleDateFormat(pattern);
		return format.format(date.getTime());
	}
	/**
	 * Date转Calendar
	 * @param date 日期
	 * @param pattern 格式
	 * @return Calendar
	 */
	public Calendar datetoCalendar(Date date, String pattern) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new java.util.Date());
		return calendar;
	}

	/**
	 * 
	 * @param str 格式
	 * @return SimpleDateFormat
	 */
	public SimpleDateFormat getSimDate(String str){
	return new SimpleDateFormat(str);
	}
	
	/**
	 * 
	 * @param str 对时间格式进行校验（yyyy-MM-dd）
	 * @return SimpleDateFormat
	 */
	public static boolean formatDate(String str){
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date parse = simpleDateFormat.parse(str);
			String format = simpleDateFormat.format(parse);
			if(!str.equals(format)){
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	/**
	 * @flightno  航班号
	 * 航班号配合order，三位
	 * @return    order要求的航班号
	 */
	public static String getFlightNo(String flightno){
		if(!"".equals(flightno)){
			if(flightno.length()==4){
				if("0".equals(flightno.substring(0,1))){
					flightno=flightno.substring(1);
				}
			}else{
				return flightno;
			}
		}else{
			return "";
		}
		return flightno;
	}
	/**
	 * @返回中航班号补0
	 * return 四位航班号
	 */
	public static String setFlightNo(String flightno){
		if(!"".equals(flightno)){
			if(flightno.length()==1){
				flightno="000"+flightno;
			}else if(flightno.length()==2){
				flightno="00"+flightno;
			}else if(flightno.length()==3){
				flightno="0"+flightno;
			}else{
				flightno=flightno;
			}
		}else{
			return "";
		}
		return flightno;
	}
}
