package com.travelsky.quick.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.SelvetContext;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.util.helper.PayCallbackHelper;

public class WorldPayCallBack extends ApiServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3340740323857274711L;
	private static Logger logger = LoggerFactory.getLogger(WorldPayCallBack.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.info("Start worldPay callback......");
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 初始化支付回调信息
		ApiContext apiCtx = context.getContext();
		ParmExUtil.build(apiCtx);
		context.setRequest(request);
		context.setResponse(response);
		apiCtx.setServiceName(CommonConstants.API_PAYCALLBACK_SEVNAME);
		logger.info("Pay call back user is:[{}], appid is:[{}], deptid is:[{}], channel is:[{}]",
				apiCtx.getUserID(),
				apiCtx.getAppID(),
				apiCtx.getTicketDeptid(),
				apiCtx.getChannelNo());
		// 获取请求参数
//		parseInputJson(request, context.getInput());
		context.begin();
		try {
			if (checkLogin(context)) {
				doServlet(context);
			}
		}
		catch (Exception e) {
			logger.error("world pay callback failed!",e);
			context.getRet().setError("9999", "系统错误");
		}
		context.commit();
	}
	
	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		HttpServletResponse response = context.getResponse();

		boolean notificationSaved = false;
		notificationSaved = saveNotification(context);
		if (notificationSaved) {
			this.payEnd(response);
		}
	}
	
	private boolean saveNotification(SelvetContext<ApiContext> context) {
		PayCallbackHelper manager = new PayCallbackHelper();
		manager.worldPay(context);
		return true;
	}
	
	/**
	 * 支付完成
	 * @param response
	 */
	private void payEnd(HttpServletResponse response) {
		try {
			PrintWriter writer = response.getWriter();
			writer.print("{\"notificationResponse\":\"[OK]\",\"statusCode\":\"200\"}");
			writer.flush();
		} catch (IOException e) {
			logger.error("world pay callback failed!",e);
			ApiServletHolder.get().getRet().setError("9999", "系统错误");
		}
	}
}
