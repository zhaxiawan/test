
package com.travelsky.quick.common;

import java.io.File;
import java.nio.file.Paths;

import org.springframework.util.StringUtils;

/**
 * 常量定义类
 * @author hu
 *
 */
public final class CommonConstants {

	public static final String CONF_PATH_KEY = "res-api.conf";
	/**
	 * 成功状态
	 */
	public static final String API_RESP_SUCC = "0000";
	/**
	 * 错误码
	 */
	public static final String API_RESP_ERROR_CODE = "errorcode";
	/**
	 * 错误描述
	 */
	public static final String API_RESP_ERROR_DESC = "errordesc";
	/**
	 * 辅营类型改
	 */
	public static final String API_PENALTY_TYPE_CHANGE = "Change";
	/**
	 * 辅营类型退
	 */
	public static final String API_PENALTY_TYPE_REFUND = "Refund";
	/**
	 * 是否追加辅营,Y表示追加
	 */
	public static final String API_PENALTY_IS_ADD = "Y";
	/**
	 * 是否追加辅营,N表示非追加
	 */
	public static final String API_PENALTY_IS_NOADD = "N";
	/**
	 * 旅客类型:成人
	 */
	public static final String API_PASSENGER_TYPE_ADT = "ADT";
	/**
	 * 旅客类型:儿童
	 */
	public static final String API_PASSENGER_TYPE_CHD = "CHD";
	/**
	 * 旅客类型:婴儿
	 */
	public static final String API_PASSENGER_TYPE_INF = "INF";
	/**
	 * 客票状态
	 */
	public static final String API_PASSENGER_TICKET_CODE = "O";
	/**
	 * 辅营类型:行李
	 */
	public static final String API_PENALTY_TYPE_BAG = "bag";
	/**
	 * 辅营类型:保险
	 */
	public static final String API_PENALTY_TYPE_INS = "ins";
	/**
	 * 辅营类型:餐食
	 */
	public static final String API_PENALTY_TYPE_MEAL = "meal";
	/**
	 * 机场建设费
	 */
	public static final String API_COST_TYPE_CNFEE = "CN";
	/**
	 * 燃油附加费
	 */
	public static final String API_COST_TYPE_YQFEE = "YQ";
	/**
	 * 其他税费
	 */
	public static final String API_COST_TYPE_TAX = "TAX";
	/**
	 * 用户信息查询
	 */
	public static final String API_QUERY_TYPE_USER = "USER_QUERY";
	/**
	 * 偏好查询
	 */
	public static final String API_QUERY_TYPE_PREFERENCE = "QUERY_PREFERENCE";
	/**
	 * 会员信息修改
	 */
	public static final String API_UPDATE_MEMBER = "USER_MODIFY";
	/**
	 *乘机人信息修改
	 */
	public static final String API_UPDATE_PASSENGER = "MODIFY_PASSENGER";
	/**
	 * 偏好修改
	 */
	public static final String API_UPDATE_PREFERENCE = "USER_MODIFY_PREFERENCE";
	/**
	 * 证件号修改
	 */
	public static final String API_UPDATE_ID = "USER_MODIFY_FOID";
	/**
	 * 地址修改
	 */
	public static final String API_UPDATE_ADDRESS = "USER_MODIFY_ADDRESS";
	/**
	 * 联系方式修改
	 */
	public static final String API_UPDATE_CONTACT = "USER_MODIFY_CONTACTMETHOD";

	/**
	 * 增加航班预约
	 */
	public static final String API_INSERT_FLIGHT = "INSERT_FLIGHT";
	/**
	 * 增加航班预约
	 */
	public static final String API_INSERT_AIRLINE = "INSERT_AIRLINE";

	/**
	 * 查询航班预约
	 */
	public static final String API_QUERY_FLIGHT = "QUERY_FLIGHT";
	/**
	 * 查询航线预约
	 */
	public static final String API_QUERY_AIRLINE = "QUERY_AIRLINE";
	/**
	 * 取消航班预约
	 */
	public static final String API_CANCEL_FLIGHT = "CANCEL_FLIGHT";
	/**
	 * 取消航线预约
	 */
	public static final String API_CANCEL_AIRLINE = "CANCEL_AIRLINE";
	/**
	 * 删除航班预约
	 */
	public static final String API_DELETE_FLIGHT = "DELETE_FLIGHT";
	/**
	 * 航站对查询所有值
	 */
	public static final String API_SEARCHODS_AIRPORTCODE ="ALL";
	/**
	 * 删除航线预约
	 */
	public static final String API_DELETE_AIRLINE = "DELETE_AIRLINE";

