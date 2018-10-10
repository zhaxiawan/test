package com.travelsky.quick.business;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserCreateRQDocument;
import org.iata.iata.edist.LCCUserCreateRQDocument.LCCUserCreateRQ.Create;
import org.iata.iata.edist.LCCUserCreateRSDocument;
import org.iata.iata.edist.LCCUserCreateRSDocument.LCCUserCreateRS;
import org.iata.iata.edist.LCCUserType;
import org.iata.iata.edist.LCCUserType.Age;
import org.iata.iata.edist.LCCUserType.ContactInfo;
import org.iata.iata.edist.LCCUserType.FOID;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo.PersonName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
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
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;
/**
 * 会员信息的增加（常用登机人）
 * @author LiHongZhi
 * @version 0.1
 * 类型说明:
 *			INSERT_PASSENGER 常用登机人
 */
@Service("LCC_PASSENGERCREATE_SERVICE")
public class APIUserPassengerCreateNDCBusiness  extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5025481288373902872L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserPassengerCreateNDCBusiness.class);
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try{
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean
			context.setRet(getResponseBean());
			if ("".equals(context.getRet().getErrorCode())) {
				context.setRet(getResult());
			}
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
			String channelNo = context.getContext().getChannelNo();
			//渠道号
			input.addParm("channelno", channelNo);
			input.addParm("tktdeptid",context.getContext().getTicketDeptid());
			Create  create = reqdoc.getCreate();
			
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
			//会员信息增加_常用乘机人
			insertPassengerXmlToRequestBean(input, create);
		}
		/**
		 * 数据提交shopping后台
		 * @param input  请求的XML参数
		 * @param context 用于调用doOther请求后台数据
		 * @return  请求后台返回的对象
		 */
		public CommandRet getResponseBean() {
			SelvetContext<ApiContext> context = ApiServletHolder.get();
			CommandData input = context.getInput();
			MemberManager memberManager=new MemberManager();
			//会员信息增加_常用乘机人
			return memberManager.addPassenger(input, context);	
		}
		
		private CommandRet getResult() {
			SelvetContext<ApiContext> context = ApiServletHolder.get();
			CommandData input = context.getInput();
			MemberManager memberManager=new MemberManager();
			return memberManager.getPassenger(input, context);
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
					
					LCCUserType lCCUserInfo = rs.addNewResponse().addNewLCCUserInfo();
					//成功标识
					rs.addNewSuccess();
					//地址在数据库中的ID
				//	String addressID = xmlOutput.getParm("id").getStringColumn();
					//会员信息增加_常用乘机人
					transPassenger(lCCUserInfo, commandRet,input);
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

		private void transPassenger(LCCUserType lCCUserInfo, Object commandRet, CommandData input) {
			// 常用乘机人
			Item passengerItem = ((CommandData) commandRet).getParm("passenger");
			Table passengerTable = passengerItem == null? null : passengerItem.getTableColumn();
			int count = passengerTable == null? 0 : passengerTable.getRowCount();
			lCCUserInfo.setUserID(input.getParm("memberid").getStringColumn());
			for (int i=0; i<count; i++) {
				LCCPassengerInfo lccPassengerInfo =lCCUserInfo.addNewLCCPassengerInfo();
				Row passengerRow = passengerTable.getRow(i);
				Item idItem = passengerRow.getColumn("id");
				String id = idItem == null? null : idItem.getStringColumn();
				if (StringUtils.hasLength(id)){
					lccPassengerInfo.setPassengerID(id);
				}
				// 姓
				Item lastnameItem = passengerRow.getColumn("lastname");
				String lastname = lastnameItem == null? null : lastnameItem.getStringColumn();
				org.iata.iata.edist.LCCUserType.LCCPassengerInfo.PersonName personName =
						lccPassengerInfo.addNewPersonName();
				personName.setSurname(lastname);
				// 名
				Item firstnameItem = passengerRow.getColumn("firstname");
				String firstname = firstnameItem == null? null : firstnameItem.getStringColumn();
				if (StringUtils.hasLength(firstname)){
					personName.setGivenName(firstname);
				}
				// 拼音姓名
				Item pinyinItem = passengerRow.getColumn("pinyin");
				String pinyinname = pinyinItem == null? null : pinyinItem.getStringColumn();
				personName.setSpellGivenName(pinyinname);
				// 证件类别
				Item idtypeItem = passengerRow.getColumn("idtype");
				String idtype = idtypeItem == null? null : idtypeItem.getStringColumn();
				boolean idTypeIsEmpty = !StringUtils.hasLength(idtype);
				// 证件号
				Item idnoItem = passengerRow.getColumn("idno");
				String idno = idnoItem == null? null : idnoItem.getStringColumn();
				boolean idNoIsEmtpy = !StringUtils.hasLength(idno);
				if (!idTypeIsEmpty && !idNoIsEmtpy) {
					org.iata.iata.edist.LCCUserType.LCCPassengerInfo.FOID foid = lccPassengerInfo.addNewFOID();
					foid.setType(idtype);
					foid.addNewID().setStringValue(idno);
					if(idtype.equals("PP")){
						String expiryDate = passengerRow.getColumn("expiryDate").getStringColumn();
						String issueCountry = passengerRow.getColumn("issueCountry").getStringColumn();
						foid.setExpiryDate(expiryDate);
						foid.setIssueCountry(issueCountry);
					}
				}
				// 手机号
				Item mobileItem = passengerRow.getColumn("mobile");
				String mobile = mobileItem == null? null : mobileItem.getStringColumn();
				if (StringUtils.hasLength(mobile)) {
					org.iata.iata.edist.LCCUserType.LCCPassengerInfo.ContactInfo contactInfo = lccPassengerInfo.addNewContactInfo();
					contactInfo.setType(CommonConstants.API_TYPE_MOBILE);
					contactInfo.setAreaCode(passengerRow.getColumn("area").getStringColumn());
					contactInfo.setNumber(mobile);
				}
				// 出生日期
				Item birthdayItem = passengerRow.getColumn("birthday");
				String birthday = birthdayItem == null? null : birthdayItem.getStringColumn();
				if (StringUtils.hasLength(birthday)){
					lccPassengerInfo.addNewAge().addNewBirthDate().setStringValue(birthday);
				}else {
					lccPassengerInfo.addNewAge().addNewBirthDate();
				}
				// 性别
				Item genderItem = passengerRow.getColumn("gender");
				String gender = genderItem == null? null : genderItem.getStringColumn();
				if (StringUtils.hasLength(gender)){
					lccPassengerInfo.setGender(gender);
				}
			}
		
		}

		//-----------------------xml-->Reqbean-元素区----------------------------------------------------------------		
		/**
		 *增加常用乘机人xml-->Reqbean
		 * @param input 请求的XML
		 * @param create 节点
		 * @throws APIException APIException
		 */
		public void insertPassengerXmlToRequestBean(CommandData input,Create  create )
			throws APIException {
			//证件类型
			String idtype = "" ;
			//证件号
			String idno = "" ;
			//联系方式类型
			String type = "" ;
			//手机号
			String mobile = "" ;
			//邮箱号
			String email = "" ;
			//性别
			String gender ="";
			//区号
			String area="";
			//签发国
			String issueCountry ="";
			//有效期
			String expiryDate = "";
			String nationality = "";
			SimpleDateFormat adf = new SimpleDateFormat("yyyy-MM-dd");
			LCCPassengerInfo[] passengerInfo = create.getLCCUserInfo().getLCCPassengerInfoArray();
				if(null != passengerInfo && passengerInfo.length>0){
					//出生日期
					Date birthday =null;
					if (passengerInfo[0]!=null&&passengerInfo[0].getAge()!=null&&passengerInfo[0].getAge().getBirthDate()!=null) {
						birthday=passengerInfo[0].getAge().getBirthDate().getDateValue();
						input.addParm("birthday", adf.format(birthday));
					}
					getPersonName(input,passengerInfo);
					//性别
					 gender = passengerInfo[0].getGender() ;
					 nationality = passengerInfo[0].getCitizenshipCountryCode();
					 //只留姓，名，证件号，证件类型的非空校验
//					 if(!StringUtils.hasLength(gender)){
//						 LOGGER.info(TipMessager.getInfoMessage(
//									ErrCodeConstants.API_NULL_GENDER, 
//									ApiServletHolder.getApiContext().getLanguage()));
//						 throw APIException.getInstance(ErrCodeConstants.API_NULL_GENDER);
//					 }	
					org.iata.iata.edist.LCCUserType.LCCPassengerInfo.FOID[] foid = 
							passengerInfo[0].getFOIDArray();
					if(null != foid && foid.length>0){
						idtype = foid[0].getType();
						 if(!StringUtils.hasLength(idtype)){
							 LOGGER.info(TipMessager.getInfoMessage(
										ErrCodeConstants.API_NULL_IDTYPE, 
										ApiServletHolder.getApiContext().getLanguage()));
							 throw APIException.getInstance(
									 ErrCodeConstants.API_NULL_IDTYPE);
						 }
						 idno = foid[0].getID().getStringValue();
						 if(!StringUtils.hasLength(idno)){
							 LOGGER.info(TipMessager.getInfoMessage(
										ErrCodeConstants.API_NULL_IDNO, 
										ApiServletHolder.getApiContext().getLanguage()));
							 throw APIException.getInstance(ErrCodeConstants.API_NULL_IDNO);
						 }	
						 if(idtype.equals("PP")){
							 expiryDate = foid[0].getExpiryDate();
							 issueCountry = foid[0].getIssueCountry();
						 }
					}else{
						LOGGER.info(TipMessager.getInfoMessage(
								ErrCodeConstants.API_NULL_IDINFO, 
								ApiServletHolder.getApiContext().getLanguage()));
						 throw APIException.getInstance(ErrCodeConstants.API_NULL_IDINFO);
					}
					org.iata.iata.edist.LCCUserType.LCCPassengerInfo.
						ContactInfo[] contactInfo = passengerInfo[0].
							getContactInfoArray();
					 if(null != contactInfo && contactInfo.length>0){
						for (int i = 0; i < contactInfo.length; i++) {
							 type = contactInfo[i].getType();
							 if ("email".equals(type)) {
								email=contactInfo[i].getNumber();
							}else {
								mobile = contactInfo[i].getNumber();
								//区号
								area= contactInfo[i].getAreaCode();
							}
							
						}
//						 if(!StringUtils.hasLength(type)){
//							 LOGGER.info(TipMessager.getInfoMessage(
//										ErrCodeConstants.API_NULL_CONTACT_TYPE, 
//										ApiServletHolder.getApiContext().getLanguage()));
//							 throw APIException.getInstance(
//									 ErrCodeConstants.API_NULL_CONTACT_TYPE);
//						 }	
//						if(!StringUtils.hasLength(mobile)){
//							LOGGER.info(TipMessager.getInfoMessage(
//									ErrCodeConstants.API_NULL_CONTACT_NO, 
//									ApiServletHolder.getApiContext().getLanguage()));
//							 throw APIException.getInstance(
//									ErrCodeConstants.API_NULL_CONTACT_NO);
//						 }	
					 }else{
						 LOGGER.info(TipMessager.getInfoMessage(
									ErrCodeConstants.API_NULL_CONTACT, 
									ApiServletHolder.getApiContext().getLanguage()));
						 throw APIException.getInstance(
								 ErrCodeConstants.API_NULL_CONTACT);
					 }
				}else{
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_PAXS, 
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS);
				}
			input.addParm("idtype", idtype);
			input.addParm("idno", idno);
			input.addParm("type", type);
			input.addParm("mobile", mobile);
			input.addParm("email", email);
			input.addParm("gender", gender);
			input.addParm("area", area);
			input.addParm("expiryDate", expiryDate);
			input.addParm("issueCountry", issueCountry);
			input.addParm("nationality", nationality);
		}
		
		/**
		 *  姓名
		 * @param input XML
		 * @param passengerInfo  节点
		 */
		private static void getPersonName(CommandData input,LCCPassengerInfo[] passengerInfo)
			throws APIException {
			//姓
			String lastname = "" ; 
			//名
			String firstname = "" ;
			PersonName[]  personName =	passengerInfo[0].getPersonNameArray();
			if(null != personName && personName.length>0){
				//姓
				lastname = personName[0].getSurname();
				//名
				firstname = personName[0].getGivenName();
				//判断姓属否为空
				if(!StringUtils.hasLength(lastname)){
					 LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_SURNAME, 
							ApiServletHolder.getApiContext().getLanguage()));
					 throw APIException.getInstance(ErrCodeConstants.API_NULL_SURNAME);
				 }
				//判断名属否为空
				 if(!StringUtils.hasLength(firstname)){
					 LOGGER.info(TipMessager.getInfoMessage(
								ErrCodeConstants.API_NULL_GIVENNAME, 
								ApiServletHolder.getApiContext().getLanguage()));
					 throw APIException.getInstance(ErrCodeConstants.API_NULL_GIVENNAME);
				 }	
			 }else{
				 LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_PERSONNAME, 
							ApiServletHolder.getApiContext().getLanguage()));
				 throw APIException.getInstance(ErrCodeConstants. API_NULL_PERSONNAME);
			 }
			input.addParm("lastname", lastname);
			input.addParm("firstname", firstname);
		}
}
