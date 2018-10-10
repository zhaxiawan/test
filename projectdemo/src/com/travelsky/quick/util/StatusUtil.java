package com.travelsky.quick.util;

import com.cares.sh.parm.CommandData;

public class StatusUtil {
	
	public static String getStatus(String invStatus, String saleStatus,String deliveryStatus){
		/**
		 * 1待确认    2已拒绝     3待支付    4已取消    5已支付   6待退款    
		 * 7已退款   8已值机   9已登机   10已使用   11未使用   12异常 
		 * 13 已变更
		 */
		if(deliveryStatus != null && deliveryStatus.contains(",")){
			String[] split = deliveryStatus.split(",");
			deliveryStatus = split[1];
		}
		String status="12";
		//待确认 待确认
		if ("NN".equals(invStatus) && "NN".equals(saleStatus) ) {
			return "1";
		}
		//已拒绝 待确认
		else if("UC".equals(invStatus) && "NN".equals(saleStatus) ){
			return "2";
		}
		//已确认 待确认
		else if("HK".equals(invStatus) && "NN".equals(saleStatus)){
			return "1";
		}
		//已确认 待用户确认
		else if("HK".equals(invStatus) && "USERNN".equals(saleStatus)){
			return "1";
		}
		//已确认 待支付
		else if("HK".equals(invStatus) && "TOPAY".equals(saleStatus)){
			return "3";
		}
		//已确认 已支付 
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus)&& "".equals(deliveryStatus)){
			return "5";
		}
		//已确认 已支付 待初始化
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus)&& "INIT".equals(deliveryStatus)){
			return "5";
		}
		// 已确认 已支付     可使用
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus) && "OP".equals(deliveryStatus)){
			return "5";
		}
		// 已确认 已支付     已值机
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus) && "AC".equals(deliveryStatus)){
			return "8";
		}		
		//	已确认 已支付     已登机
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus) && "BD".equals(deliveryStatus)){
			return "9";
		}
		//  已确认 已支付     已使用
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus) && "USED".equals(deliveryStatus)){
			return "10";
		}
		//	已确认 已支付     已失效
		else if("HK".equals(invStatus) && "PAYED".equals(saleStatus) && "INVALID".equals(deliveryStatus)){
			return "5";
		}
		//因NOSHOW未交付
		else if("NS".equals(deliveryStatus)){
			//已取消 已支付
			if("CANCELLED".equals(invStatus) && "PAYED".equals(saleStatus)){
				return "5";
			//已取消 待确认
			}else if("CANCELLED".equals(invStatus) && "NN".equals(saleStatus)){
				return "4";
			//已取消 待退款
			}else if("CANCELLED".equals(invStatus) && "TOREFUND".equals(saleStatus)){
				return "6";
			//已取消 已退款
			}else if("CANCELLED".equals(invStatus) && "REFUNDED".equals(saleStatus)){
				return "7";
			}else{
				return "5";
			}
		}
		//因IROP未交付
		else if("IROP".equals(deliveryStatus)){
			//已取消 已支付
			if("CANCELLED".equals(invStatus) && "PAYED".equals(saleStatus)){
				return "5";
			//已取消 待确认
			}else if("CANCELLED".equals(invStatus) && "NN".equals(saleStatus)){
				return "4";
			//已取消 待退款
			}else if("CANCELLED".equals(invStatus) && "TOREFUND".equals(saleStatus)){
				return "6";
			//已取消 已退款
			}else if("CANCELLED".equals(invStatus) && "REFUNDED".equals(saleStatus)){
				return "7";
			}else{
				return "5";
			}
		}
		//已取消 待确认
		else if("CANCELLED".equals(invStatus) && "NN".equals(saleStatus)){
			return "4";
		}
		//已取消 待退款
		else if("CANCELLED".equals(invStatus) && "TOREFUND".equals(saleStatus)){
			return "6";
		}
		//已取消 已退款
		else if("CANCELLED".equals(invStatus) && "REFUNDED".equals(saleStatus)){
			return "7";
		}
		//已取消 已变更
		else if("CANCELLED".equals(invStatus) && "EXCHANGED".equals(saleStatus)){
			return "13";
		}
		//已取消 已支付
		else if("CANCELLED".equals(invStatus) && "PAYED".equals(saleStatus)){
			return "5";
		}
		//航班保护确认 待确认
		else if("TK".equals(invStatus) && "NN".equals(saleStatus)){
			return "1";
		}
		//航班保护确认 待用户确认
		else if("TK".equals(invStatus) && "USERNN".equals(saleStatus)){
			return "1";
		}
		//航班保护确认 待支付
		else if("TK".equals(invStatus) && "TOPAY".equals(saleStatus)){
			return "3";
		}
		//航班保护确认 已支付  
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "".equals(deliveryStatus)){
			return "5";
		}
		//航班保护确认 已支付  待初始化
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "INIT".equals(deliveryStatus)){
			return "5";
		}
		//航班保护确认 已支付  可使用
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "OP".equals(deliveryStatus)){
			return "5";
		}
		//航班保护确认 已支付  已值机
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "AC".equals(deliveryStatus)){
			return "8";
		}
		//航班保护确认 已支付  已登机
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "BD".equals(deliveryStatus)){
			return "9";
		}
		//航班保护确认 已支付  已使用
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "USED".equals(deliveryStatus)){
			return "10";
		}
		//航班保护确认 已支付  已失效
		else if("TK".equals(invStatus) && "PAYED".equals(saleStatus) && "INVALID".equals(deliveryStatus)){
			return "12";
		}
		//航班保护确认 已变更
		else if("TK".equals(invStatus) && "EXCHANGED".equals(saleStatus)){
			return "12";
		}
		//航班保护确认 待退款
		else if("TK".equals(invStatus) && "TOREFUND".equals(saleStatus)){
			return "6";
		}
		//航班保护确认 已退款
		else if("TK".equals(invStatus) && "REFUNDED".equals(saleStatus)){
			return "7";
		}
		//收益目的主动清座 待确认
		else if("NO".equals(invStatus) && "NN".equals(saleStatus)){
			return "4";
		}
		//收益目的主动清座 待用户确认
		else if("NO".equals(invStatus) && "USERNN".equals(saleStatus)){
			return "4";
		}
		//收益目的主动清座 已变更
		else if("NO".equals(invStatus) && "EXCHANGED".equals(saleStatus)){
			return "13";
		}
		//收益目的主动清座 待支付
		else if("NO".equals(invStatus) && "TOPAY".equals(saleStatus)){
			return "4";
		}
		//收益目的主动清座 已支付 
		else if("NO".equals(invStatus) && "PAYED".equals(saleStatus)){
			return "5";
		}
		//收益目的主动清座 待退款 
		else if("NO".equals(invStatus) && "TOREFUND".equals(saleStatus)){
			return "6";
		}
		//收益目的主动清座 已退款 
		else if("NO".equals(invStatus) && "REFUNDED".equals(saleStatus)){
			return "7";
		}
		//航班保护取消   已支付
		else if("UN".equals(invStatus) && "PAYED".equals(saleStatus) ){
			return "5";
		}
		//航班保护取消  待退款
		else if("UN".equals(invStatus) && "TOREFUND".equals(saleStatus) ){
			return "6";
		}
		//航班保护取消  已退款
		else if("UN".equals(invStatus) && "REFUNDED".equals(saleStatus) ){
			return "7";
		}
		//航班保护取消  已变更
		else if("UN".equals(invStatus) && "EXCHANGED".equals(saleStatus) ){
			return "13";
		}
		else{
			return status;
		}
	}
	
	public static String getLanguageName(CommandData data, String language){
		String name ="";
		if(data == null){
			return name;
		}
		if ("zh_CN".equals(language)) {
			name = data.getParm("zh_CN").getStringColumn();
		}else if("en_US".equals(language)){
			name = data.getParm("en_US").getStringColumn();
		}else if("ko_KR".equals(language)) {
			name = data.getParm("ko_KR").getStringColumn();
		}
		if("".equals(name)){
			name = data.getParm("en_US").getStringColumn();
			if("".equals(name)){
				name = data.toString();
			}
		}
		return name;
	}
	
	/**
	 * 根据交付状态得出航班状态(仅适用于我的行程)
	 * @param deliverstatus
	 * @return
	 */
	public static String getStatus(String deliverstatus){
		/**
		 * 1待确认    2已拒绝     3待支付    4已取消    5已支付   6待退款    
		 * 7已退款   8已值机   9已登机   10已使用   11未使用   12异常 
		 * 13 已变更
		 */
		String result = "";	
		//待初始化
		if(deliverstatus.indexOf("INIT") >= 0){
			result = "5";
		//可使用
		}else if(deliverstatus.indexOf("OP") >= 0){
			result = "5";
		//已值机
		}else if(deliverstatus.indexOf("AC") >= 0){
			result = "8";
		//已登机
		}else if(deliverstatus.indexOf("BD") >= 0){
			result = "9";
		//已使用
		}else if(deliverstatus.indexOf("USED") >= 0){
			result = "10";
		//因NOSHOW未交付
		}else if(deliverstatus.indexOf("NS") >= 0){
			result = "5";
		//因IROP未交付
		}else if(deliverstatus.indexOf("OP") >= 0){
			result = "5";
		//无效
		}else if(deliverstatus.indexOf("INVALID") >= 0){
			result = "5";
		}
		//因GOSHOW未交付
		else if(deliverstatus.indexOf("DS") >= 0){
			result = "15";
		}
		return result;
	}
}
