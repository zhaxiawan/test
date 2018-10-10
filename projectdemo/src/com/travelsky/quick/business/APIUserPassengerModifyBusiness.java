package com.travelsky.quick.business;

import java.util.Date;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserModifyRQDocument;
import org.iata.iata.edist.LCCUserModifyRQDocument.LCCUserModifyRQ.Modify;
import org.iata.iata.edist.LCCUserModifyRSDocument;
import org.iata.iata.edist.LCCUserModifyRSDocument.LCCUserModifyRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo.ContactInfo;
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
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;


/**
 * 
 * @author MaRuifu 2016年5月3日下午3:12:11
 * @version 0.1
 *  类说明:信息修改
 *			passInfoUpdate乘机人信息修改
 */
@Service("LCC_PASSENGERMODIFY_SERVICE")
public class APIUserPassengerModifyBusiness extends AbstractService<ApiContext> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserPassengerModifyBusiness.class);
	
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
		return memberManager.updPassenger(input, context);
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
	/*	//原密码
		String oldPassword = lCCUserInfo.getOldPassword();
		if(StringUtils.hasLength(oldPassword)){
			input.addParm("oldPassword", oldPassword);
		}*/
		//常用乘机人
		getPassenger(input, lCCUserInfo, memberid);
	}

	/**
	 * 乘机人信息
	 * @param input CommandData
	 * @param lCCUserInfo LCCUserType
	 * @param memberid String
	 */
	private void getPassenger(CommandData input, LCCUserType lCCUserInfo,
			String memberid) throws APIException {
		//LCCPassengerInfo乘机人信息
		LCCPassengerInfo  lCCPassengerInfo = lCCUserInfo.getLCCPassengerInfoArray(0);
		if(lCCPassengerInfo==null){
			LOGGER.info(TipMessager.getInfoMessage(
		    		ErrCodeConstants.API_NULL_PAXS, 
		    		ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS);
		}
		//乘机人ID标志
		String id = lCCPassengerInfo.getPassengerID();
		if(!StringUtils.hasLength(id)){
			LOGGER.info(TipMessager.getInfoMessage(
		    		ErrCodeConstants.API_NULL_PASSENGER_ID, 
		    		ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PASSENGER_ID);
		}
		input.addParm("id", id);
		//性别
		String gender = lCCPassengerInfo.getGender();
		if(StringUtils.hasLength(gender)){
			input.addParm("gender", gender);
		}
		//乘机人姓
		String lastname = lCCPassengerInfo.getPersonNameArray(0).getSurname();
		if(!StringUtils.hasLength(lastname)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_SURNAME, 
					ApiServletHolder.getApiContext().getLanguage()));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_SURNAME);
		}
		input.addParm("lastname", lastname);
		//乘机人名
		String firstname = lCCPassengerInfo.getPersonNameArray(0).getGivenName();
		if(!StringUtils.hasLength(firstname)){
			 LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_GIVENNAME, 
						ApiServletHolder.getApiContext().getLanguage()));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_GIVENNAME);
		}
		input.addParm("firstname", firstname);
		//证件类别
		String idtype = lCCPassengerInfo.getFOIDArray(0).getType();
		if(!StringUtils.hasLength(idtype)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_IDTYPE, 
					ApiServletHolder.getApiContext().getLanguage()));
		 throw APIException.getInstance(
				 ErrCodeConstants.API_NULL_IDTYPE);
		}
		input.addParm("idtype", idtype);
		//证件号
		String idno = lCCPassengerInfo.getFOIDArray(0).getID().getStringValue();
		if(!StringUtils.hasLength(idno)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_IDNO, 
					ApiServletHolder.getApiContext().getLanguage()));
		 throw APIException.getInstance(ErrCodeConstants.API_NULL_IDNO);
		}
		input.addParm("idno", idno);
		if(idtype.equals("PP")){
			//有效日期
			String expiryDate = lCCPassengerInfo.getFOIDArray(0).getExpiryDate();
			input.addParm("expiryDate", expiryDate);
			//签发国
			String issueCountry = lCCPassengerInfo.getFOIDArray(0).getIssueCountry();
			input.addParm("issueCountry", issueCountry);
		}
		ContactInfo[] contactInfo= lCCPassengerInfo.getContactInfoArray();
		if(null != contactInfo && contactInfo.length>0){
			for (int i = 0; i < contactInfo.length; i++) {
				//联系类型
				String type = contactInfo[i].getType();
				if ("email".equals(type)) {
					//邮箱号
					String email = contactInfo[i].getNumber();
					if(StringUtils.hasLength(email)){
						input.addParm("email", email);
					}
				}else {
					//手机号
					String mobile = contactInfo[i].getNumber();
					if(StringUtils.hasLength(mobile)){
						input.addParm("mobile", mobile);
					}
					//区号
					String areaCode = contactInfo[i].getAreaCode();
					if(!StringUtils.isEmpty(areaCode)){
						input.addParm("area", areaCode);
					}
				}
				
			}
		}
		
		//出生日期
		Date birthday = lCCPassengerInfo.getAge().getBirthDate().getDateValue();
		if(birthday!=null){
			input.addParm("birthday", DateUtils.getInstance().format(birthday));
		}
		input.addParm(MEMBERID, memberid);
		//国籍
		String nationality = lCCPassengerInfo.getCitizenshipCountryCode();
		input.addParm("nationality", nationality);
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
