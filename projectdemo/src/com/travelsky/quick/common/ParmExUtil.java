package com.travelsky.quick.common;

import com.cares.sh.comm.SystemConfig;

/**
 * passbase项目作为服务起点时,使用该类填充ParmEx
 * @author mike
 *
 */
public class ParmExUtil {
	/**
	 * config.ini里的节点名称
	 */
	public static final String SYS_APP_KEY = "SYS_APP";
	/**
	 * 为传入的 CommandInput 设置 ParmEx
	 * @param apiCtx		目标参数
	 */
	public static final void build(ApiContext apiCtx){
		// 设置默认用户
		String user=SystemConfig.getConfig().getItemString(SYS_APP_KEY, "USERID","SYS_ADMIN");
		apiCtx.setUserID(user);
		// 设置默认APPID
		String appId=SystemConfig.getConfig().getItemString(SYS_APP_KEY, "APPID","LT_WEB");
		apiCtx.setAppID(appId);
		// 设置默认部门
		String deptId=SystemConfig.getConfig().getItemString(SYS_APP_KEY, "DEPTID","SYS_DEPT");
		apiCtx.setTicketDeptid(deptId);
		// 设置渠道
		String channelNo = SystemConfig.getConfig().getItemString(SYS_APP_KEY, "CHANNEL","CTO");
		apiCtx.setChannelNo(channelNo);
		
	}
	
	/**
	 * 获取DES加密加密&解密toke值 
	 * @return
	 */
	public static final String getDesKey(){
		return SystemConfig.getConfig().getItemString(SYS_APP_KEY ,"DES_KEY","B2Ckeynecarescom");
	}
	/**
	 * 获取退款回凋调试的标记
	 * @return
	 */
	public static final String getDebug(){
		return SystemConfig.getConfig().getItemString(SYS_APP_KEY, "ISDEBUG", "false");
	}
	
}
