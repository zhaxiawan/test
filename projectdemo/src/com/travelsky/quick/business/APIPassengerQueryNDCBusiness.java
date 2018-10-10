package com.travelsky.quick.business;

import java.text.ParseException;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserReadRQDocument;
import org.iata.iata.edist.LCCUserReadRQDocument.LCCUserReadRQ.Query;
import org.iata.iata.edist.LCCUserReadRSDocument;
import org.iata.iata.edist.LCCUserReadRSDocument.LCCUserReadRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.Age;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo.ContactInfo;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo.FOID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DateUtils;

import org.springframework.util.StringUtils;

import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:查询常用乘机人接口
 * 
 * @author huxizhun
 *
 */
@Service("LCC_PASSENGERQUERY_SERVICE")
public class APIPassengerQueryNDCBusiness extends AbstractService<ApiContext> {

	/**
	 *
	 */
	private static final long serialVersionUID = -2595881636351455263L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIPassengerQueryNDCBusiness.class);

	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();

		try {
			// 转换 xml-->Reqbean.并进行处理
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_ALWAYS_PASSENGER,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	private CommandRet getResult() {
		CommandRet ret = new CommandRet("");
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		MemberManager memberManager = new MemberManager();
		String type = input.getParm("type").getStringColumn();
		if ("QUERY_PASSENGER".equals(type)) {
			ret = memberManager.getPassenger(input, context);
		} else if ("QUERY_PASSENGERANDUSER".equals(type)) {
			ret = memberManager.getPassengerAndUser(input, context);
		}
		return ret;
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
		// LCCUserReadRQ->Query
		Query query = reqDoc.getQuery();
		String type = query.getActionType();
		input.addParm("type", type);
		// 乘机人ID
		String paxid = "";
		if(query.getLCCUserInfo()!=null && query.getLCCUserInfo().getUserID()!=null){
			paxid = query.getLCCUserInfo().getUserID();
		}
		input.addParm("paxid", paxid);
		// 登录用户id
		input.addParm("memberid", context.getContext().getUserID());
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		LCCUserReadRSDocument doc = LCCUserReadRSDocument.Factory.newInstance();
		LCCUserReadRS root = doc.addNewLCCUserReadRS();
		try {
			if (!processError(commandRet, root)) {
				root.addNewSuccess();
				LCCUserType lccUserInfo = root.addNewResponse().addNewLCCUserInfo();
				transPassenger(lccUserInfo, commandRet);
			}
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
	 * 转换查询常用乘机人结果到XmlBean
	 * 
	 * @param lccUserInfo
	 *            节点
	 * @param commandRet
	 *            结果集
	 */
	private void transPassenger(LCCUserType lccUserInfo, CommandRet commandRet) {
		// 常用乘机人
		Item passengerItem = commandRet.getParm("passenger");
		Table passengerTable = passengerItem == null ? null : passengerItem.getTableColumn();
		int count = passengerTable == null ? 0 : passengerTable.getRowCount();
		for (int i = 0; i < count; i++) {
			LCCPassengerInfo lccPassengerInfo = lccUserInfo.addNewLCCPassengerInfo();
			Row passengerRow = passengerTable.getRow(i);
			Item idItem = passengerRow.getColumn("id");
			String id = idItem == null ? null : idItem.getStringColumn();
			if (StringUtils.hasLength(id)) {
				lccPassengerInfo.setPassengerID(id);
			}
			// 姓
			Item lastnameItem = passengerRow.getColumn("lastname");
			String lastname = lastnameItem == null ? null : lastnameItem.getStringColumn();
			org.iata.iata.edist.LCCUserType.LCCPassengerInfo.PersonName personName = lccPassengerInfo
					.addNewPersonName();
			personName.setSurname(lastname);
			// 名
			Item firstnameItem = passengerRow.getColumn("firstname");
			String firstname = firstnameItem == null ? null : firstnameItem.getStringColumn();
			if (StringUtils.hasLength(firstname)) {
				personName.setGivenName(firstname);
			}
			// 拼音姓名
			Item pinyinItem = passengerRow.getColumn("pinyin");
			String pinyinname = pinyinItem == null ? null : pinyinItem.getStringColumn();
			personName.setSpellGivenName(pinyinname);
			// 证件类别
			Item idtypeItem = passengerRow.getColumn("idtype");
			String idtype = idtypeItem == null ? null : idtypeItem.getStringColumn();
			boolean idTypeIsEmpty = !StringUtils.hasLength(idtype);
			// 证件号
			Item idnoItem = passengerRow.getColumn("idno");
			String idno = idnoItem == null ? null : idnoItem.getStringColumn();
			boolean idNoIsEmtpy = !StringUtils.hasLength(idno);
			if (!idTypeIsEmpty && !idNoIsEmtpy) {
				FOID foid = lccPassengerInfo.addNewFOID();
				foid.setType(idtype);
				foid.addNewID().setStringValue(idno);
				if (idtype.equals("PP")) {
					String issuecountry = passengerRow.getColumn("issuecountry").getStringColumn();
					String expiry = passengerRow.getColumn("expiry").getStringColumn();
					foid.setExpiryDate(expiry);
					foid.setIssueCountry(issuecountry);
				}
			}
			// 区号
			Item areaTtem = passengerRow.getColumn("area");
			String area = areaTtem == null ? null : areaTtem.getStringColumn();
			// 手机号
			Item mobileItem = passengerRow.getColumn("mobile");
			String mobile = mobileItem == null ? null : mobileItem.getStringColumn();
			if (StringUtils.hasLength(mobile)) {
				ContactInfo contactInfo = lccPassengerInfo.addNewContactInfo();
				contactInfo.setType(CommonConstants.API_TYPE_MOBILE);
				contactInfo.setNumber(mobile);
				// 区号
				contactInfo.setAreaCode(area);
			}
			// 邮箱号
			Item emailItem = passengerRow.getColumn("email");
			String email = emailItem == null ? null : emailItem.getStringColumn();
			if (StringUtils.hasLength(email)) {
				ContactInfo contactInfo = lccPassengerInfo.addNewContactInfo();
				contactInfo.setType(CommonConstants.API_TYPE_EMAIL);
				contactInfo.setNumber(email);
			}
			// 出生日期
			Item birthdayItem = passengerRow.getColumn("birthday");
			String birthday = birthdayItem == null ? null : birthdayItem.getStringColumn();
			if (StringUtils.hasLength(birthday) && passengerRow.getColumn("birthday").getStringColumn().length() > 9) {
				lccPassengerInfo.addNewAge().addNewBirthDate()
						.setStringValue(passengerRow.getColumn("birthday").getStringColumn().substring(0, 10));
			}
			// 性别
			Item genderItem = passengerRow.getColumn("gender");
			String gender = genderItem == null ? null : genderItem.getStringColumn();
			if (StringUtils.hasLength(gender)) {
				lccPassengerInfo.setGender(gender);
			}
			//国籍
			String nationality = passengerRow.getColumn("nationality").getStringColumn();
			lccPassengerInfo.setCitizenshipCountryCode(nationality);
		}
	}
}
