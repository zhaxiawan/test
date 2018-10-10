package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserModifyRQDocument;
import org.iata.iata.edist.LCCUserModifyRQDocument.LCCUserModifyRQ.Modify;
import org.iata.iata.edist.LCCUserModifyRSDocument;
import org.iata.iata.edist.LCCUserModifyRSDocument.LCCUserModifyRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;


/**
 * 
 * @author MaRuifu 2016年5月3日下午3:12:11
 * @version 0.1
 *  类说明:信息修改
 *	  		contactInfoUpdate联系方式信息修改
 */
@Service("LCC_USERCONTACTMETHODMODIFY_SERVICE")
public class APIUserContactMethodModifyBusiness extends AbstractService<ApiContext> {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserContactMethodModifyBusiness.class);
	
	private static final String  MEMBERID = "memberid";
	/**
	 * 
	 * @param  context SelvetContext<ApiContext>
	 * @throws Exception Exception
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
		try{
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean                            
			context.setRet(getResult());
		}
		catch (APIException e) {
            throw e;
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_USER_MODIFY,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}
	  /**
	   * 
	   * @param input CommandData
	   * @param context SelvetContext
	   * @return 
	   * CommandRet    返回类型 
	   */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MemberManager memberManager=new MemberManager();
		String actionType=input.getParm("actionType").getStringColumn();
		if ("USER_MODIFY_CONTACTMETHOD_EMAIL".equals(actionType)) {
			return memberManager.updContactEmail(input, context);
		}else{
			return memberManager.updContact(input, context);
		}
		
	}
	
	/**
	 * 
	 * @param context SelvetContext
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCUserModifyRQDocument rootDoc = null;
		rootDoc = LCCUserModifyRQDocument.Factory.parse(xmlInput);
		LCCUserModifyRQDocument.LCCUserModifyRQ reqDoc = rootDoc.getLCCUserModifyRQ();
		String channelNo = context.getContext().getChannelNo();
		//渠道号
		input.addParm("channelno", channelNo);
		input.addParm("tktdeptid",context.getContext().getTicketDeptid());
		// 添加用户id  orderno
		input.addParm("memberid", context.getContext().getUserID());
		//修改信息
		Modify modifyinfo = reqDoc.getModify();
		//修改信息类型
		String  actionType = modifyinfo.getActionType();
		if (!StringUtils.hasLength(actionType)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ACTIONTYPE, 
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACTIONTYPE);
		}
		input.addParm("actionType", actionType);
		//会员信息
		LCCUserType  lCCUserInfo = modifyinfo.getLCCUserInfo();
		//会员账号
		String memberid = context.getContext().getUserID();
		if (!StringUtils.hasLength(memberid)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_USER_ID, 
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		//修改联系方式
		getContactInfo(input, lCCUserInfo, memberid);
	}

	/**
	 * 
	 * @param input CommandData
	 * @param lCCUserInfo LCCUserType
	 * @param memberid String
	 */
	private void getContactInfo(CommandData input, LCCUserType lCCUserInfo, String memberid)
		throws APIException {
		//联系信息
		ContactInfo contactInfo = lCCUserInfo.getContactInfoArray(0);
		if (contactInfo==null) {
			LOGGER.info(TipMessager.getInfoMessage(
		    		ErrCodeConstants.API_NULL_CONTACT, 
		    		ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT);
		}
		//ID:联系方式的标识
		String id = contactInfo.getID();
		if (!StringUtils.hasLength(id)) {
			LOGGER.info(TipMessager.getInfoMessage(
		    		ErrCodeConstants.API_NULL_CONTACT_ID, 
		    		ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_ID);
		}
		input.addParm("id", id);
		//type 联系方式 tel电话，mobile手机，email电邮，qq QQ，webchat 微信
		String type = contactInfo.getType();
		if(StringUtils.hasLength(type)){
			input.addParm("type", type);
		}
		//number号码
		String number = contactInfo.getNumber();
		if(StringUtils.hasLength(number)){
			input.addParm("no", number);
		}
		//区号
		String areaCode = contactInfo.getAreaCode();
		if(!StringUtils.isEmpty(areaCode)){
			input.addParm("area", areaCode);
		}
		input.addParm(MEMBERID, memberid);
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param xmlOutput CommandRet
	 * @param input  CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet xmlOutput ,CommandData input) {
		
		LCCUserModifyRSDocument sadoc = LCCUserModifyRSDocument.Factory.newInstance();
		LCCUserModifyRS rprs = sadoc.addNewLCCUserModifyRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}else{
				rprs.addNewSuccess();
			}
		}
		catch (Exception e) {
			sadoc = LCCUserModifyRSDocument.Factory.newInstance(); 
			rprs = sadoc.addNewLCCUserModifyRS();                  
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return sadoc;
	}
}
