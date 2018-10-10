package com.travelsky.quick.business;


import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserModifyRQDocument;
import org.iata.iata.edist.LCCUserModifyRQDocument.LCCUserModifyRQ;
import org.iata.iata.edist.LCCUserModifyRSDocument;
import org.iata.iata.edist.LCCUserModifyRSDocument.LCCUserModifyRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;

import org.springframework.util.StringUtils;

import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:修改密码接口
 * @author huxizhun
 *
 */
@Service("LCC_USERCHANGEPWD_SERVICE")
public class APIUserChangePwdNDCBusiness extends AbstractService<ApiContext> {
	/**
	 *
	 */
	private static final long serialVersionUID = 5060807052531020562L;

	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserChangePwdNDCBusiness.class);

	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();


		try {
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
					ErrCodeConstants.API_UNKNOW_USER_REGISTER,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	private CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MemberManager memberManager=new MemberManager();
		
		String actionType=input.getParm("actionType").getStringColumn();
		if ("PWD_UPDATE_EMAIl".equals(actionType)) {
			return memberManager.changePwdEmail(input, context);
		}else{
			return memberManager.changePwd(input, context);
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
		// 部门ID
//		String deptno = NdcXmlHelper.getDeptNo(reqDoc.getParty());
		input.addParm("tktdeptid",ApiServletHolder.getApiContext().getTicketDeptid());
		//渠道验证
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		LCCUserType lccUserInfo = reqDoc.getModify().getLCCUserInfo();
		if (lccUserInfo == null) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_USER, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER);
		}
		String actionType = reqDoc.getModify().getActionType();
		input.addParm("actionType", actionType);
		if ("PWD_UPDATE_EMAIl".equals(actionType)) {
			transInputXmlToRequestBeanEmail(input, lccUserInfo, language, context);
			return;
		}
		// 会员id
		String userId = context.getContext().getUserID();
		if (!StringUtils.hasLength(userId)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_USER_ID, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		input.addParm("memberid", userId);

		// 原密码
		String oldPwd = new String(Base64.decodeBase64(lccUserInfo.getOldPassword().getBytes()));
		if (!StringUtils.hasLength(oldPwd)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_PWD_OLD, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PWD_OLD);
		}
		input.addParm("oldpassword", oldPwd);

		// 新密码
		String newPwd = new String(Base64.decodeBase64(lccUserInfo.getNewPassword().getBytes()));
		if (!StringUtils.hasLength(newPwd)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_PWD_NEW, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PWD_NEW);
		}
		input.addParm("newpassword", newPwd);
		// 验证码
		String vcCode = lccUserInfo.getVerificationCode();
		if (!StringUtils.hasLength(vcCode)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_VALIDATE_CODE, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_VALIDATE_CODE);
		}
		input.addParm("smscode", vcCode);
		//手机号
		ContactInfo[] contactArr = lccUserInfo.getContactInfoArray();
		if (contactArr != null && contactArr.length > 0) {
			for (ContactInfo contactInfo : contactArr) {
				String type = contactInfo.getType();
				String number = contactInfo.getNumber();
				//区号
				String areaCode = contactInfo.getAreaCode();
				// Validate Type
				if (!StringUtils.hasLength(type)) {
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_CONTACT_TYPE,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_TYPE);
				}

				// 记录手机号
				if (CommonConstants.API_TYPE_MOBILE.equals(type)
						&& StringUtils.hasLength(number) && StringUtils.hasLength(areaCode)) {
					// Validate number
					input.addParm("mobile", number);
					input.addParm("areaCode", areaCode);
				}
			}
		}
	}

	private void transInputXmlToRequestBeanEmail(CommandData input, LCCUserType lccUserInfo, String language,
			SelvetContext<ApiContext> context) throws APIException {
		// 会员id
				String userId = context.getContext().getUserID();
				if (!StringUtils.hasLength(userId)) {
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_USER_ID, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
				}
				input.addParm("memberid", userId);

				// 原密码
				String oldPwd = new String(Base64.decodeBase64(lccUserInfo.getOldPassword().getBytes()));
				if (!StringUtils.hasLength(oldPwd)) {
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_PWD_OLD, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_PWD_OLD);
				}
				input.addParm("oldpassword", oldPwd);

				// 新密码
				String newPwd = new String(Base64.decodeBase64(lccUserInfo.getNewPassword().getBytes()));
				if (!StringUtils.hasLength(newPwd)) {
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_PWD_NEW, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_PWD_NEW);
				}
				input.addParm("newpassword", newPwd);
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		// TODO Auto-generated method stub
		LCCUserModifyRSDocument doc = LCCUserModifyRSDocument.Factory.newInstance();
		LCCUserModifyRSDocument.LCCUserModifyRS root = doc.addNewLCCUserModifyRS();
		try {
			if (!processError(commandRet, root)) {
				root.addNewSuccess();
			}
		}
		catch (Exception e) {
			doc = LCCUserModifyRSDocument.Factory.newInstance();
			root = doc.addNewLCCUserModifyRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM,
				TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}

		return doc;
	}

	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * @param ret
	 * @param root
	 * @return
	 */
	private boolean processError(CommandRet ret, LCCUserModifyRS root) {
		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}

}
