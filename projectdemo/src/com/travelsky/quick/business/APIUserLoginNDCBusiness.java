package com.travelsky.quick.business;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.ErrorsType;
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
import org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate;
import org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo;
import org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo.LCCOrderTemplateInfo;
import org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo.LCCOrderTemplateInfo.FOID.ID;
import org.iata.iata.edist.LCCUserType.PersonName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
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
 * 会员登陆接口
 * 
 * @author huxizhun
 *
 */
@Service("LCC_USERLOGIN_SERVICE")
public class APIUserLoginNDCBusiness extends AbstractService<ApiContext> {
	/**
	 *
	 */
	private static final long serialVersionUID = 8691825821149118846L;
	/**
	 *
	 */

	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserLoginNDCBusiness.class);

	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_USER_LOGIN,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	private CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MemberManager memberManager = new MemberManager();
		return memberManager.login(input, context);
	}

	/**
	 *
	 * @param context
	 *            后台返回的数据对象
	 * @param xmlInput
	 *            请求的XML
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
		LCCUserInfo lccUserInfo = query.getLCCUserInfo();
		if (lccUserInfo == null) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_LOGIN,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_LOGIN);
		}
		// email登陆区别type
		String actionType = query.getActionType();
		if (!StringUtils.hasLength(actionType)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ACTIONTYPE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACTIONTYPE);
		}
		input.addParm("actionType", actionType);
		String userID = lccUserInfo.getUserID();
		String pinNumber = new String(Base64.decodeBase64(lccUserInfo.getPinNumber().getBytes()));
		// Validate LCCUserInfo
		if (!StringUtils.hasLength(userID) || !StringUtils.hasLength(pinNumber)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ACCOUNT_PASSWORD,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACCOUNT_PASSWORD);
		}

		// 用户名
		input.addParm("username", userID);
		// 密码
		input.addParm("password", pinNumber);
		// 记录用户id与渠道
	}

	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * 
	 * @param ret
	 *            请求
	 * @param root
	 *            节点
	 * @return 是否返回
	 */
	private boolean processError(CommandRet ret, LCCUserReadRS root) {
		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorsType errors = root.addNewErrors();
			ErrorType error1 = errors.addNewError();
			error1.setCode(TipMessager.getErrorCode(errCode));

			// 错误描述
			StringBuilder errMsg = new StringBuilder();
			String errDesc = TipMessager.getMessage(errCode, ApiServletHolder.getApiContext().getLanguage());
			if (StringUtils.hasLength(errDesc)) {
				errMsg = errMsg.append(errDesc);
			}

			String lan = ApiServletHolder.getApiContext().getLanguage();
			// 剩余登录次数.<=0时，用户被锁定
			String count = ret.getParm("count").getStringColumn();
			if (StringUtils.hasLength(count)) {
				int errCount;
				try {
					errCount = Integer.parseInt(count);
					errMsg = errMsg.append(" .").append(":").append(errCount);
				} catch (Exception e) {
					LOGGER.warn("The remaining logins is not a number. The value is:{}", count);
				}
			}

			error1.setStringValue(errMsg.toString());

			return true;
		}
		return false;
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
			// 会员账号
			Item memberidItem = commandRet.getParm("userid");
			String memberId = memberidItem == null ? null : memberidItem.getStringColumn();
			lccUserInfo.setUserID(memberId);
			// 姓名
			getPersonName(commandRet, lccUserInfo);
			// 性别 M 男 F 女
			Item genderItem = commandRet.getParm("gender");
			String gender = genderItem == null ? null : genderItem.getStringColumn();
			// if (StringUtils.hasLength(gender)){
			lccUserInfo.setGender(gender);
			// }
			// 出生日期
			getBirthdayItem(commandRet, lccUserInfo);
			//配置值
			Item promotionemailItem = commandRet.getParm("configValue");
			String promotionEmail = promotionemailItem == null ? null : promotionemailItem.getStringColumn();
			// if (StringUtils.hasLength(promotionEmail)){
			lccUserInfo.setPromotionEmail(promotionEmail);
			// }
			//国籍
			 String nationality = commandRet.getParm("nationality").getStringColumn();
			lccUserInfo.setCitizenshipCountryCode(nationality);
			// 证件信息
			getIdlistItem(commandRet, lccUserInfo);
			// 地址信息
			getAddresslistItem(commandRet, lccUserInfo);
			// 联系信息
			getContactlistItem(commandRet, lccUserInfo);
			//乘机人可配项
			getPassengerInfo(commandRet, lccUserInfo);

		} catch (Exception e) {
			doc = LCCUserReadRSDocument.Factory.newInstance();
			root = doc.addNewLCCUserReadRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM, TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}
		return doc;
	}

	// /**
	// * 用户操作.更新登录状态
	// * @param userID 用户id
	// * @param channel 渠道
	// * @param airline 航班
	// * @param lastTime 超时时间
	// * @throws APIException APIException
	// */
	// public static void updLoginStatus(String userID, String channel, String
	// airline, Date lastTime)
	// throws APIException {
	// String key = userID + channel;
	// try {
	// String value = RedisManager.getManager().get(key);
	// value = new StringBuffer(value.split("\\+")[0])
	// .append("+")
	// .append(DateUtils.getInstance().formatDate(lastTime, "yyyyMMddHHmmss"))
	// .toString();
	//
	// // 更新超时时间
	// updRedisTimeout(key, value, airline, channel);
	// }
	// catch (Exception e) {
	// RedisManager.getManager().set(key, null, 1);
	// LOGGER.error(TipMessager.getInfoMessage(
	// ErrCodeConstants.API_DATA_LOGIN_STATUS,
	// language),e);
	// throw e;
	// }
	// }

	/**
	 * 更新Redis缓存超时时间
	 * 
	 * @param key
	 *            签名
	 * @param value
	 *            值
	 * @param airline
	 *            航班
	 * @param channel
	 *            渠道
	 *//*
		 * private static void updRedisTimeout(String key, String value, String
		 * airline, String channel) { // 获取超时时间 ChannelInfo channelInfo =
		 * DbMemory.getChannel(airline, channel); int second = channelInfo ==
		 * null? 1 : Integer.valueOf(channelInfo.getTimeout()).intValue();
		 * RedisManager.getManager().set(key, value, second); }
		 */

	/**
	 * 用户登录.更新登录状态
	 * 
	 * @param userID
	 *            用户ID
	 * @param channel
	 *            渠道
	 * @param airline
	 *            航班
	 *//*
		 * private void updLoginStatus(String userID, String channel, String
		 * airline) throws APIException { if (!StringUtils.hasLength(userID)) {
		 * LOGGER.info(TipMessager.getInfoMessage(
		 * ErrCodeConstants.API_DATA_LOGIN_STATUS, language)); throw
		 * APIException.getInstance(ErrCodeConstants.API_DATA_LOGIN_STATUS); }
		 * 
		 * String key = userID + channel; try { DateUtils dateUtils =
		 * DateUtils.getInstance(); Date date = new Date(); String value = new
		 * StringBuffer(dateUtils.formatDate(date, "yyyyMMddHHmmss"))
		 * .append("+") .append(dateUtils.formatDate(date, "yyyyMMddHHmmss"))
		 * .toString();
		 * 
		 * // 更新超时时间 updRedisTimeout(key, value, airline, channel); } catch
		 * (Exception e) { RedisManager.getManager().set(key, null, 1);
		 * LOGGER.error(TipMessager.getInfoMessage(
		 * ErrCodeConstants.API_DATA_LOGIN_STATUS, language),e); throw e; } }
		 */
	/**
	 * 出生日期
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getBirthdayItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item birthdayItem = commandRet.getParm("birthday");
		String birthday = birthdayItem == null ? null : birthdayItem.getStringColumn();
		if (StringUtils.hasLength(birthday)) {
			DateUtils dateUtils = DateUtils.getInstance();
			try {
				lccUserInfo.addNewAge().addNewBirthDate()
						.setStringValue(dateUtils.formatDate(dateUtils.parseDate(birthday, "yyyyMMdd"), "yyyy-MM-dd"));
			} catch (ParseException e) {
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
	}

	/**
	 * 证件信息
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getIdlistItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item idlistItem = commandRet.getParm("idlist");
		Table idListTable = idlistItem == null ? null : idlistItem.getTableColumn();
		int count = idListTable == null ? 0 : idListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				FOID foid = lccUserInfo.addNewFOID();
				Row idListRow = idListTable.getRow(i);
				// ID标识
				Item idItem = idListRow.getColumn("id");
				String id = idItem == null ? null : idItem.getStringColumn();
				// if (StringUtils.hasLength(id)){
				foid.setFOIDKey(id);
				// }
				// 证件类别
				Item idTypeItem = idListRow.getColumn("idtype");
				String idType = idTypeItem == null ? null : idTypeItem.getStringColumn();
				// if (StringUtils.hasLength(idType)){
				foid.setType(idType);
				// }
				// 证件号
				Item idNoItem = idListRow.getColumn("idno");
				String idNo = idNoItem == null ? null : idNoItem.getStringColumn();

				// if (StringUtils.hasLength(idNo)){
				foid.addNewID().setStringValue(idNo);
				String vendorCode = idListRow.getColumn("status").getStringColumn();
				foid.getID().setVendorCode(vendorCode);
				// }
			}
		} else {
			FOID foid = lccUserInfo.addNewFOID();
			foid.setFOIDKey(null);
			foid.setType("");
			foid.addNewID();
			foid.getID().setVendorCode(null);
		}

	}

	/**
	 * 地址信息
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getAddresslistItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item addresslistItem = commandRet.getParm("addresslist");
		Table addressListTable = addresslistItem == null ? null : addresslistItem.getTableColumn();
		int count = addressListTable == null ? 0 : addressListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				Address addressEle = lccUserInfo.addNewAddress();
				Row addrRow = addressListTable.getRow(i);
				// Id标识
				Item idItem = addrRow.getColumn("id");
				String id = idItem == null ? null : idItem.getStringColumn();
				// if (StringUtils.hasLength(id)){
				addressEle.setAddressKey(id);
				// }
				// 省
				Item provinceItem = addrRow.getColumn("province");
				String province = provinceItem == null ? null : provinceItem.getStringColumn();
				// if (StringUtils.hasLength(province)){
				addressEle.setCountryName(province);
				// }
				// 市
				Item cityItem = addrRow.getColumn("city");
				String city = cityItem == null ? null : cityItem.getStringColumn();
				// if (StringUtils.hasLength(city)){
				addressEle.setCityName(city);
				// }
				// 邮政编码
				Item zipCodeItem = addrRow.getColumn("zipcode");
				String zipCode = zipCodeItem == null ? null : zipCodeItem.getStringColumn();
				// if (StringUtils.hasLength(zipCode)){
				addressEle.setPostalCode(zipCode);
				// }
				// 地址
				Item addressItem = addrRow.getColumn("address");
				String address = addressItem == null ? null : addressItem.getStringColumn();
				// if (StringUtils.hasLength(address)){
				addressEle.setStreetNmbr(address);
				// }
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
	 * 联系信息
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getContactlistItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item contactlistItem = commandRet.getParm("contactlist");
		Table contactListTable = contactlistItem == null ? null : contactlistItem.getTableColumn();
		int count = contactListTable == null ? 0 : contactListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				ContactInfo contactInfo = lccUserInfo.addNewContactInfo();
				Row contactRow = contactListTable.getRow(i);
				// ID标识
				Item idItem = contactRow.getColumn("id");
				String id = idItem == null ? null : idItem.getStringColumn();
				// if (StringUtils.hasLength(id)){
				contactInfo.setID(id);
				// }
				// 区号
				Item areaItem = contactRow.getColumn("area");
				String area = areaItem == null ? null : areaItem.getStringColumn();
				contactInfo.setAreaCode(area);
				// 类别 tel电话 mobile手机 email电邮 qq QQ webchat 微信
				Item typeItem = contactRow.getColumn("type");
				String type = typeItem == null ? null : typeItem.getStringColumn();
				// if (StringUtils.hasLength(type)){
				contactInfo.setType(type);
				// }
				// 号码
				Item noItem = contactRow.getColumn("no");
				String no = noItem == null ? null : noItem.getStringColumn();
				// if (StringUtils.hasLength(no)){
				contactInfo.setNumber(no);
				// }
				contactInfo.setStatus(contactRow.getColumn("status").getStringColumn());
			}
		} else {
			ContactInfo contactInfo = lccUserInfo.addNewContactInfo();
			contactInfo.setID(null);
			contactInfo.setType(null);
			contactInfo.setNumber(null);
			contactInfo.setStatus(null);
			contactInfo.setAreaCode(null);
		}

	}

	/**
	 * 乘机人的配置信息
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getPassengerInfo(CommandRet commandRet, LCCUserType lccUserInfo) {
		CommandData paxInfoTemplateItem = commandRet.getParm("paxInfoTemplate").getObjectColumn();
		if (paxInfoTemplateItem != null && !"".equals(paxInfoTemplateItem)) {
			// RouteType：I 国际；D 国内
			LCCPaxInfoTemplate domesticFITTemp = lccUserInfo.addNewLCCPaxInfoTemplate();
			domesticFITTemp.setRouteType("D");
			LCCPaxInfoTemplate InterFITTemp = lccUserInfo.addNewLCCPaxInfoTemplate();
			InterFITTemp.setRouteType("I");
			// 添加国内散客
			CommandData domesticFITTempItem = paxInfoTemplateItem.getParm("DomesticFITTemp").getObjectColumn();
			if (domesticFITTempItem != null && !"".equals(domesticFITTempItem)) {
				// OrderType：F 散客；G 团队
				LCCRouteTemplateInfo routeTemplateInfo = domesticFITTemp.addNewLCCRouteTemplateInfo();
				routeTemplateInfo.setOrderType("F");
				addResultPassengerInfo(domesticFITTempItem, routeTemplateInfo);
			}
			// 添加国内团队
			CommandData domesticGroupTempItem = paxInfoTemplateItem.getParm("DomesticGroupTemp").getObjectColumn();
			if (domesticGroupTempItem != null && !"".equals(domesticGroupTempItem)) {
				// OrderType：F 散客；G 团队
				LCCRouteTemplateInfo routeTemplateInfo = domesticFITTemp.addNewLCCRouteTemplateInfo();
				routeTemplateInfo.setOrderType("G");
				addResultPassengerInfo(domesticGroupTempItem, routeTemplateInfo);
			}
			// 添加国际散客
			CommandData InterFITTempItem = paxInfoTemplateItem.getParm("InterFITTemp").getObjectColumn();
			if (InterFITTempItem != null && !"".equals(InterFITTempItem)) {
				// OrderType：F 散客；G 团队
				LCCRouteTemplateInfo routeTemplateInfo = InterFITTemp.addNewLCCRouteTemplateInfo();
				routeTemplateInfo.setOrderType("F");
				addResultPassengerInfo(InterFITTempItem, routeTemplateInfo);
			}
			// 添加国际团队
			CommandData interGroupTempItem = paxInfoTemplateItem.getParm("InterGroupTemp").getObjectColumn();
			if (interGroupTempItem != null && !"".equals(interGroupTempItem)) {
				// OrderType：F 散客；G 团队
				LCCRouteTemplateInfo routeTemplateInfo = InterFITTemp.addNewLCCRouteTemplateInfo();
				routeTemplateInfo.setOrderType("G");
				addResultPassengerInfo(interGroupTempItem, routeTemplateInfo);
			}

		}
	}

	/**
	 * 乘机人的配置信息添加
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void addResultPassengerInfo(CommandData item, LCCRouteTemplateInfo routeTemplateInfo) {
		// 添加成人
		CommandData ADTItem = item.getParm("ADT").getObjectColumn();
		if (ADTItem != null && !"".equals(ADTItem)) {
			LCCOrderTemplateInfo orderTemplateInfo = routeTemplateInfo.addNewLCCOrderTemplateInfo();
			// PaxType：旅客类型（ADT 成人；CHD 儿童；INF 婴儿；CONTACT 联系人）
			orderTemplateInfo.setPaxType("ADT");
			addDetailPassenger(ADTItem, orderTemplateInfo);
		}
		// 添加儿童
		CommandData CHDItem = item.getParm("CHD").getObjectColumn();
		if (CHDItem != null && !"".equals(CHDItem)) {
			LCCOrderTemplateInfo orderTemplateInfo = routeTemplateInfo.addNewLCCOrderTemplateInfo();
			// PaxType：旅客类型（ADT 成人；CHD 儿童；INF 婴儿；CONTACT 联系人）
			orderTemplateInfo.setPaxType("CHD");
			addDetailPassenger(CHDItem, orderTemplateInfo);
		}
		// 添加婴儿
		CommandData INFItem = item.getParm("INF").getObjectColumn();
		if (INFItem != null && !"".equals(INFItem)) {
			LCCOrderTemplateInfo orderTemplateInfo = routeTemplateInfo.addNewLCCOrderTemplateInfo();
			// PaxType：旅客类型（ADT 成人；CHD 儿童；INF 婴儿；CONTACT 联系人）
			orderTemplateInfo.setPaxType("INF");
			addDetailPassenger(INFItem, orderTemplateInfo);
		}
		// 添加联系人
		CommandData contactItem = item.getParm("contact").getObjectColumn();
		if (contactItem != null && !"".equals(contactItem)) {
			LCCOrderTemplateInfo orderTemplateInfo = routeTemplateInfo.addNewLCCOrderTemplateInfo();
			// PaxType：旅客类型（ADT 成人；CHD 儿童；INF 婴儿；CONTACT 联系人）
			orderTemplateInfo.setPaxType("CONTACT");
			addDetailPassenger(contactItem, orderTemplateInfo);
		}
		// 添加乘机人
		CommandData PAXItem = item.getParm("PAX").getObjectColumn();
		if (PAXItem != null && !"".equals(PAXItem)) {
			LCCOrderTemplateInfo orderTemplateInfo = routeTemplateInfo.addNewLCCOrderTemplateInfo();
			// PaxType：旅客类型（ADT 成人；CHD 儿童；INF 婴儿；CONTACT 联系人）
			orderTemplateInfo.setPaxType("PAX");
			addDetailPassenger(PAXItem, orderTemplateInfo);
		}
	}

	/**
	 * 乘机人的具体配置信息添加
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void addDetailPassenger(CommandData item, LCCOrderTemplateInfo orderTemplateInfo) {
		// 出生日期
		String birthday = "";
		// 性别
		String sex = "";
		// 证件号
		String certificateno = "";
		// 邮箱地址
		String emailaddress = "";
		// 名
		String name = "";
		// 证件签发国和地区certificateissuingcountryandregion
		String CFSCRegion = "";
		// 姓
		String surname = "";
		// 证件有效日期
		String validdateofcertificate = "";
		// 国家和地区区号
		String countryandregioncode = "";
		// 证件类型
		String certificatetype = "";
		// 手机号码
		String mobileno = "";
		// 选择监护人
		String selectguardian = "";
		// 团队type
		String type = "";
		// 国籍
		String nationality = "";
		birthday = item.getParm("birthday").getStringColumn();
		sex = item.getParm("sex").getStringColumn();
		certificateno = item.getParm("certificateno").getStringColumn();
		emailaddress = item.getParm("emailaddress").getStringColumn();
		name = item.getParm("name").getStringColumn();
		CFSCRegion = item.getParm("certificateissuingcountryandregion").getStringColumn();
		surname = item.getParm("surname").getStringColumn();
		validdateofcertificate = item.getParm("validdateofcertificate").getStringColumn();
		countryandregioncode = item.getParm("countryandregioncode").getStringColumn();
		certificatetype = item.getParm("certificatetype").getStringColumn();
		mobileno = item.getParm("mobileno").getStringColumn();
		selectguardian = item.getParm("selectguardian").getStringColumn();
		type = item.getParm("type").getStringColumn();
		nationality = item.getParm("nationality").getStringColumn();
		// 性别
		if (!checkPassengerIsEmpty(sex)) {
			orderTemplateInfo.setGender(sex);
		}
		// 姓名
		if (!checkPassengerIsEmpty(surname, name)) {
			org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo.LCCOrderTemplateInfo.PersonName personName = orderTemplateInfo
					.addNewPersonName();
			personName.setSurname(surname);
			personName.setGivenName(name);
		}
		// 证件相关
		if (!checkPassengerIsEmpty(certificatetype, certificateno)) {
			org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo.LCCOrderTemplateInfo.FOID foId = orderTemplateInfo
					.addNewFOID();
			// 证件类型
			foId.setType(certificatetype);
			// 证件号
			foId.addNewID().setStringValue(certificateno);
			if (!checkPassengerIsEmpty(validdateofcertificate, CFSCRegion)) {
				// 证件有效日期
				foId.setExpiryDate(validdateofcertificate);
				// 证件签发国和地区
				foId.setIssueCountry(CFSCRegion);
			}
		}
		// 出生日期
		if (!checkPassengerIsEmpty(birthday)) {
			orderTemplateInfo.addNewAge().setBirthDate(birthday);
		}
		// 联系方式
		if (!checkPassengerIsEmpty(countryandregioncode, mobileno)) {
			org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo.LCCOrderTemplateInfo.ContactInfo contactInfo = orderTemplateInfo
					.addNewContactInfo();
			contactInfo.setType("mobile");
			// 国家和地区区号
			contactInfo.setAreaCode(countryandregioncode);
			// 手机号
			contactInfo.setNumber(mobileno);
		}
		// 邮箱
		if (!checkPassengerIsEmpty(emailaddress)) {
			org.iata.iata.edist.LCCUserType.LCCPaxInfoTemplate.LCCRouteTemplateInfo.LCCOrderTemplateInfo.ContactInfo emailContactInfo = orderTemplateInfo
					.addNewContactInfo();
			emailContactInfo.setType("mail");
			emailContactInfo.setNumber(emailaddress);
		}
		// 监护人
		if (!checkPassengerIsEmpty(selectguardian)) {
			orderTemplateInfo.setPaxAssociation(selectguardian);
		}
		// 团队type需给xml位置
		if (!checkPassengerIsEmpty(type)) {
			// 
		}
		//国籍
		if (!checkPassengerIsEmpty(nationality)) {
			orderTemplateInfo.setCitizenshipCountryCode(nationality);
		}

	}

	private static boolean checkPassengerIsEmpty(String... params) {
		boolean isFaile = true;
		E: for (int i = 0; i < params.length; i++) {
			if (params[i] != null && !"".equals(params[i])) {
				isFaile = false;
			} else {
				isFaile = true;
				break E;
			}
		}
		return isFaile;
	}

	/**
	 * 姓、名 、拼音姓、拼音名
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getPersonName(CommandRet commandRet, LCCUserType lccUserInfo) {
		// 姓
		PersonName personName = lccUserInfo.addNewPersonName();
		Item lastnameItem = commandRet.getParm("lastname");
		String lastName = lastnameItem == null ? null : lastnameItem.getStringColumn();
		personName.setSurname(lastName);
		// 名
		Item firstnameItem = commandRet.getParm("firstname");
		String firstName = firstnameItem == null ? null : firstnameItem.getStringColumn();
		// if (StringUtils.hasLength(firstName)){
		personName.setGivenName(firstName);
		// }
		// 拼音姓
		Item pinyinxingItem = commandRet.getParm("pinyinxing");
		String spellSurName = pinyinxingItem == null ? null : pinyinxingItem.getStringColumn();
		// if (StringUtils.hasLength(spellSurName)){
		personName.setSpellSurname(spellSurName);
		// }
		// 拼音名
		Item pinyinmingItem = commandRet.getParm("pinyinming");
		String spellGivenName = pinyinmingItem == null ? null : pinyinmingItem.getStringColumn();
		// if (StringUtils.hasLength(spellGivenName)){
		personName.setSpellGivenName(spellGivenName);
		// }
	}

}
