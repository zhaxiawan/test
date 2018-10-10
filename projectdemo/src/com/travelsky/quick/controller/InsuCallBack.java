package com.travelsky.quick.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.SystemConfig;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.log.ats.AtsLogHelper;

public class InsuCallBack extends ApiServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 7537442593711543756L;
	private static Logger logger = LoggerFactory.getLogger(InsuCallBack.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.info("Start insu online pay callback......");
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 初始化支付回调信息
		ApiContext apiCtx = context.getContext();
		context.setRequest(request);
		context.setResponse(response);
		ParmExUtil.build(apiCtx);
		apiCtx.setServiceName(CommonConstants.API_PAYCALLBACK_SEVNAME);
		logger.info("Insu call back user is:[{}], appid is:[{}], deptid is:[{}], channel is:[{}]", apiCtx.getUserID(),
				apiCtx.getAppID(), apiCtx.getTicketDeptid(), apiCtx.getChannelNo());
		// 获取请求参数
		parseInputJson(request, context.getInput());

		context.begin();

		// ====日志部分开始====
		// 如果不存在全局TransactionID，则生成一个
		if (context.getTransactions().equals("")) {
			context.initTransactions();
		}
		Date startTime = new Date();
		// 取得module名
		String module = this.getClass().getName() + "-doPost";
		// 将模块名中【.】换为【-】,因为【.】属于日志头部分的保留字符
		// 取得TransactionID
		String tid = context.getTransactions();
		module = module.replace('.', '-');
		// 取得userid
		String userid = context.getContext().getUserID();
		// 生成扩展区
		HashMap<String, String> extend = new HashMap<String, String>();
		extend.put("IP", SystemConfig.getServerIp());
		extend.put("Action", context.getContext().getServiceName());
		extend.put("OnlineCallBack", "insu");
		// ====日志部分结束====
		try {
			if (checkLogin(context)) {
				// context.initTransactions();
				SystemConfig.setTID(context.getTransactions());
				// context.log();
				// Add Begin
				// 输出跟踪带日志STAT_IN部分
				AtsLogHelper.outputAtsLog("STAT_IN", module, tid, userid, extend, JsonUnit.toJson(context.getInput()));
				// Add End
				doServlet(context);
			}
		} catch (Exception e) {
			logger.error("Online insu callback failed!", e);
			context.getRet().setError("9999", "系统错误");
		}

		context.commit();
		// ====日志部分开始====
		if (this.checkLogin(context)) {
			Date endTime = new Date();
			// 计算Servlet调用所花费时间（单位：毫秒）
			long timeCost = endTime.getTime() - startTime.getTime();
			extend.put("TimeCost", Long.toString(timeCost));
			// 输出跟踪带日志STAT_OUT部分
			AtsLogHelper.outputAtsLog("STAT_OUT", module, tid, userid, extend, JsonUnit.toJson(context.getRet()));
		}
		// ====日志部分结束====
	}

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		CommandInput input0 = new CommandInput("com.cares.sh.insurance.callback");
		CommandData input = context.getInput();
		input.copyTo(input0);
		context.doOther(input0, false);
	}

	/*
	 * 解析xml 流的内容
	 * 
	 * (non-Javadoc)
	 * @see com.travelsky.quick.controller.ApiServlet#parseInputJson(javax.servlet.http.HttpServletRequest, com.cares.sh.parm.CommandData)
	 */
	@Override
	protected CommandRet parseInputJson(HttpServletRequest request, CommandData input) {

		try {
			//获取流的数据
			BufferedReader br = new BufferedReader(
					new InputStreamReader((ServletInputStream) request.getInputStream(), "utf-8"));
			StringBuffer sb = new StringBuffer("");
			String temp;
			while ((temp = br.readLine()) != null) {
				sb.append(temp);
			}
			br.close();
			String xml = sb.toString().trim();
			input.addParm("xml", xml);
			
			
			
			//获取key
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
			
			
		} catch (Exception ex) {
			Unit.process(ex);
		}
		
		return null;
	}
}
