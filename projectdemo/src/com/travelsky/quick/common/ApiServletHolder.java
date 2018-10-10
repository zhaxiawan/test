package com.travelsky.quick.common;


import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.travelsky.quick.controller.BaseNDCController;
import com.travelsky.quick.log.ats.AtsLogHelper;
import com.travelsky.quick.util.DESUtil;
import com.travelsky.quick.util.RequestUtil;
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * <p>
 * Title: SelvetContext<ApiContext> 的全局 Holder类
 * </p>
 * <p>
 * Description: 取得当前SelvetContext的全局类,采用ThreadLocal,多线程下的 线程副本.
 * </p>
 *
 * @author yanzj
 * @date 2016年6月23日 上午9:50:45
 */
public class ApiServletHolder implements ISerletHolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiServletHolder.class);
	public static ThreadLocal<SelvetContext<ApiContext>> contextHolder = new ThreadLocal<SelvetContext<ApiContext>>() {
		@Override
		protected SelvetContext<ApiContext> initialValue() {
			SelvetContext<ApiContext> context=new SelvetContext<ApiContext>(new ApiContext());
			return context;
		}
	};

	/**
	 * 取得当前线程的holder
	 *
	 * @return
	 */
	public static SelvetContext<ApiContext> get() {
		SelvetContext<ApiContext> sa = contextHolder.get();
		return sa;
	}

	/**
	 * 获取当前线程的ApiContext
	 *
	 * @return
	 */
	public static ApiContext getApiContext() {
		return get().getContext();
	}

	/**
	 * 取得并初始化数据
	 * @param input
	 * @param reqJson
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static SelvetContext<ApiContext> getAndInit(CommandData input, String reqJson) {
		SelvetContext<ApiContext> context = get();
		// Init transacation id
		context.setTransactions(AtsLogHelper.getGloalTransactionID());
		if (input == null) {
			return context;
		}

		ApiContext apiCtx = getApiContext();
		// UserID
		String userID = input.getParm(ApiContext.REQ_PARAM_USERID).getStringColumn();
		if (StringUtils.hasLength(userID)) {
			apiCtx.setUser(userID);
		}

		// TicketDeptid
		String ticketDeptid = input.getParm("AuthTktdeptid").getStringColumn();
		if (StringUtils.hasLength(ticketDeptid)) {
			apiCtx.setTicketDeptid(ticketDeptid);
//			getChannelno(ticketDeptid,context,apiCtx);
		}

		// AppID
		String appID = input.getParm(ApiContext.REQ_PARAM_APPID).getStringColumn();
		if (StringUtils.hasLength(appID)) {
			apiCtx.setAppID(appID);
		}

		// Language
		String language = input.getParm(ApiContext.REQ_PARAM_LANGUAGE).getStringColumn();
		if (StringUtils.hasLength(language)) {
			apiCtx.setLanguage(language);
		}

		// ServiceName
		String serviceName = input.getParm(ApiContext.REQ_PARAM_SERVICENAME).getStringColumn();
		if (StringUtils.hasLength(serviceName)) {
			apiCtx.setServiceName(serviceName);
		}

		// Sign
		String sign = input.getParm(ApiContext.REQ_PARAM_SIGN).getStringColumn();
		if (StringUtils.hasLength(sign)) {
			apiCtx.setSign(sign);
		}

		// Timestamp
		String timestamp = input.getParm(ApiContext.REQ_PARAM_TIMESTAMP).getStringColumn();
		if (StringUtils.hasLength(timestamp)) {
			apiCtx.setTimestamp(timestamp);
		}

		// Version
		String version = input.getParm(ApiContext.REQ_PARAM_VERSION).getStringColumn();
		if (StringUtils.hasLength(version)) {
			apiCtx.setVersion(version);
		}

		if (StringUtils.hasLength(reqJson)) {
			apiCtx.setReqXML(reqJson);
		}

		// Client IP
		String clientIP = input.getParm(ApiContext.REQ_PARAM_CLIENTIP).getStringColumn();
		if (StringUtils.hasLength(clientIP)) {
			apiCtx.setClientIP(clientIP);
		}

		// Server IP
		// 从HttpServletRequest中获取ip无需解密
		apiCtx.setIPArr(RequestUtil.getInstance().getIpAddress(context.getRequest()));
		apiCtx.setTimeZone("+08:00");
		return context;
	}

	/**
	 * 取得并初始化数据
	 *
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static SelvetContext<ApiContext> getAndInit(boolean isDecry) throws Exception {
		// 上下文容器
		SelvetContext<ApiContext> context = get();
		ApiContext apiContext = context.getContext();
		HttpServletRequest req = context.getRequest();
		// UserID
		String userID = req.getParameter(ApiContext.REQ_PARAM_USERID);
		if (!StringUtils.hasLength(userID)) {
			userID = (String) req.getAttribute(ApiContext.REQ_PARAM_USERID);
		}
		apiContext.setUser(userID);


		// tktdeptid
		String  tktdeptid = req.getParameter(ApiContext.REQ_PARAM_TKTDEPTID);

		if (!StringUtils.hasLength(tktdeptid)) {
			tktdeptid = (String) req.getAttribute(ApiContext.REQ_PARAM_TKTDEPTID);
		}

		apiContext.setTicketDeptid(tktdeptid);

		// AppID
		String appID = req.getParameter(ApiContext.REQ_PARAM_APPID);
		if (!StringUtils.hasLength(appID)) {
			appID = (String) req.getAttribute(ApiContext.REQ_PARAM_APPID);
		}
		apiContext.setAppID(appID);
		// ServiceName
		String serviceName = req.getParameter(ApiContext.REQ_PARAM_SERVICENAME);
		if (serviceName == null) {
			serviceName = (String) req.getAttribute(ApiContext.REQ_PARAM_SERVICENAME);
		}
		apiContext.setServiceName(serviceName);

		// Timestamp
		String timestamp = req.getParameter(ApiContext.REQ_PARAM_TIMESTAMP);
		if (!StringUtils.hasLength(timestamp)) {
			timestamp = (String) req.getAttribute(ApiContext.REQ_PARAM_TIMESTAMP);
		}
		apiContext.setTimestamp(timestamp);

		// ReqXML
		String reqXML = req.getParameter(ApiContext.REQ_PARAM_XML);
		if (!StringUtils.hasLength(reqXML)) {
			reqXML = (String) req.getAttribute(ApiContext.REQ_PARAM_XML);
		}

		// 解码
		try {
			if(reqXML != null){
				apiContext.setReqXML(reqXML);
			}
		} catch (Exception e) {
			LOGGER.error("Request content decoding errors", e);
			return null;
		}

		// Language
		String language = req.getParameter(ApiContext.REQ_PARAM_LANGUAGE);
		if (!StringUtils.hasLength(language)) {
			language = (String) req.getAttribute(ApiContext.REQ_PARAM_LANGUAGE);
		}
		apiContext.setLanguage(language);

		// Sign
		String sign = req.getParameter(ApiContext.REQ_PARAM_SIGN);
		if (!StringUtils.hasLength(sign)) {
			sign = (String) req.getAttribute(ApiContext.REQ_PARAM_SIGN);
		}
		apiContext.setSign(sign);

		// Version
		String version = req.getParameter(ApiContext.REQ_PARAM_VERSION);
		if (StringUtils.hasLength(version)) {
			apiContext.setVersion(version);
		}
		else {
			version = (String) req.getAttribute(ApiContext.REQ_PARAM_VERSION);
			if (StringUtils.hasLength(version)) {
				apiContext.setVersion(version);
			}
		}

		// Client IP
		String clientIP = req.getParameter(ApiContext.REQ_PARAM_CLIENTIP);
		if (StringUtils.hasLength(clientIP)) {
			apiContext.setClientIP(clientIP);
		}
		else {
			clientIP = (String) req.getAttribute(ApiContext.REQ_PARAM_CLIENTIP);
			if (StringUtils.hasLength(clientIP)) {
				apiContext.setClientIP(clientIP);
			}
		}

		// Server IP
		apiContext.setIPArr(RequestUtil.getInstance().getIpAddress(req));
		// TimeZone
		String timeZone = req.getParameter(ApiContext.REQ_PARAM_TIMEZONE);
		if (StringUtils.hasLength(timeZone)) {
			apiContext.setTimeZone(timeZone);
		}else{
			timeZone = (String) req.getAttribute(ApiContext.REQ_PARAM_TIMEZONE);
			if (StringUtils.hasLength(timeZone)) {
				apiContext.setTimeZone(timeZone);
			}
		}
		return context;
	}

	/**
	 * 移出当前线程的Holder数据.
	 */
	public static void destory() {
		if (contextHolder.get() != null) {
			contextHolder.remove();
		}
	}


	
	
