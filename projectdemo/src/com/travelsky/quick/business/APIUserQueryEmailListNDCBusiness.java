package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.ErrorsType;
import org.iata.iata.edist.LCCUserReadRQDocument;
import org.iata.iata.edist.LCCUserReadRQDocument.LCCUserReadRQ.Query;
import org.iata.iata.edist.LCCUserReadRQDocument.LCCUserReadRQ.Query.LCCUserInfo;
import org.iata.iata.edist.LCCUserReadRSDocument;
import org.iata.iata.edist.LCCUserReadRSDocument.LCCUserReadRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 获取邮箱列表接口
 * 
 * @author zxw
 *
 */
@Service("LCC_QUERYEMAILLIST_SERVICE")
public class APIUserQueryEmailListNDCBusiness extends AbstractService<ApiContext> {
	/**
	 *
	 */
	private static final long serialVersionUID = 8691825821149118999L;
	/**
	 *
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserQueryEmailListNDCBusiness.class);

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
		return memberManager.getEmailList(input, context);
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
		 String userId = lccUserInfo.getUserID();
		// Validate LCCUserInfo
		if (!StringUtils.hasLength(userId)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ACCOUNT_PASSWORD,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ACCOUNT_PASSWORD);
		}
		// userId
		input.addParm("userId", userId);
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
			String errDesc = ret.getErrorDesc();
			if (StringUtils.hasLength(errDesc)) {
				errMsg = errMsg.append(errDesc);
			}
			error1.setStringValue(errMsg.toString());
			return true;
		}
		return false;
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		// 看是否有缓存数据，有就直接返回
		CommandRet xmlOutput = (CommandRet) commandRet;
		String redisValue = xmlOutput.getParm("LCC_QUERYEMAILLIST_SERVICE").getStringColumn();
		if (redisValue != null && !"".equals(redisValue)) {
			try {
				LCCUserReadRSDocument document = LCCUserReadRSDocument.Factory.parse(redisValue);
				return document;
			} catch (XmlException e) {
				e.printStackTrace();
			}
		}
		LCCUserReadRSDocument doc = LCCUserReadRSDocument.Factory.newInstance();
		LCCUserReadRS root = doc.addNewLCCUserReadRS();
		try {
			if (processError(commandRet, root)) {
				return doc;
			}
			root.addNewSuccess();
			LCCUserType lccUserInfo = root.addNewResponse().addNewLCCUserInfo();
			// 会员账号
			Item memberidItem = commandRet.getParm("userId");
			String memberId = memberidItem == null ? null : memberidItem.getStringColumn();
			lccUserInfo.setUserID(memberId);
			// 邮箱列表
			getEmaillistItem(commandRet, lccUserInfo);

		} catch (Exception e) {
			doc = LCCUserReadRSDocument.Factory.newInstance();
			root = doc.addNewLCCUserReadRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM, TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}
		//添加到缓存
		String userId = input.getParm("userId").getStringColumn();
		RedisManager.getManager().set(RedisNamespaceEnum.api_service_emaillist.toKey(userId), doc.toString(), 600);
		return doc;
	}

	/**
	 * 邮箱信息
	 * 
	 * @param commandRet
	 *            后台返回的数据对象
	 * @param lccUserInfo
	 *            XML节点
	 */
	private static void getEmaillistItem(CommandRet commandRet, LCCUserType lccUserInfo) {
		Item emailListItem = commandRet.getParm("contactlist");
		Table emailListTable = emailListItem == null ? null : emailListItem.getTableColumn();
		int count = emailListTable == null ? 0 : emailListTable.getRowCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				Row contactRow = emailListTable.getRow(i);
				// 只返回邮箱列表
				Item typeItem = contactRow.getColumn("contacttype");
				String type = typeItem == null ? null : typeItem.getStringColumn();
				if ("email".equals(type)) {
					ContactInfo contactInfo = lccUserInfo.addNewContactInfo();
					contactInfo.setType(type);
					// ID标识
					Item idItem = contactRow.getColumn("memberid");
					String id = idItem == null ? null : idItem.getStringColumn();
					contactInfo.setID(id);
					// 类别 tel电话 mobile手机 email电邮 qq QQ webchat 微信

					// 号码
					Item noItem = contactRow.getColumn("contactvalue");
					String no = noItem == null ? null : noItem.getStringColumn();
					contactInfo.setNumber(no);
					contactInfo.setStatus(contactRow.getColumn("status").getStringColumn());
				}
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

}
