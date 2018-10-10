package com.travelsky.quick.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;

/**
 *
 * @author ZHANGJIABIN
 *
 */
public class RedisUtil {

	private static Object object = new Object();

	/**
	 * 对B2C的请求做计数 并存入缓存
	 * @param type (LOOK/SERVICE) appId
	 * @param context context
	 */
	public void docount(String type,SelvetContext<ApiContext> context){
		 String depno=context.getContext().getTicketDeptid();
			String appId= context.getContext().getAppID();
		String key=new SimpleDateFormat("yyyyMMddHH").format(new Date())+"|"+type+"|"+appId+"|"+depno;
		synchronized (object) {
			if (RedisManager.getManager().get(key)==null||RedisManager.getManager().get(key).equals("")) {
				RedisManager.getManager().set(key, "1", 24*3600);
			}else{
				RedisManager.getManager().incrCount(key);
			}
		}

	}
}
