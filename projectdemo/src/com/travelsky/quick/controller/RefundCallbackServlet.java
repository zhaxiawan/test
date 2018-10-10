package com.travelsky.quick.controller;

import java.io.IOException;
import java.io.PrintWriter;
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
import com.travelsky.quick.util.helper.RefundCallbackHelper;

/**
 * easypay退款回调
 * @author hu
 *
 */
public class RefundCallbackServlet extends ApiServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 7537442593711543756L;
	private static Logger logger = LoggerFactory.getLogger(RefundCallbackServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		logger.debug("Start refund callback......");
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 初始化支付回调信息
		ApiContext apiCtx = context.getContext();
	/*	// 设置默认用户
		String user=SystemConfig.getConfig().getItemString("PAYCALLBACK", "USERID","SYS_ADMIN");
		apiCtx.setUserID(user);
		// 设置默认APPID
		String appId=SystemConfig.getConfig().getItemString("PAYCALLBACK", "APPID","LT_WEB");
		apiCtx.setAppID(appId);
		// 设置默认部门
		String deptId=SystemConfig.getConfig().getItemString("PAYCALLBACK", "DEPTID","SYS_DEPT");
		apiCtx.setTicketDeptid(deptId);
		// 设置渠道
		String channelNo = SystemConfig.getConfig().getItemString("PAYCALLBACK", "CHANNEL","CTO");
		apiCtx.setChannelNo(channelNo);*/
		ParmExUtil.build(apiCtx);
		apiCtx.setServiceName(CommonConstants.API_REFUNDCALLBACK_SEVNAME);
		logger.debug("Refund call back user is:[{}], appid is:[{}], deptid is:[{}], channel is:[{}]",
				apiCtx.getUserID(),
				apiCtx.getAppID(),
				apiCtx.getTicketDeptid(),
				apiCtx.getChannelNo());

		// 获取请求参数
		parseInputJson(request, context.getInput());

		context.begin();
		//====日志部分开始====
		//如果不存在全局TransactionID，则生成一个
		if (context.getTransactions().equals("")) {
			context.initTransactions();
		}
		Date startTime = new Date();
		//取得module名
		String module = this.getClass().getName() + "-doPost";
		//将模块名中【.】换为【-】,因为【.】属于日志头部分的保留字符
		//取得TransactionID
		String tid = context.getTransactions();
		module = module.replace('.','-') ;
		//取得userid
		String userid = context.getContext().getUserID();
		//生成扩展区
		HashMap<String,String> extend = new HashMap<String,String>();
		extend.put ("IP", SystemConfig.getServerIp());
		extend.put ("Action", context.getContext().getServiceName());
		//====日志部分结束====
		try {
			if (checkLogin(context)) {
//				context.initTransactions();
				SystemConfig.setTID(context.getTransactions());
				//输出跟踪带日志STAT_IN部分
				AtsLogHelper.outputAtsLog("STAT_IN", module, tid, userid, extend, JsonUnit.toJson(context.getInput()));
//				context.log();
				doServlet(context);
			}
		}
		catch (Exception e) {
			logger.error("Online refund callback failed!",e);
			context.getRet().setError("9999", "系统错误");
		}

		context.commit();
		//====日志部分开始====
		if(this.checkLogin(context)){
			Date endTime = new Date();
			//计算Servlet调用所花费时间（单位：毫秒）
			long timeCost = endTime.getTime() - startTime.getTime();
			extend.put ("TimeCost", Long.toString(timeCost));
			//输出跟踪带日志STAT_OUT部分
			AtsLogHelper.outputAtsLog("STAT_OUT", module, tid, userid, extend, JsonUnit.toJson(context.getRet()));
		}
		//====日志部分结束====
	}

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		RefundCallbackHelper manager = new RefundCallbackHelper();
		// easypay验签
		if (manager.verfiySign(context)) {
			CommandData input = context.getInput();
			// 退款状态
			String refundStatus = input.getParm("refundstatus").getStringColumn();
			logger.debug("refund status is:{}",refundStatus);
			// 退款失败
			if ("3".equals(refundStatus)) {
				logger.info(String.format("Refund failed! orderno:%s,money:%s,error:%s",
						input.getParm("orderno").getStringColumn(),
						input.getParm("refundamount").getStringColumn(),
						input.getParm("msg").getStringColumn()));
			}
			// 退款成功
			else if("2".equals(refundStatus)){
				// 退款
				logger.debug("Start refund call back method");
				manager.refundCallback(context);
			}
		}

		payEnd(context.getResponse());
	}

	/**
	 * 退款完成
	 * @param response
	 */
	private void payEnd(HttpServletResponse response) {
		try {
			PrintWriter writer =response.getWriter();
			logger.debug("Online refund callback complete......");
			writer.print("OK");
			writer.flush();
		} catch (IOException e) {
			logger.error("Online refund callback failed!",e);
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
