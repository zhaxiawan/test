package com.travelsky.quick.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCCommonRSDocument;
import org.iata.iata.edist.LCCCommonRSDocument.LCCCommonRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.services.s3.model.GetBucketAnalyticsConfigurationResult;
import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.log.ats.AtsLogHelper;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DESUtil;
import com.travelsky.quick.util.concurrent.APIConcurrentManager;
import com.travelsky.quick.util.concurrent.ConcurrentManageService.ActionType;
import com.travelsky.quick.util.helper.ConfigurationManager;
import com.travelsky.quick.util.helper.TipMessager;
import com.travelsky.quick.util.validate.AbstractBaseValidator;
import com.travelsky.quick.util.validate.BaseValidator;

/**
 *
 * @author Administrator
 *
 */
public class BaseNDCController extends HttpServlet {
	private static final long serialVersionUID = 3221141130204194571L;
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseNDCController.class);
    
	
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)  {
		try {
			// Init 上下文容器SelvetContext
			SelvetContext<ApiContext> context = ApiServletHolder.get();
			context.setRequest(req);
			context.setResponse(resp);
			context.setInput(new CommandData());
			context.setRet(new CommandRet(""));
			//初始化各类信息
			ApiServletHolder.Init(this);
			// true 解密  false 不解密
			boolean isDecry = false;
			req.setCharacterEncoding("utf-8");
			// 为容器赋初始d
			try {
				if (ApiServletHolder.getAndInit(isDecry) == null) {
					throw new Exception();
				}
			}
			catch (Exception e) {
				// 初始化失败
				processError(ErrCodeConstants.API_SYSTEM, new Date());
				return ;
			}

			
			
			
			// 获取service
			AbstractService<ApiContext> service = getService();
			if (service == null) {
				choiceServiceErr();
			} else {
				processRequest(false, service);
				// 处理响应
				service.processResponse();
			}
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_SYSTEM, null), e);			
			return ;
		}
		finally {
			ApiContext apiCtx = ApiServletHolder.getApiContext();
			try {
				// 减并发数
				APIConcurrentManager.getInstance().operate(ActionType.SUB, apiCtx.getAppID(), apiCtx.getServiceName(), apiCtx.getVersion());
			}
			catch (Exception e) {
				LOGGER.error(TipMessager.getInfoMessage(
						ErrCodeConstants.API_UNKNNOW_CONCURRNET, apiCtx.getLanguage()), e);
			}

			// 清除当前线程上下文,释放资源
			ApiServletHolder.destory();
		}
	}

	/**
	 * 选择Service name发生异常
	 * @throws IOException
	 */
	private void choiceServiceErr() throws IOException {
		//判断验签时是否有异常返回，如果有则返回验签中code，没有正常返回code
		 String errorCode = ApiServletHolder.get().getRet().getErrorCode();
		if (StringUtils.hasLength(errorCode)) {
			processError(errorCode, new Date());
		}else {
			processError(ErrCodeConstants.API_ENUM_SERVICENAME, new Date());
		}
	}

	private void processError(String code, Date startTime) throws IOException {
		LCCCommonRSDocument doc = LCCCommonRSDocument.Factory.newInstance();
		LCCCommonRS root = doc.addNewLCCCommonRS();
		ErrorType error = root.addNewErrors().addNewError();
		error.setCode(TipMessager.getErrorCode(code));
		error.setStringValue(TipMessager.getMessage(code,
				ApiServletHolder.getApiContext().getLanguage()));

		HttpServletResponse resp = ApiServletHolder.get().getResponse();
		resp.setContentType("text/html;charset=utf-8");
		try {
			String res=ApiServletHolder.getApiContext().isRtnEncrypt()?
					DESUtil.newEncrypt(AbstractService.transObjectToOutputXML(doc, "UTF-8"),ParmExUtil.getDesKey()):
						AbstractService.transObjectToOutputXML(doc, "UTF-8");
			int len=res.getBytes().length;
			resp.setHeader("Content-Length", String.valueOf(len));
			resp.getWriter().write(res);
		} catch (Exception e) {
			LOGGER.error("Return value encrypt failed!",e);
			resp.getWriter().write(AbstractService.transObjectToOutputXML(doc, "UTF-8"));
		}
		
		// 跟踪带日志返回值开始
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		Date endTime = new Date();
		//计算Servlet调用所花费时间（单位：毫秒）
		long timeCost = endTime.getTime() - startTime.getTime();
		Map<String, String> extend = new HashMap<>();
		extend.put ("TimeCost", Long.toString(timeCost));
		//获取配置中日志中不包含xml数据的接口数据
		String inxl = ConfigurationManager.getINXL(context);
		//输出跟踪带日志STAT_OUT部分
		if(StringUtils.isEmpty(context.getContext().getServiceName()) 
				|| inxl.contains(context.getContext().getServiceName())){
			AtsLogHelper.outputAtsLog("STAT_OUT", this.getClass().getName() + "-processResponse",
					context.getTransactions(), context.getContext().getUserID(), extend, "RES:"+context.getRet().getErrorCode());
		}else{
			AtsLogHelper.outputAtsLog("STAT_OUT", this.getClass().getName() + "-processResponse",
					context.getTransactions(), context.getContext().getUserID(), extend, AbstractService.transObjectToOutputXML(doc, "UTF-8"));
		}
	}

	/**
	 * 执行业务处理
	 * @param isErr
	 * @param service
	 */
	private void processRequest(boolean isErr,
			AbstractService<ApiContext> service) {
		try {
			if (!isErr) {
				// 处理请求
				service.processRequest();
			}
		} catch (Exception e) {
			String language = ApiServletHolder.getApiContext().getLanguage();
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_SYSTEM, language), e);

			SelvetContext<ApiContext> context = ApiServletHolder.get();
			context.getRet().setError(
					ErrCodeConstants.API_SYSTEM,
					TipMessager.getMessage(ErrCodeConstants.API_SYSTEM, language));
		}
	}

	/**
	 * 根据ServiceName获取Service
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private AbstractService<ApiContext> getService() {
		ApiContext apiCtx = ApiServletHolder.getApiContext();
		String serviceName = apiCtx.getServiceName();
		WebApplicationContext springCtx = ApiContext.getApplicationContext();
		AbstractService<ApiContext> service = null;

		if (!StringUtils.hasLength(serviceName)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_SERVICENAME,
					apiCtx.getLanguage()));
		}
		else {
			try {
				service=springCtx.getBean(serviceName, AbstractService.class);
			}
			catch (NoSuchBeanDefinitionException e) {
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_ENUM_SERVICENAME,
						apiCtx.getLanguage()), e);
				service = null;
			}
		}

		return service;
	}
	public  void  getTipResult(String file,Map<String, String> map) throws Exception {
		 String strR="";
			InputStream data = getServletContext().getResourceAsStream(file);
			byte[] b = new byte[data.available()];
			data.read(b);
			data.close();
			strR= new String(b);
			String[] split = strR.split("\r\n");
			for (String string : split) {
				String[] split2 = string.split("=");
				map.put(split2[0], split2[1]);
			}
	}
	
	
}