	/**
	 * 性别男
	 */
	public static final String API_LCCUSER_GENDER_M = "M";
	/**
	 * 性别女
	 */
	public static final String API_LCCUSER_GENDER_F = "F";
	/**
	 * 证件类别:身份证
	 */
	public static final String API_LCCUSER_FOID_NI = "NI";
	/**
	 * 证件类别:护照
	 */
	public static final String API_LCCUSER_FOID_PP = "PP";
	/**
	 * 证件类别:信用卡
	 */
	public static final String API_LCCUSER_FOID_CC = "CC";
	/**
	 * 证件类别:常旅客
	 */
	public static final String API_LCCUSER_FOID_FF = "FF";
	/**
	 * 证件类别:驾照
	 */
	public static final String API_LCCUSER_FOID_DL = "DL";
	/**
	 * 联系方式-电话
	 */
	public static final String API_TYPE_TEL = "tel";
	/**
	 * 联系方式-手机
	 */
	public static final String API_TYPE_MOBILE = "mobile";
	/**
	 * 联系方式-电邮
	 */
	public static final String API_TYPE_EMAIL = "email";
	/**
	 * 联系方式-QQ
	 */
	public static final String API_TYPE_QQ= "qq";
	/**
	 * 联系方式-微信
	 */
	public static final String API_TYPE_WEBCHAT = "webchat";
	/**
	 * 首选舱位标记——F
	 */
	public static final String API_CABIN_F = "F";
	/**
	 * 首选舱位标记——C
	 */
	public static final String API_CABIN_C = "C";
	/**
	 * 首选舱位标记——Y
	 */
	public static final String API_CABIN_Y = "Y";
	/**
	 * 通知方式-短信
	 */
	public static final String API_NOTICETYPE_ONE = "1";
	/**
	 * 通知方式-邮件
	 */
	public static final String API_NOTICETYPE_TWO = "2";
	/**
	 * 通知方式-短信+邮件
	 */
	public static final String API_NOTICETYPE_THREE = "3";
	/**
	 * 查询通知类型:查询用户通知
	 */
	public static final String API_NOTICE_QUERYTYPE_USER = "NOTICETYPE_USER";
	/**
	 * 查询通知类:获取所有通知
	 */
	public static final String API_NOTICE_QUERYTYPE_ALL = "NOTICETYPE_GETALL";
	/**
	 * 会员信息增加_地址
	 */
	public static final String API_USER_INSERT_ADDRESS = "USER_INSERT_ADDRESS";
	/**
	 * 会员信息增加_联系方式
	 */
	public static final String API_USER_INSERT_CONTACTMETHOD = "USER_INSERT_CONTACTMETHOD";
	/**
	 * 会员信息增加_证件号
	 */
	public static final String API_USER_INSERT_FOID = "USER_INSERT_FOID";
	/**
	 * 会员信息增加_常用乘机人
	 */
	public static final String API_USER_INSERT_PASSENGER = "INSERT_PASSENGER";
	/**
	 * 会员信息删除_地址
	 */
	public static final String API_USER_DELETE_ADDRESS = "USER_DELETE_ADDRESS";
	/**
	 * 会员信息删除_联系方式
	 */
	public static final String API_USER_DELETE_CONTACTMETHOD = "USER_DELETE_CONTACTMETHOD";
	/**
	 * 会员信息删除_证件号
	 */
	public static final String API_USER_DELETE_FOID = "USER_DELETE_FOID";
	/**
	 * 会员信息删除_常用乘机人
	 */
	public static final String API_USER_DELETE_PASSENGER = "DELETE_PASSENGER";
	/**
	 * 发送短信
	 */
	public static final String API_MSGTYPE_SMS = "MSGTYPE_SMS";
	/**
	 * 发送邮件
	 */
	public static final String API_MSGTYPE_EMAIL = "MSGTYPE_EMAIL";
	/**
	 * 认证手机号
	 * 认证邮箱号
	 */
	public static final String API_AUTH_PHONE = "AUTH_PHONE";
	/**
	 * 实名认证
	 */
	public static final String API_AUTH_ID = "AUTH_ID";
	/**
	 * 是否仅查询直飞航班(Y是，N否)
	 */
	public static final String API_DIRECT_Y = "Y";
	/**
	 * 是否仅查询直飞航班(Y是，N否)
	 */
	public static final String API_DIRECT_N = "N";
	/**
	 * "ture"时，表明给定的日期范围内每天都有航班且没有dates字段
	 */
	public static final String API_EVERYDAY_TURE = "ture";
	/**
	 * 不可用状态
	 */
	public static final String API_STATUS_DISABLED = "1";
	/**
	 * 渠道来源B2B
	 */
	public static final String API_CHANNELTYPE_B2C = "B2C";
	/**
	 * 渠道来源OTA
	 */
	public static final String API_CHANNELTYPE_OTA = "OTA";
	/**
	 * 渠道来源B2B
	 */
	public static final String API_CHANNELTYPE_B2B = "B2B";

	/**
	 * 支付回调Service name
	 */
	public static final String API_PAYCALLBACK_SEVNAME="LCC_PAYCALLBACK_SERVICE";
	/**
	 * 退款回调Service name
	 */
	public static final String API_REFUNDCALLBACK_SEVNAME="LCC_REFUNDCALLBACK_SERVICE";

	public static final String CONF_PATH = getConfigPath();

	public static final String ROOT_PATH = StringUtils.hasLength(CONF_PATH)?
			Paths.get(CONF_PATH).getParent().toString()+File.separatorChar :
				"/";

	/**
	 * 获取配置根路径
	 * @return
	 */
	private static final String getConfigPath() {
		String confPath = System.getProperty(CONF_PATH_KEY, "");
		if (StringUtils.hasLength(confPath)) {
			if (!confPath.endsWith("/")
					&& !confPath.endsWith("\\")) {
				confPath = new StringBuilder(confPath)
						.append(File.separatorChar)
						.toString();
			}
		}
		return confPath;
	}

	private CommonConstants() {}
}
