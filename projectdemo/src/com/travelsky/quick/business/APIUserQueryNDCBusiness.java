package com.travelsky.quick.business;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserReadRQDocument;
import org.iata.iata.edist.LCCUserReadRQDocument.LCCUserReadRQ.Query;
import org.iata.iata.edist.LCCUserReadRQDocument.LCCUserReadRQ.Query.LCCUserInfo;
import org.iata.iata.edist.LCCUserReadRSDocument;
import org.iata.iata.edist.LCCUserReadRSDocument.LCCUserReadRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.Address;
import org.iata.iata.edist.LCCUserType.Age;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.iata.iata.edist.LCCUserType.FOID;
import org.iata.iata.edist.LCCUserType.PersonName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
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
 * 类说明:
 * <ul>
 * <li>1.用户查询(获取会员信息接口)</li>
 * </ul>
 * 不同类型根据请求xml中ActionType区分,取值参考CommonConstants类
 * 
 * @see com.travelsky.quick.common.util.CommonConstants#API_QUERY_TYPE_USER
 * @author hu
 *
 */
@Service("LCC_USERQUERY_SERVICE")
public class APIUserQueryNDCBusiness extends AbstractService<ApiContext> {

	/**
	 *
	 */
	private static final long serialVersionUID = -2595881636351455263L;

	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserQueryNDCBusiness.class);

	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			// 转换 xml-->Reqbean.并进行处理
			transInputXmlToRequestBean();
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_USER_QUERY,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	/**
	 * 调用底层处理并获取结果
	 * 
	 * @param input
	 *            输入
	 * @param context
	 *            ServletContext
	 * @return CommandRet CommandRet
	 * @throws APIException
	 *             APIException
	 */
	private CommandRet getResult() throws APIException {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		/*
		 * CommandData input = context.getInput(); String actionType =
		 * input.getParm("actionType").getStringColumn().trim(); // 获取用户信息 if
		 * (CommonConstants.API_QUERY_TYPE_USER.equals(actionType)) {
		 */
		MemberManager memberManager = new MemberManager();
		CommandData input = context.getInput();
		String actionType=input.getParm("actionType").getStringColumn();
		if ("USER_QUERY_EMAIL".equals(actionType)) {
			return memberManager.getMemberEmail(input, context);
		}else{
			return memberManager.getMember(input, context);
		}
		/*
		 * }else { LOGGER.info(TipMessager.getInfoMessage(
		 * ErrCodeConstants.API_ENUM_ACTIONTYPE,
		 * ApiServletHolder.getApiContext().getLanguage())); throw
		 * APIException.getInstance(ErrCodeConstants.API_ENUM_ACTIONTYPE); }
		 */
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
		LCCUserReadRQDocument rootDoc = null;
		rootDoc = LCCUserReadRQDocument.Factory.parse(xmlInput);
		LCCUserReadRQDocument.LCCUserReadRQ reqDoc = rootDoc.getLCCUserReadRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid", deptno);
		// LCCUserReadRQ->Query
		Query query = reqDoc.getQuery();
		//actionType
		String actionType = query.getActionType();
		if (!StringUtils.hasLength(actionType)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ACTIONTYPE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACTIONTYPE);
		}
		input.addParm("actionType", actionType);
		LCCUserInfo lccUserInfo = query.getLCCUserInfo();
		if (lccUserInfo == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER);
		}
		String userID = lccUserInfo.getUserID();
		// Validate LCCUserInfo
		if (!StringUtils
				.hasLength(userID)/* || !StringUtils.hasLength(pinNumber) */) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_ID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		// 添加用户id
		input.addParm("memberid", userID);
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		LCCUserReadRSDocument doc = LCCUserReadRSDocument.Factory.newInstance();
		LCCUserReadRS root = doc.addNewLCCUserReadRS();

		try {
			if (processError(commandRet, root)) {
				return doc;
			}

			root.addNewSuccess();
			LCCUserType lccUserInfo = root.addNewResponse().addNewLCCUserInfo();
			// 查询用户
			transUser(lccUserInfo, commandRet);

		} catch (Exception e) {
			doc = LCCUserReadRSDocument.Factory.newInstance();
			root = doc.addNewLCCUserReadRS();
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
	 *            结果集
	 * @param root
	 *            节点
	 * @return 处理错误,如果包括错误
	 */
	private boolean processError(CommandRet ret, LCCUserReadRS root) {
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
	 * 转换查询用户信息结果到XmlBean
	 * 
	 * @param lccUserInfo
	 *            节点
	 * @param commandRet
	 *            结果集
	 * @param input
	 *            XmlBean
	 */
	private void transUser(LCCUserType lccUserInfo, CommandRet commandRet) {
		// 会员账号
		Item memberIdItem = commandRet.getParm("userid");
		String memberId = memberIdItem == null ? null : memberIdItem.getStringColumn();
		// if (StringUtils.hasLength(memberId)){
		lccUserInfo.setUserID(memberId);
		// }
		// 用户姓名
		getPersonName(commandRet, lccUserInfo);
		// 性别 M 男 F 女
		Item genderItem = commandRet.getParm("gender");
		String gender = genderItem == null ? null : genderItem.getStringColumn();
		// if (StringUtils.hasLength(gender)){
		lccUserInfo.setGender(gender);
		// }
		// 出生日期
		Item birthdayItem = commandRet.getParm("birthday");
		String birthday = birthdayItem == null ? null : birthdayItem.getStringColumn();
		if (StringUtils.hasLength(birthday)) {
			// DateUtils dateUtils = DateUtils.getInstance();
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date format = simpleDateFormat.parse(birthday);
				String format2 = simpleDateFormat.format(format);
				lccUserInfo.addNewAge().addNewBirthDate().setStringValue(format2);
			} catch (Exception e) {
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_BIRTHDAY,
						ApiServletHolder.getApiContext().getLanguage()), e);
				Age age = lccUserInfo.getAge();
				if (age != null) {
					age.dump();
				}
			}
		} else {
			lccUserInfo.addNewAge().addNewBirthDate();
		}
        //国籍
		String nationality = commandRet.getParm("nationality").getStringColumn();
		lccUserInfo.setCitizenshipCountryCode(nationality);
		// 证件信息
		getIdListItem(commandRet, lccUserInfo);
		// 地址信息
		getAddressListItem(commandRet, lccUserInfo);
		// 联系信息
		getContactListItem(commandRet, lccUserInfo);
		// 是否订阅促销邮件 Y 是 N否
		Item emailItem = commandRet.getParm("email");
		String emailName = emailItem == null ? null : emailItem.getStringColumn();
		// if (StringUtils.hasLength(promotionEmail)){
		lccUserInfo.setUserName(emailName);
		// }
		// 语言类型
		String languageCode = commandRet.getParm("languageCode").getStringColumn();
		if (StringUtils.hasLength(languageCode)) {
			lccUserInfo.addNewLCCPreference().addNewLanguageCode().setStringValue(languageCode);

		}
	}

	/**
	 * 转换查询用户姓名结果到XmlBean
	 * 
	 * @param commandRet
	 *            结果集
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getPersonName(CommandRet commandRet, LCCUserType lccUserInfo) {
		PersonName personName = lccUserInfo.addNewPersonName();
		// 姓
		Item lastNameItem = commandRet.getParm("lastname");
		String lastName = lastNameItem == null ? null : lastNameItem.getStringColumn();
		personName.setSurname(lastName);
		// 名
		Item firstNameItem = commandRet.getParm("firstname");
		String firstName = firstNameItem == null ? null : firstNameItem.getStringColumn();
		// if (StringUtils.hasLength(firstName)){
		personName.setGivenName(firstName);
		// }
		// 拼音姓
		Item spellSurNameItem = commandRet.getParm("pinyinxing");
		String spellSurName = spellSurNameItem == null ? null : spellSurNameItem.getStringColumn();
		// if (StringUtils.hasLength(spellSurName)){
		personName.setSpellSurname(spellSurName);
		// }
		// 拼音名
		Item spellGivenNameItem = commandRet.getParm("pinyinming");
		String spellGivenName = spellGivenNameItem == null ? null : spellGivenNameItem.getStringColumn();
		// if (StringUtils.hasLength(spellGivenName)){
		personName.setSpellGivenName(spellGivenName);
		// }
	}

	/**
	 * 转换查询用户证件信息结果到XmlBean
	 * 
	 * @param commandRet
	 *            结果集
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getIdListItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item idListItem = commandRet.getParm("idlist");
		Table idListTable = idListItem == null ? null : idListItem.getTableColumn();
		int count = idListTable == null ? 0 : idListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				FOID foid = lccUserInfo.addNewFOID();
				Row idListRow = idListTable.getRow(i);
				// ID标识
				Item idItem = idListRow.getColumn("id");
				String id = idItem == null ? null : idItem.getStringColumn();
				if (StringUtils.hasLength(id)) {
					foid.setFOIDKey(id);
				}
				// 证件类别
				Item idTypeItem = idListRow.getColumn("idtype");
				String idType = idTypeItem == null ? null : idTypeItem.getStringColumn();
				if (StringUtils.hasLength(idType)) {
					foid.setType(idType);
				}
				// 证件号
				Item idNoItem = idListRow.getColumn("idno");
				String idNo = idNoItem == null ? null : idNoItem.getStringColumn();
				// if (StringUtils.hasLength(idNo)){
				foid.addNewID().setStringValue(idNo);
				// }
				if (idType.equals("PP")) {
					String expiryDete = idListRow.getColumn("expiry").getStringColumn();
					foid.setExpiryDate(expiryDete);
					String issuecountry = idListRow.getColumn("issuecountry").getStringColumn();
					foid.setIssueCountry(issuecountry);
				}
				String status = idListRow.getColumn("status").getStringColumn();
				foid.getID().setVendorCode(status);

			}
		} else {
			FOID foid = lccUserInfo.addNewFOID();
			foid.setFOIDKey("");
			foid.setType("");
			foid.addNewID();
			foid.getID().setVendorCode("");
		}

	}

	/**
	 * 转换查询用户地址信息结果到XmlBean
	 * 
	 * @param commandRet
	 *            结果集
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getAddressListItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item addressListItem = commandRet.getParm("addresslist");
		Table addressListTable = addressListItem == null ? null : addressListItem.getTableColumn();
		int count = addressListTable == null ? 0 : addressListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				Address addressEle = lccUserInfo.addNewAddress();
				Row addrRow = addressListTable.getRow(i);
				// Id标识
				Item idItem = addrRow.getColumn("id");
				String id = idItem == null ? null : idItem.getStringColumn();
				if (StringUtils.hasLength(id)) {
					addressEle.setAddressKey(id);
				}
				// 省
				Item provinceItem = addrRow.getColumn("province");
				String province = provinceItem == null ? null : provinceItem.getStringColumn();
				if (StringUtils.hasLength(province)) {
					addressEle.setCountryName(province);
				}
				// 市
				Item cityItem = addrRow.getColumn("city");
				String city = cityItem == null ? null : cityItem.getStringColumn();
				if (StringUtils.hasLength(city)) {
					addressEle.setCityName(city);
				}
				// 邮政编码
				Item zipCodeItem = addrRow.getColumn("zipcode");
				String zipCode = zipCodeItem == null ? null : zipCodeItem.getStringColumn();
				// if (StringUtils.hasLength(zipCode)){
				addressEle.setPostalCode(zipCode);
				// }
				// 地址
				Item addressItem = addrRow.getColumn("address");
				String address = addressItem == null ? null : addressItem.getStringColumn();
				if (StringUtils.hasLength(address)) {
					addressEle.setStreetNmbr(address);
				}
			}
		} else {
			Address addressEle = lccUserInfo.addNewAddress();
			addressEle.setAddressKey(null);
			addressEle.setCountryName(null);
			addressEle.setCityName(null);
			addressEle.setPostalCode(null);
			addressEle.setStreetNmbr(null);
		}

	}

	/**
	 * 转换查询用户联系信息结果到XmlBean
	 * 
	 * @param commandRet
	 *            结果集
	 * @param lccUserInfo
	 *            节点
	 */
	private static void getContactListItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item contactListItem = commandRet.getParm("contactlist");
		Table contactListTable = contactListItem == null ? null : contactListItem.getTableColumn();
		int count = contactListTable == null ? 0 : contactListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				ContactInfo contactInfo = lccUserInfo.addNewContactInfo();
				Row contactRow = contactListTable.getRow(i);
				// ID标识
				Item idItem = contactRow.getColumn("id");
				String id = idItem == null ? null : idItem.getStringColumn();
				if (StringUtils.hasLength(id)) {
					contactInfo.setID(id);
				}
				// 类别 tel电话 mobile手机 email电邮 qq QQ webchat 微信
				Item typeItem = contactRow.getColumn("type");
				String type = typeItem == null ? null : typeItem.getStringColumn();
				if (StringUtils.hasLength(type)) {
					contactInfo.setType(type);
				}
				// 号码
				Item noItem = contactRow.getColumn("no");
				String no = noItem == null ? null : noItem.getStringColumn();
				if (StringUtils.hasLength(no)) {
					contactInfo.setNumber(no);
				}
				String status = contactRow.getColumn("status").getStringColumn();
				contactInfo.setStatus(status);
				if ("mobile".equals(type)) {
					contactInfo.setAreaCode(contactRow.getColumn("area").getStringColumn());
				}
			}
		} else {
			ContactInfo contactInfo = lccUserInfo.addNewContactInfo();
			contactInfo.setID(null);
			contactInfo.setType(null);
			contactInfo.setNumber(null);
			contactInfo.setStatus(null);
		}

	}
}
