package com.travelsky.quick.business;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserModifyRQDocument;
import org.iata.iata.edist.LCCUserModifyRQDocument.LCCUserModifyRQ.Modify;
import org.iata.iata.edist.LCCUserModifyRSDocument;
import org.iata.iata.edist.LCCUserModifyRSDocument.LCCUserModifyRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserCreateRQDocument.LCCUserCreateRQ.Create;
import org.iata.iata.edist.LCCUserType.Address;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.iata.iata.edist.LCCUserType.FOID;
import org.iata.iata.edist.LCCUserType.PersonName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 
 * @author MaRuifu 2016年5月3日下午3:12:11
 * @version 0.1 类说明:信息修改 userInfoUpdate用户信息修改
 */
@Service("LCC_USERMODIFY_SERVICE")
public class APIUserModifyBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserModifyBusiness.class);

	private static final String MEMBERID = "memberid";

	/**
	 * 
	 * @param context
	 *            SelvetContext<ApiContext>
	 * @throws Exception
	 *             Exception
	 */
	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 获取xml
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_USER_MODIFY,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	/**
	 * 
	 * @param input
	 *            CommandData
	 * @param context
	 *            SelvetContext
	 * @return CommandRet 返回类型
	 */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MemberManager memberManager = new MemberManager();
		return memberManager.updateMember(input, context);
	}

	/**
	 * 
	 * @param context
	 *            SelvetContext
	 * @param xmlInput
	 *            String
	 * @throws APIException
	 *             APIException
	 * @throws Exception
	 *             Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCUserModifyRQDocument rootDoc = null;
		rootDoc = LCCUserModifyRQDocument.Factory.parse(xmlInput);
		LCCUserModifyRQDocument.LCCUserModifyRQ reqDoc = rootDoc.getLCCUserModifyRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid", deptno);
		// 修改信息
		Modify modifyinfo = reqDoc.getModify();
		// 修改信息类型
		String actionType = modifyinfo.getActionType();
		if (!StringUtils.hasLength(actionType)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ACTIONTYPE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACTIONTYPE);
		}

		input.addParm("actionType", actionType);
		// 会员信息
		LCCUserType lCCUserInfo = modifyinfo.getLCCUserInfo();
		if ("MODIFY_USER_EMAIL".equals(actionType)) {
			// 语言类型
			String languageCode = lCCUserInfo.getLCCPreferenceArray(0).getLanguageCode().getStringValue();
			if (!StringUtils.hasLength(languageCode)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_LANGUAGE,
						ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_LANGUAGE);
			}
			input.addParm("languageCode", languageCode);
			//国籍
			String nationality = lCCUserInfo.getCitizenshipCountryCode();
			input.addParm("nationality", nationality);
		}

		// 会员账号
		String memberid = context.getContext().getUserID();
		if (!StringUtils.hasLength(memberid)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_ID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		// 原密码
		String oldPassword = lCCUserInfo.getOldPassword();
		if (StringUtils.hasLength(oldPassword)) {
			input.addParm("oldPassword", oldPassword);
		}
		// 会员信息
		getUserInfo(input, lCCUserInfo, memberid);
		// 证件信息
		getUserIdentity(input, lCCUserInfo);
		// 地址信息
		getUserAddress(input, lCCUserInfo);
		// 联系信息
		getUserContact(input, lCCUserInfo);
	}

	/**
	 * 会员信息增加联系方式xml-->Reqbean
	 * 
	 * @param input
	 *            请求的XML
	 * @param create
	 *            节点
	 * @throws APIException
	 *             APIException
	 */
	public void getUserContact(CommandData input, LCCUserType LCCUserInfo) throws APIException {
		// 类型
		String type = "";
		// 号码
		String number = "";
		// 区号
		String area = "";
		ContactInfo[] contactInfo = LCCUserInfo.getContactInfoArray();
		Table table = new Table(new String[] { "area", "type", "no" });
		if (null != contactInfo && contactInfo.length > 0) {
			for (int i = 0; i < contactInfo.length; i++) {
				Row row = table.addRow();
				type = contactInfo[i].getType();
				number = contactInfo[i].getNumber();
				area = contactInfo[i].getAreaCode();
				if (!StringUtils.hasLength(type)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT_TYPE,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_TYPE);
				}
				if (!StringUtils.hasLength(number)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT_NO,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_NO);
				}
				row.addColumn("type", type);
				row.addColumn("no", number);
				row.addColumn("area", area);
			}
		} else {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT);
		}
		input.addParm("contactlist", table);
	}

	/**
	 * 获取地址信息
	 * 
	 * @param input
	 * @param create
	 * @throws APIException
	 */
	public void getUserAddress(CommandData input, LCCUserType LCCUserInfo) throws APIException {
		// 省
		String province = "";
		// 市
		String cityName = "";
		// 邮编
		String zipcode = "";
		// 地址
		String streetNmbr = "";
		Address[] address = LCCUserInfo.getAddressArray();
		if (null != address && address.length > 0) {
			province = address[0].getCountryName();
			cityName = address[0].getCityName();
			zipcode = address[0].getPostalCode();
			streetNmbr = address[0].getStreetNmbr();
		} else {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ADDRESS,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ADDRESS);
		}
		input.addParm("addresstitle", "地址");
		input.addParm("province", province);
		input.addParm("city", cityName);
		input.addParm("zipcode", zipcode);
		input.addParm("address", streetNmbr);
	}

	/**
	 * 获取证件信息
	 * 
	 * @param input
	 * @param lCCUserInfo
	 * @throws APIException
	 * @throws ParseException 
	 */
	public void getUserIdentity(CommandData input, LCCUserType lCCUserInfo) throws APIException, ParseException {
		// 证件类别
		String idtype = "";
		// 证件号
		String idno = "";
		// 签发国
		String issueCountry = "";
		// 有效期
		String  expiryDate ="";
		FOID[] info = lCCUserInfo.getFOIDArray();
		Table table = new Table(new String[] { "idtype", "idno", "expiry", "issuecountry" });
		if (null != info && info.length > 0) {
			for (int i = 0; i < info.length; i++) {
				Row row = table.addRow();
				idtype = info[i].getType();
				idno = info[i].getID().getStringValue();
				if (!StringUtils.hasLength(idtype)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDTYPE,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_IDTYPE);
				}
				if (!StringUtils.hasLength(idno)) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDNO,
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_IDNO);
				}
				if (idtype.equals("PP")) {
					if (StringUtils.hasLength(info[0].getExpiryDate())) {
						expiryDate = info[i].getExpiryDate().substring(0, 10);
					} else {
						expiryDate = info[i].getExpiryDate();
					}
					issueCountry = info[i].getIssueCountry();
				}
				row.addColumn("idtype", idtype);
				row.addColumn("idno", idno);
				row.addColumn("expiry", expiryDate);
				row.addColumn("issuecountry", issueCountry);
			}
		}
		// 证件信息为空异常
		else {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDINFO,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_IDINFO);
		}
		input.addParm("idlist", table);
	}

	/**
	 * 
	 * @param input
	 *            CommandData
	 * @param lCCUserInfo
	 *            LCCUserType
	 * @param memberid
	 *            String
	 */
	private void getUserInfo(CommandData input, LCCUserType lCCUserInfo, String memberid) throws APIException {
		/**
		 * 会员信息修改
		 */
		// 是否订阅促销邮件 Y 是 N否-
		String promotionEmail = lCCUserInfo.getPromotionEmail();
		if (StringUtils.hasLength(promotionEmail)) {
			input.addParm("promotionemail", promotionEmail);
		}
		// 姓名信息
		PersonName personName = lCCUserInfo.getPersonNameArray(0);
		if (personName == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PERSONNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PERSONNAME);
		}
		// 姓
		String surname = personName.getSurname();
		if (!StringUtils.hasLength(surname)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_SURNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_SURNAME);
		}
		// 名
		String sivenName = personName.getGivenName();
		if (!StringUtils.hasLength(sivenName)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_GIVENNAME,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_GIVENNAME);
		}
		// 出生日期
		Date birthDate = lCCUserInfo.getAge() == null ? null
				: lCCUserInfo.getAge().getBirthDate() == null ? null
						: lCCUserInfo.getAge().getBirthDate().getDateValue();
		if (birthDate != null) {
			input.addParm("birthday", DateUtils.getInstance().format(birthDate));
		}
		// 性别
		String gender = lCCUserInfo.getGender();
		if (!StringUtils.hasLength(gender)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_GENDER,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_GENDER);
		}
		input.addParm(MEMBERID, memberid);
		input.addParm("lastname", surname);
		input.addParm("firstname", sivenName);
		input.addParm("gender", gender);
	}

	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @param xmlOutput
	 *            CommandRet
	 * @param input
	 *            CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet xmlOutput, CommandData input) {
		LCCUserModifyRSDocument sadoc = LCCUserModifyRSDocument.Factory.newInstance();
		LCCUserModifyRS rprs = sadoc.addNewLCCUserModifyRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
			} else {
				// 会员账号
				String memberid = xmlOutput.getParm(MEMBERID).getStringColumn();
				rprs.addNewResponse().addNewLCCUserInfo().setUserID(memberid);
				rprs.addNewSuccess();
			}
		} catch (Exception e) {
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
