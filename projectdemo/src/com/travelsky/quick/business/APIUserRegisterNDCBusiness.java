package com.travelsky.quick.business;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserCreateRQDocument;
import org.iata.iata.edist.LCCUserCreateRQDocument.LCCUserCreateRQ;
import org.iata.iata.edist.LCCUserCreateRSDocument;
import org.iata.iata.edist.LCCUserCreateRSDocument.LCCUserCreateRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.Age;
import org.iata.iata.edist.LCCUserType.Age.BirthDate;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.iata.iata.edist.LCCUserType.FOID;
import org.iata.iata.edist.LCCUserType.FOID.ID;
import org.iata.iata.edist.LCCUserType.LCCPreference;
import org.iata.iata.edist.LCCUserType.PersonName;
import org.iata.iata.edist.LanguageCodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:用户注册(会员注册接口)
 * 
 * @author huxizhun
 *
 */
@Service("LCC_USERREGISTER_SERVICE")
public class APIUserRegisterNDCBusiness extends AbstractService<ApiContext> {
	private static final long serialVersionUID = -5443561065140468028L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserRegisterNDCBusiness.class);

	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();

		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
			// ......
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_USER_REGISTER,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	private CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MemberManager memberManager = new MemberManager();
		String actionType = input.getParm("actionType").getStringColumn();
		if ("USER_REGISTER_EAMIL".equals(actionType)) {
			return memberManager.registerEmail(input, context);
		} else {
			return memberManager.register(input, context);
		}
	}

	/**
	 * @throws APIException
	 *             APIException
	 * @throws Exception
	 *             Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();

		CommandData input = context.getInput();
		LCCUserCreateRQDocument rootDoc = null;
		rootDoc = LCCUserCreateRQDocument.Factory.parse(xmlInput);
		LCCUserCreateRQDocument.LCCUserCreateRQ reqDoc = rootDoc.getLCCUserCreateRQ();

		// 部门ID
		// String deptno = NdcXmlHelper.getDeptNo(reqDoc.getParty());
		input.addParm("tktdeptid", ApiServletHolder.getApiContext().getTicketDeptid());
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		String actionType = reqDoc.getCreate().getActionType();
		if (!StringUtils.hasLength(actionType)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ACTIONTYPE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACTIONTYPE);
		}
		input.addParm("actionType", actionType);
		if ("USER_REGISTER_EAMIL".equals(actionType)) {
			transInputXmlToRequestBeanEmail(input, reqDoc, language, context);
			return;
		}

		// 用户id
		LCCUserType lccUserInfo = reqDoc.getCreate().getLCCUserInfo();
		if (lccUserInfo == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER);
		}
		// 短信验证码
		String smsCode = lccUserInfo.getVerificationCode();
		if (!StringUtils.hasLength(smsCode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_VALIDATE_CODE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_VALIDATE_CODE);
		}
		input.addParm("smscode", smsCode);

		// Validate UserID
		String userId = lccUserInfo.getUserID();
		if (!StringUtils.hasLength(userId)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_ID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		input.addParm("username", userId);

		// 密码
		String password =  new String(Base64.decodeBase64(lccUserInfo.getNewPassword().getBytes()));
		// Validate NewPassword
		if (!StringUtils.hasLength(password)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PWD_NEW,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PWD_NEW);
		}
		input.addParm("password", password);
		// 生日
		Date birthDate = lccUserInfo.getAge().getBirthDate().getDateValue();
		if (birthDate != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			input.addParm("birthday", sdf.format(birthDate));
		}
		// 姓名
		getPersonName(input, lccUserInfo);
		// 证件类别、证件ID
		getIdListItem(input, lccUserInfo);
		// 联系方式
		getContactListItem(input, lccUserInfo);
		// 性别
		String gender = lccUserInfo.getGender();
		// Validate gender
		if (!StringUtils.hasLength(gender)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_GENDER,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_GENDER);
		}
		input.addParm("gender", gender);
	}

	private void transInputXmlToRequestBeanEmail(CommandData input, LCCUserCreateRQ reqDoc, String language,
			SelvetContext<ApiContext> context) throws APIException, Exception {
		// 用户id
		LCCUserType lccUserInfo = reqDoc.getCreate().getLCCUserInfo();
		if (lccUserInfo == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER);
		}
		// Validate UserID
		String userId = lccUserInfo.getUserID();
		if (!StringUtils.hasLength(userId)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_ID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		input.addParm("userId", userId);

		// 密码
		String password = new String(Base64.decodeBase64(lccUserInfo.getNewPassword().getBytes()));
		// Validate NewPassword
		if (!StringUtils.hasLength(password)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PWD_NEW,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PWD_NEW);
		}
		input.addParm("password", password);
		// 生日
		Date birthDate = null;
		if (lccUserInfo.getAge() != null && lccUserInfo.getAge().getBirthDate() != null
				&& lccUserInfo.getAge().getBirthDate() != null) {
			birthDate = lccUserInfo.getAge().getBirthDate().getDateValue();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			input.addParm("birthday", sdf.format(birthDate));
		}
		// 语言Code
		LCCPreference[] lccPreferenceArray = lccUserInfo.getLCCPreferenceArray();
		if (lccPreferenceArray == null || lccPreferenceArray.length < 1 || lccPreferenceArray[0] == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_LANGUAGE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_LANGUAGE);
		}
		String languageCode = lccPreferenceArray[0].getLanguageCode().getStringValue();
		if (!StringUtils.hasLength(languageCode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_LANGUAGE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_LANGUAGE);
		}
		input.addParm("languageCode", languageCode);
		// 姓
		PersonName[] personNameArr = lccUserInfo.getPersonNameArray();
		// Validate PersonName
		if (personNameArr == null || personNameArr.length < 1 || personNameArr[0] == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_NAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_NAME);
		}
		PersonName personName = personNameArr[0];
		String surName = personName.getSurname();
		// Validate Surname
		if (!StringUtils.hasLength(surName)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_SURNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_SURNAME);
		}
		input.addParm("lastname", surName);

		// 名
		String givenName = personName.getGivenName();
		// Validate GivenName
		if (!StringUtils.hasLength(givenName)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_GIVENNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_GIVENNAME);
		}
		input.addParm("firstname", givenName);
		// 联系方式
		ContactInfo[] contactArr = lccUserInfo.getContactInfoArray();
		if (contactArr != null && contactArr.length > 0 && StringUtils.hasLength(contactArr[0].getType())
				&& StringUtils.hasLength(contactArr[0].getNumber())) {
			getContactListItemEmail(input, lccUserInfo);
		}
		// 性别
		String gender = lccUserInfo.getGender();
		// Validate gender
		if (StringUtils.hasLength(gender)) {
			input.addParm("gender", gender);
		}
		// 证件类别、证件ID
		FOID[] foidArr = lccUserInfo.getFOIDArray();

		if (foidArr != null && foidArr.length > 0 && StringUtils.hasLength(foidArr[0].getType())) {
			getIdListItemEmail(input, lccUserInfo);
		}
		String nationality = lccUserInfo.getCitizenshipCountryCode();
		input.addParm("nationality", nationality);
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		LCCUserCreateRSDocument doc = LCCUserCreateRSDocument.Factory.newInstance();
		LCCUserCreateRSDocument.LCCUserCreateRS root = doc.addNewLCCUserCreateRS();

		try {
			if (processError(commandRet, root)) {
				return doc;
			}

			root.addNewSuccess();
			String userId = commandRet.getParm("memberid").getStringColumn();
			root.addNewResponse().addNewLCCUserInfo().setUserID(userId);
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()), e);
			doc = LCCUserCreateRSDocument.Factory.newInstance();
			root = doc.addNewLCCUserCreateRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM, TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}

		return doc;
	}

	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * 
	 * @param ret
	 * @param root
	 * @return
	 */
	private boolean processError(CommandRet ret, LCCUserCreateRS root) {

		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode, ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}

	/**
	 * 转换查询用户姓名结果到XmlBean
	 * 
	 * @param input
	 *            XML
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getPersonName(CommandData input, LCCUserType lccUserInfo) throws APIException {
		// 姓
		PersonName[] personNameArr = lccUserInfo.getPersonNameArray();
		// Validate PersonName
		if (personNameArr == null || personNameArr.length < 1 || personNameArr[0] == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_NAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_NAME);
		}
		PersonName personName = personNameArr[0];
		String surName = personName.getSurname();
		// Validate Surname
		if (!StringUtils.hasLength(surName)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_SURNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_SURNAME);
		}
		input.addParm("lastname", surName);

		// 名
		String givenName = personName.getGivenName();
		// Validate GivenName
		if (!StringUtils.hasLength(givenName)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_GIVENNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_GIVENNAME);
		}
		input.addParm("firstname", givenName);

		// 拼音姓
		String spellSurName = personName.getSpellSurname();
		if (StringUtils.hasLength(spellSurName)) {
			input.addParm("pinyinxing", spellSurName);
		}

		// 拼音名
		String spellGivenName = personName.getSpellGivenName();
		if (StringUtils.hasLength(spellGivenName)) {
			input.addParm("pinyinming", spellGivenName);
		}
	}

	/**
	 * 转换查询用户证件信息结果到XmlBean
	 * 
	 * @param input
	 *            XML
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getIdListItem(CommandData input, LCCUserType lccUserInfo) throws APIException {
		FOID[] foidArr = lccUserInfo.getFOIDArray();
		// Validate FOID
		if (foidArr == null || foidArr.length < 1 || foidArr[0] == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDINFO,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_IDINFO);
		}
		Table table = null;
		for (int i = 0; i < foidArr.length; i++) {
			if (foidArr[i].getType().equals("PP")) {
				table = new Table(new String[] { "idtype", "idno", "expiryDate", "issueCountry" });
			} else {
				table = new Table(new String[] { "idtype", "idno" });
			}
			for (int j = 0; j < foidArr.length; j++) {
				Row row = table.addRow();
				// 证件类型
				String foidType = foidArr[j].getType();
				if (!StringUtils.hasLength(foidType)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDTYPE,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_IDTYPE);
				}
				row.addColumn("idtype", foidType);
				// 证件号
				ID id = foidArr[j].getID();
				String idVal = id == null ? null : id.getStringValue();
				// Validate FOID ID
				if (!StringUtils.hasLength(idVal)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDNO,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_IDNO);
				}
				row.addColumn("idno", idVal.toUpperCase());
				// 判断证件类型为护照/PP添加参数签发国和有效日期，否则不添加
				if (foidType.equals("PP")) {
					String expiryDate = foidArr[j].getExpiryDate();
					String issueCountry = foidArr[j].getIssueCountry();
					row.addColumn("expiry", expiryDate);
					row.addColumn("issuecountry", issueCountry);
				}
			}
		}
		input.addParm("idlist", table);
	}

	/**
	 * Email转换查询用户证件信息结果到XmlBean
	 * 
	 * @param input
	 *            XML
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getIdListItemEmail(CommandData input, LCCUserType lccUserInfo) throws APIException {
		FOID[] foidArr = lccUserInfo.getFOIDArray();
		// Validate FOID
		if (foidArr == null || foidArr.length < 1 || foidArr[0] == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDINFO,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_IDINFO);
		}
		Table table = new Table(
				new String[] { "type", "no", "expiry", "issuecountry", "firstname", "lastname", "gender", "birthday" });
		for (int i = 0; i < foidArr.length; i++) {
			Row row = table.addRow();
			// 证件类型
			String foidType = foidArr[i].getType();
			if (!StringUtils.hasLength(foidType)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDTYPE,
						ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_IDTYPE);
			}
			row.addColumn("type", foidType);
			// 证件号
			ID id = foidArr[i].getID();
			String idVal = id == null ? null : id.getStringValue();
			// Validate FOID ID
			if (!StringUtils.hasLength(idVal)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDNO,
						ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_IDNO);
			}
			row.addColumn("no", idVal.toUpperCase());
			// 添加证件的用户信息
			row.addColumn("firstname", input.getParm("firstname").getStringColumn());
			row.addColumn("lastname", input.getParm("lastname").getStringColumn());
			row.addColumn("gender", input.getParm("gender").getStringColumn());
			row.addColumn("birthday", input.getParm("birthday").getStringColumn());

			// 判断证件类型为护照/PP添加参数签发国和有效日期，否则不添加
			if (foidType.equals("PP")) {
				String expiryDate = foidArr[i].getExpiryDate();
				String issueCountry = foidArr[i].getIssueCountry();
				row.addColumn("expiry", expiryDate);
				row.addColumn("issuecountry", issueCountry);
			}
		}
		input.addParm("idlist", table);
	}

	/**
	 * Email转换查询用户联系信息结果到XmlBean
	 * 
	 * @param input
	 *            XMl
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getContactListItemEmail(CommandData input, LCCUserType lccUserInfo) throws APIException {
		ContactInfo[] contactArr = lccUserInfo.getContactInfoArray();
		Table contactTable = new Table(new String[] { "no", "type", "regionCode" });
		if (contactArr != null && contactArr.length > 0) {
			for (ContactInfo contactInfo : contactArr) {
				String type = contactInfo.getType();
				String number = contactInfo.getNumber();
				// 国际区号
				String areaCode = contactInfo.getAreaCode();
				// Validate Type
				if (!StringUtils.hasLength(type)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT_TYPE,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_TYPE);
				}

				// 记录手机号
				if (CommonConstants.API_TYPE_MOBILE.equals(type) && StringUtils.hasLength(number)
						&& StringUtils.hasLength(areaCode)) {
					// Validate number
					Row contactRow = contactTable.addRow();
					contactRow.addColumn("no", number);
					contactRow.addColumn("type", CommonConstants.API_TYPE_MOBILE);
					contactRow.addColumn("regionCode", areaCode);
				}
				// 记录邮箱
				else if (CommonConstants.API_TYPE_EMAIL.equals(type) && StringUtils.hasLength(number)) {
					Row contactRow = contactTable.addRow();
					contactRow.addColumn("no", number);
					contactRow.addColumn("type", CommonConstants.API_TYPE_EMAIL);
				}
			}
		}
		input.addParm("contact", contactTable);
	}

	/**
	 * 转换查询用户联系信息结果到XmlBean
	 * 
	 * @param input
	 *            XMl
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getContactListItem(CommandData input, LCCUserType lccUserInfo) throws APIException {
		ContactInfo[] contactArr = lccUserInfo.getContactInfoArray();
		if (contactArr != null && contactArr.length > 0) {
			for (ContactInfo contactInfo : contactArr) {
				String type = contactInfo.getType();
				String number = contactInfo.getNumber();
				// 国际区号
				String areaCode = contactInfo.getAreaCode();
				// Validate Type
				if (!StringUtils.hasLength(type)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT_TYPE,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_TYPE);
				}

				// 记录手机号
				if (CommonConstants.API_TYPE_MOBILE.equals(type) && StringUtils.hasLength(number)
						&& StringUtils.hasLength(areaCode)) {
					// Validate number
					input.addParm("mobile", number);
					input.addParm("areaCode", areaCode);
				}
				// 记录邮箱
				else if (CommonConstants.API_TYPE_EMAIL.equals(type) && StringUtils.hasLength(number)) {
					input.addParm("email", number);
				}
			}
		}
	}
}
