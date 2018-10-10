package com.travelsky.quick.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.SystemConfig;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.log.ats.AtsLogHelper;
import com.travelsky.quick.util.DESUtil;
import com.travelsky.quick.util.helper.PayCallbackHelper;

import org.apache.commons.codec.binary.Base64;

public class AdyenOnlinePayResult  extends ApiServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 7537442593711543756L;
	private static Logger logger = LoggerFactory.getLogger(AdyenOnlinePayResult.class);


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		ApiContext apiCtx = context.getContext();
		ParmExUtil.build(apiCtx);
		context.setRequest(request);
		context.setResponse(response);
		// 获取请求参数
		CommandData str = new CommandData();
		String json = request.getQueryString().trim();
		String desKey = ParmExUtil.getDesKey();
		try {
			json =DESUtil.decrypt(json, desKey);
		} catch (Exception e1) {
			logger.error("DES encrypt failed! The encrypt content:[{}], key:[{}]",json, desKey);
		}
		if(!json.equals("") && json.startsWith("{") && json.endsWith("}")){
			JsonUnit.fromJson(str, json);
		}
		//获取的请求参数放入到context.getInput中
		str.copyTo(context.getInput());
		context.begin();
		try {
			if (checkLogin(context)) {
				doServlet(context);
			}
		}
		catch (Exception e) {
			context.getRet().setError("9999", "系统错误");
		}
		context.commit();
	}

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
			saveNotification(context);
	}
	
	/**
	 * Print all request headers and parameters of a notification to System.out
	 */
	private boolean saveNotification(SelvetContext<ApiContext> context) {
		PayCallbackHelper manager = new PayCallbackHelper();
		manager.adyenOnlinePayResult(context);
		return true;
	}

}
