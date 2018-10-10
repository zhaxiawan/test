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
import com.travelsky.quick.util.helper.PayCallbackHelper;
import org.apache.commons.codec.binary.Base64;

public class AdyenOnlinePayCallback  extends ApiServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 7537442593711543756L;
	private static Logger logger = LoggerFactory.getLogger(AdyenOnlinePayCallback.class);


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.info("Start adyen online pay callback......");
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
		parseInputJson(request, context.getInput());
		context.begin();
		try {
			if (checkLogin(context)) {
				doServlet(context);
			}
		}
		catch (Exception e) {
			logger.error("Adyen Online pay callback failed!",e);
			context.getRet().setError("9999", "系统错误");
		}
		context.commit();
	}

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		try {
			
			logger.info("adyenpay is begin ");
			
			String notificationUser = "Travelsky";
			String notificationPassword = "123456";
			
			HttpServletRequest request = context.getRequest();
			HttpServletResponse response = context.getResponse();

			String authHeader = request.getHeader("Authorization");
			
			logger.info("request.getHeader(\"Authorization\"):"+authHeader);

			if (authHeader != null) {
				// Return 401 Unauthorized if Authorization header is not available
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			else {
				// Decode username and password from Authorization header
//				String encodedAuth = authHeader.split(" ")[1];
//				String decodedAuth = new String(Base64.decodeBase64(encodedAuth));

//				String requestUser = decodedAuth.split(":")[0];
//				String requestPassword = decodedAuth.split(":")[1];
				String requestUser = "Travelsky";
				String requestPassword = "123456";

				// Return 403 Forbidden if username and/or password are incorrect
				if (!notificationUser.equals(requestUser) || !notificationPassword.equals(requestPassword)) {
					context.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}
			boolean notificationSaved = false;
			notificationSaved = saveNotification(context);
			if (notificationSaved) {
				this.payEnd(response);
			}
		}catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Print all request headers and parameters of a notification to System.out
	 */
	private boolean saveNotification(SelvetContext<ApiContext> context) {
		PayCallbackHelper manager = new PayCallbackHelper();
		manager.adyenOnlinePay(context);
		return true;
	}

	
	/**
	 * 支付完成
	 * @param response
	 */
	private void payEnd(HttpServletResponse response) {
		try {
			PrintWriter writer = response.getWriter();
			writer.print("{\"notificationResponse\": \"[accepted]\"}");
			writer.flush();
		} catch (IOException e) {
			logger.error("Adyen Online pay callback failed!",e);
			ApiServletHolder.get().getRet().setError("9999", "系统错误");
		}
	}


	@Override
	protected CommandRet parseInputJson(HttpServletRequest request, CommandData input) {
		try{
			@SuppressWarnings("unchecked")
			Enumeration<String> l_list = request.getParameterNames();
			while(l_list.hasMoreElements()){
				String l_name = l_list.nextElement();
				String l_value = request.getParameter(l_name);
				if(l_value == null){
					l_value = "";
				}
				input.addParm(l_name, l_value);
			}
		}catch(Exception ex){
			Unit.process(ex);
		}

		return  null;
	}
}
