package com.travelsky.quick.util;

public class Utils {

	public static String  getzhCNName(String code) {
		
		switch (code) {
		case "PEK":
			return "北京";
		case "BJS":
			return "北京";
		default:
			break;
		}
		
		
		return code;
		
	}

public static String  getenUSName(String code) {
		
		switch (code) {
		case "BJS":
			return "Capital";
		case "PEK":
			return "Capital";
		default:
			break;
		}
		
		
		return code;
		
	}
	
public static String  getisoCode(String code) {
	
	switch (code) {
	case "CNY":
		return "￥";
	case "USD":
		return "$";
	default:
		break;
	}
	
	
	return code;
	
}


}
