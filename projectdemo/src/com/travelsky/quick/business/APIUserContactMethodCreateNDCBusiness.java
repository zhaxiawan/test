package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserCreateRQDocument;
import org.iata.iata.edist.LCCUserCreateRQDocument.LCCUserCreateRQ.Create;
import org.iata.iata.edist.LCCUserCreateRSDocument;
import org.iata.iata.edist.LCCUserCreateRSDocument.LCCUserCreateRS;
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
 * 会员信息的增加（联系方式）
 * @author LiHongZhi
 * @version 0.1
 * 类型说明:
 * 			USER_INSERT_CONTACTMETHOD 联系方式
 */
@Service("LCC_USERCONTACTMETHODCREATE_SERVICE")
public class APIUserContactMethodCreateNDCBusiness  extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5025481288373902872L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserContactMethodCreateNDCBusiness.class);
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try{
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean
			context.setRet(getResponseBean());
		}
		//请求  xm转换CommandData 异常
		catch (APIException e) { 
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_USER_CREATE,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
			//转换ResponseBean-->XmlBean
			return transRespBeanToXmlBean(commandRet,input);
	}

	
	
	
	//---------------------工厂区--------------------------------------------
	
		/**
		 * 转换 xml-->Reqbean
		 * @param context 前台获取的xml数据
		 * @param xmlInput 前台获取的xml数据
		 * @throws APIException APIException
		 * @throws Exception Exception
		 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
			CommandData input = context.getInput();
			LCCUserCreateRQDocument rootDoc = null;
			rootDoc = LCCUserCreateRQDocument.Factory.parse(xmlInput);
			LCCUserCreateRQDocument.LCCUserCreateRQ reqdoc = rootDoc.getLCCUserCreateRQ();
			// 部门ID
			String channelNo = context.getContext().getChannelNo();
			//渠道号
			input.addParm("channelno", channelNo);
			input.addParm("tktdeptid",context.getContext().getTicketDeptid());
			// 添加用户id  orderno
			input.addParm("memberid", context.getContext().getUserID());
			Create  create = reqdoc.getCreate();
			//修改信息类型
			String  actionType = create.getActionType();
			if (!StringUtils.hasLength(actionType)) {
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_ACTIONTYPE, 
						ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_ACTIONTYPE);
			}
			input.addParm("actionType", actionType);
			//会员帐号
			String memberid = context.getContext().getUserID();
			 // 会员帐号为空异常
			if(!StringUtils.hasLength(memberid)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_USER_ID, 
						ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_USER_ID);
			}
			input.addParm("memberid", memberid);
			//会员信息增加_联系方式
			insertContactmethodXmlToRequestBean(input, create);
		}
		/**
		 * 数据提交shopping后台
		 * @param input  请求的XML参数
		 * @param context 用于调用doOther请求后台数据
		 * @return  请求后台返回的对象
		 */
		public CommandRet getResponseBean() {
			SelvetContext<ApiContext> context = ApiServletHolder.get();
			//会员信息增加_联系方式
			CommandData input = context.getInput();
			MemberManager memberManager=new MemberManager();
			String actionType=input.getParm("actionType").getStringColumn();
			if ("USER_INSERT_CONTACTMETHOD_EMAIL".equals(actionType)) {
				return memberManager.addContactEmail(input, context);
			}else{
				return memberManager.addContact(input, context);
			}
		}
		/**
		 * 转换ResponseBean-->XmlBean
		 * @param commandRet 后台返回的结果集
		 * @param input  B2C请求的XML
		 * @return  XML结果集
		 */
		public XmlObject transRespBeanToXmlBean(Object commandRet ,CommandData input) {
			CommandRet xmlOutput = (CommandRet)commandRet;
			LCCUserCreateRSDocument doc = LCCUserCreateRSDocument.Factory.newInstance();
			LCCUserCreateRS rs = doc.addNewLCCUserCreateRS();
			try{
				String errorcode = xmlOutput.getErrorCode();
				if(StringUtils.hasLength(errorcode)){
					ErrorType error = rs.addNewErrors().addNewError();
					error.setCode(TipMessager.getErrorCode(errorcode));
					error.setShortText(TipMessager.getMessage(errorcode,
							ApiServletHolder.getApiContext().getLanguage()));
				}else{ 
					//成功标识
					rs.addNewSuccess();
					//地址在数据库中的ID
					String addressID = xmlOutput.getParm("id").getStringColumn();
					//会员信息增加_联系方式
					rs.addNewResponse().addNewLCCUserInfo().
						addNewContactInfo().setID(addressID);
				}
			} 
			catch (Exception e) {
				//初始化XML节点
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
		
//-----------------------xml-->Reqbean-元素区----------------------------------------------------------------		
		
		/**
		 * 会员信息增加联系方式xml-->Reqbean
		 * @param input 请求的XML
		 * @param create 节点
		 * @throws APIException APIException
		 */
		public void insertContactmethodXmlToRequestBean(CommandData input,Create  create )
			throws APIException {
			//类型
			String type = ""; 
			//号码
			String number = ""; 
			//区号
			String area="";
			ContactInfo[] contactInfo = create.getLCCUserInfo().getContactInfoArray();
			 if(null != contactInfo && contactInfo.length>0){
				 type = contactInfo[0].getType();
				 number = contactInfo[0].getNumber();
				 area = contactInfo[0].getAreaCode();
				 if(!StringUtils.hasLength(type)){
					 LOGGER.info(TipMessager.getInfoMessage(
								ErrCodeConstants.API_NULL_CONTACT_TYPE, 
								ApiServletHolder.getApiContext().getLanguage()));
					 throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_TYPE);
				 }	
				if(!StringUtils.hasLength(number)){
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_CONTACT_NO, 
							ApiServletHolder.getApiContext().getLanguage()));
					 throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_NO);
				 }	
			 }else{
				 LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_CONTACT, 
							ApiServletHolder.getApiContext().getLanguage()));
				 throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT);
			 }
			input.addParm("type", type);
			input.addParm("no", number);
			input.addParm("area", area);
		}
		
}
