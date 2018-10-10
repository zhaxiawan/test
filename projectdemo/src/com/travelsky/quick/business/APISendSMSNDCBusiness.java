package com.travelsky.quick.business;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCSendMessageRQDocument;
import org.iata.iata.edist.LCCSendMessageRQDocument.LCCSendMessageRQ;
import org.iata.iata.edist.LCCSendMessageRQDocument.LCCSendMessageRQ.Query.LCCSendMessage.Method;
import org.iata.iata.edist.LCCSendMessageRSDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.MessageManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * @author 作者:LiHz
 * @version 0.1
 * 类说明:
 *		MSGTYPE_SMS   发送短信
 *
 */
@Service("LCC_SENDSMS_SERVICE")
public class APISendSMSNDCBusiness extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4118011091322993212L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APISendSMSNDCBusiness.class);
	
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean
			context.setRet(getResponseBean());
		}catch(APIException e){
			throw e;
		}catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_SEND_MESSAGE, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	////将CommadRet 转为  xmlbean
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
			//转换ResponseBean-->XmlBean
			return transRespBeanToXmlBean(commandRet,input);
	}

	
	
	
	/**
	 * 转换 xml-->Reqbean
	 * @param context shopping所用的一个集合
	 * @param xmlInput 前台获取的xml数据
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCSendMessageRQDocument rootDoc = null;
		rootDoc = LCCSendMessageRQDocument.Factory.parse(xmlInput);

		LCCSendMessageRQ reqdoc = rootDoc.getLCCSendMessageRQ();
		// 部门ID
		input.addParm("tktdeptid",ApiServletHolder.getApiContext().getTicketDeptid());		
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		//获取短信功能类型
		String actionType = reqdoc.getQuery().getActionType();
		if(!StringUtils.hasLength(actionType)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_SMS_MESSAGETYPE, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_SMS_MESSAGETYPE);
		}	
		input.addParm("actionType", actionType);
		//根据actionType判断是否是找回密码发送邮件
		if ("FORGETPWD_SEND_EMAIL".equals(actionType)) {
			String userId = reqdoc.getQuery().getLCCUserInfo().getUserID();
			input.addParm("userId", userId);
			String sendTimeLimit = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_forgetpwd.toKey("timelimit:")+userId);
			if(!StringUtils.hasLength(sendTimeLimit)){
				RedisManager.getManager().set(RedisNamespaceEnum.api_cache_forgetpwd.toKey("timelimit:")+userId, "60秒时间限制", 60);
				transInputXmlToRequestBeanEmail(input, reqdoc, language, context);
			}else{
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_ONLYONECANGRENERATEDINOENMINUTE_CODE, language));
				throw APIException.getInstance(ErrCodeConstants.API_ONLYONECANGRENERATEDINOENMINUTE_CODE);
			}	
			return ;
		}
		//获取手机号
		Method[] method	= reqdoc.getQuery().getLCCSendMessage().getMethodArray();
		if(null != method && method.length>0){
			//手机号 多个用','分隔
			if(!StringUtils.hasLength(method[0].getNumber())){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_MOBILE, language));
				 throw APIException.getInstance(ErrCodeConstants.API_NULL_MOBILE);
			 }	
			input.addParm("mobile", method[0].getNumber());
			input.addParm("areaCode", method[0].getAreaCode());
		}else{
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_MOBILE, language));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_MOBILE);
		}
		input.addParm("memberid",  context.getContext().getUserID());

	}
	
	/**
	 * 数据提交shopping后台
	 * @param input  请求的XML参数
	 * @param context 用于调用doOther请求后台数据
	 * @return  请求后台返回的对象
	 */
	public CommandRet getResponseBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MessageManager messageManager = new MessageManager();
		//发送短信Shopping后台请求获取
		String actionType=input.getParm("actionType").getStringColumn();
		if ("FORGETPWD_SEND_EMAIL".equals(actionType)) {
			return messageManager.sendEmail(input, context);
		}else{
			return messageManager.sendsmsndc(input, context);
		}
		
	}
	
	/**
	 * EMAIL
	 * @param input
	 * @param reqdoc
	 * @param language
	 * @param context
	 * @throws APIException
	 * @throws Exception
	 */
	public void transInputXmlToRequestBeanEmail(CommandData input,LCCSendMessageRQ reqdoc,String language,SelvetContext<ApiContext> context)throws APIException, Exception  {
		//获取邮箱号
		Method[] method	= reqdoc.getQuery().getLCCSendMessage().getMethodArray();
		if(null != method && method.length>0){
			//邮箱号只有一个
			if(!StringUtils.hasLength(method[0].getNumber())){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_EMAIL_ADDRESS, language));
				 throw APIException.getInstance(ErrCodeConstants.API_NULL_EMAIL_ADDRESS);
			 }	
			input.addParm("email", method[0].getNumber());
			
		}else{
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_EMAIL_ADDRESS, language));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_EMAIL_ADDRESS);
		}
		//渠道号
		input.addParm("tktdeptid", ApiServletHolder.getApiContext().getTicketDeptid());
		//邮件内容
		input.addParm("memberid",  context.getContext().getUserID());
		
		
	}
	
	/**
	 * 转换ResponseBean-->XmlBean
	 * @param commandRet 后台返回的结果集
	 * @param input  B2C请求的XML
	 * @return  XML结果集
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet ,CommandData input)  {
		CommandRet xmlOutput = (CommandRet)commandRet;
		LCCSendMessageRSDocument doc = LCCSendMessageRSDocument.Factory.newInstance();
		LCCSendMessageRSDocument.LCCSendMessageRS rs = doc.addNewLCCSendMessageRS();
		try{
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}
			//反回无吴
			else{
				rs.addNewSuccess();
			}
		}
		catch (Exception e) {
			//初始化XML节点
			doc = LCCSendMessageRSDocument.Factory.newInstance();
			rs = doc.addNewLCCSendMessageRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}
}