//	/**
//	 *根据部门编号获取渠道号
//	 * @param ticketDeptid
//	 * @param context
//	 * @param apiCtx
//	 */
//	public static void getChannelno(String ticketDeptid,SelvetContext<ApiContext> context
//			,ApiContext apiCtx){
//		// 先从缓存中获取部门信息
//		String key = new StringBuilder("APIDEPT_").append(ticketDeptid).toString();
//		String deptInf = RedisManager.getManager().get(key);
//		String channel ="";
//		String timeZone = "";
//		CommandData deptData = new CommandData();
//		boolean callCommand = false;
//		if(StringUtils.hasLength(deptInf)) {
//			JsonUnit.fromJson(deptData, deptInf);
//			channel=deptData.getParm("channel").getStringColumn();
//			timeZone=deptData.getParm("timeZone").getStringColumn();
//
//			callCommand=!StringUtils.hasLength(channel)||!StringUtils.hasLength(timeZone);
//		}
//
//		if(callCommand){
//			//channelno
//			CommandInput l_input = new CommandInput("com.cares.sh.order.dept.query");
//			l_input.addParm("code", ticketDeptid);
//			CommandRet l_ret = context.doOther(l_input,false);
//			Table taxtab = l_ret.getParm("department").getTableColumn();
//
//			if (taxtab != null) {
//				for (int i = 0; i < taxtab.getRowCount(); i++) {
//					Row taxrow = taxtab.getRow(i);
//					channel = taxrow.getColumn("channel").getStringColumn();
//					timeZone = taxrow.getColumn("timeZone").getStringColumn();
//				}
//
//				deptData.addParm("channel", channel);
//				deptData.addParm("timeZone", timeZone);
//
//				RedisManager.getManager().set(key, JsonUnit.toJson(deptData), 3);
//			}
//		}
//
//		if (StringUtils.hasLength(channel)) {
//			apiCtx.setChannelNo(channel);
//		}
//		if (StringUtils.hasLength(timeZone)) {
//			apiCtx.setTimeZone(timeZone);
//		}
//	}
	public static void Init(BaseNDCController base) throws Exception {
		if (TipMessager.getMsgkeysrcMap().size()<=0) {
			base.getTipResult("/WEB-INF/conf/msgkeyMap_en_US.properties",TipMessager.getMsgkeysrcMap());
			base.getTipResult("/WEB-INF/conf/message_en_US.properties",TipMessager.getMsgsrcMap());
		}
	}
}
