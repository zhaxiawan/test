package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserCreateRQDocument;
import org.iata.iata.edist.LCCUserCreateRQDocument.LCCUserCreateRQ.Create;
import org.iata.iata.edist.LCCUserCreateRSDocument;
import org.iata.iata.edist.LCCUserCreateRSDocument.LCCUserCreateRS;
import org.iata.iata.edist.LCCUserType.FOID;
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
 * 会员信息的增加（证件号）
 * 
 * @author LiHongZhi
 * @version 0.1 类型说明: USER_INSERT_FOID 证件号
 */
@Service("LCC_USERFOIDCREATE_SERVICE")
public class APIUserFoIdCreateNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5025481288373902872L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserFoIdCreateNDCBusiness.class);

	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResponseBean());
		}
		// 请求 xm转换CommandData 异常
		catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_USER_CREATE,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		// 转换ResponseBean-->XmlBean
		return transRespBeanToXmlBean(commandRet, input);
	}

	// ---------------------工厂区--------------------------------------------

	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @param context
	 *            前台获取的xml数据
	 * @param xmlInput
	 *            前台获取的xml数据
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
		LCCUserCreateRQDocument.LCCUserCreateRQ reqdoc = rootDoc.getLCCUserCreateRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid", deptno);
		Create create = reqdoc.getCreate();
		// 会员帐号
		String memberid = context.getContext().getUserID();
		// 会员帐号为空异常
		if (!StringUtils.hasLength(memberid)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_USER_ID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
		}
		// 会员密码
		String oldPassword = create.getLCCUserInfo().getOldPassword();

		input.addParm("memberid", memberid);
		input.addParm("OldPassword", oldPassword);
		// 会员信息增加_证件号
		insertFoidXmlToRequestBean(input, create);
	}

	/**
	 * 数据提交shopping后台
	 * 
	 * @param input
	 *            请求的XML参数
	 * @param context
	 *            用于调用doOther请求后台数据
	 * @return 请求后台返回的对象
	 */
	public CommandRet getResponseBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();

		MemberManager memberManager = new MemberManager();
		// 会员信息增加_证件号
		return memberManager.addID(input, context);
	}

	/**
	 * 转换ResponseBean-->XmlBean
	 * 
	 * @param commandRet
	 *            后台返回的结果集
	 * @param input
	 *            B2C请求的XML
	 * @return XML结果集
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet, CommandData input) {
		CommandRet xmlOutput = (CommandRet) commandRet;
		LCCUserCreateRSDocument doc = LCCUserCreateRSDocument.Factory.newInstance();
		LCCUserCreateRS rs = doc.addNewLCCUserCreateRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setShortText(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
			} else {
				// 成功标识
				rs.addNewSuccess();
				// 地址在数据库中的ID
				String addressID = xmlOutput.getParm("id").getStringColumn();
				// 会员信息增加_证件号
				rs.addNewResponse().addNewLCCUserInfo().addNewFOID().setFOIDKey(addressID);
			}
		} catch (Exception e) {
			// 初始化XML节点
			doc = LCCUserCreateRSDocument.Factory.newInstance();
			rs = doc.addNewLCCUserCreateRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}

	// -----------------------xml-->Reqbean-元素区----------------------------------------------------------------
	/**
	 * 会员信息增加证件号xml-->Reqbean
	 * 
	 * @param input
	 *            请求的XML
	 * @param create
	 *            节点
	 * @throws APIException
	 *             APIException
	 */
	public void insertFoidXmlToRequestBean(CommandData input, Create create) throws APIException {
		// 证件类别
		String idtype = "";
		// 证件号
		String idno = "";
		// 签发国
		String issueCountry = "";
		// 有效期
		String expiryDate = "";
		FOID[] info = create.getLCCUserInfo().getFOIDArray();
		if (null != info && info.length > 0) {
			idtype = info[0].getType();
			idno = info[0].getID().getStringValue();
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
					expiryDate = info[0].getExpiryDate().substring(0, 10);
				} else {
					expiryDate = info[0].getExpiryDate();
				}
				issueCountry = info[0].getIssueCountry();
			}
		}
		// 证件信息为空异常
		else {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_IDINFO,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_IDINFO);
		}
		input.addParm("idtype", idtype);
		input.addParm("idno", idno);
		input.addParm("expiryDate", expiryDate);
		input.addParm("issueCountry", issueCountry);
	}
}
