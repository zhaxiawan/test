package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCUserModifyRQDocument;
import org.iata.iata.edist.LCCUserModifyRQDocument.LCCUserModifyRQ.Modify;
import org.iata.iata.edist.LCCUserModifyRSDocument;
import org.iata.iata.edist.LCCUserModifyRSDocument.LCCUserModifyRS;
import org.iata.iata.edist.LCCUserType.LCCPassengerInfo;
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
 * 会员信息的删除（常用登机人）
 * @author LiHongZhi
 * @version 0.1
 * 类型说明:
 *			DELETE_PASSENGER 常用登机人
 */
@Service("LCC_PASSENGERDELETE_SERVICE")
public class APIUserPassengerDeleteNDCBusiness extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1465398137293482828L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUserPassengerDeleteNDCBusiness.class);
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
						ErrCodeConstants.API_UNKNOW_USER_DELETE,
						ApiServletHolder.getApiContext().getLanguage()), e);
				throw e;
			}
	}
	
	/**
	 * 转换 xml-->Reqbean
	 * @param context shopping所用的一个集合
	 * @param xmlInput 前台获取的xml数据
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCUserModifyRQDocument rootDoc = null;
		rootDoc = LCCUserModifyRQDocument.Factory.parse(xmlInput);

		LCCUserModifyRQDocument.LCCUserModifyRQ reqdoc = rootDoc.getLCCUserModifyRQ();
		
		String channelNo = context.getContext().getChannelNo();
		//渠道号
		input.addParm("channelno", channelNo);
		input.addParm("tktdeptid",context.getContext().getTicketDeptid());
		Modify modify = reqdoc.getModify();
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
		//保存删除信息的ID
		input.addParm("id", getID(modify));
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
		//会员信息删除_常用乘机人
		return memberManager.delPassenger(input, context);	
	}
	/**
	 * 转换ResponseBean-->XmlBean
	 * @param xmlOutput 后台返回的结果集
	 * @param input  B2C请求的XML
	 * @return  XML结果集
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet xmlOutput ,CommandData input){
		
		LCCUserModifyRSDocument doc = LCCUserModifyRSDocument.Factory.newInstance();
		LCCUserModifyRS rs = doc.addNewLCCUserModifyRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}else{ 
				//成功标识
				rs.addNewSuccess();
				
			}
		}
		catch (Exception e) {
			//初始化XML节点
			doc = LCCUserModifyRSDocument.Factory.newInstance();
			rs = doc.addNewLCCUserModifyRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}
	
	/**
	 * 获取删除信息的ID
	 * @param modify 节点
	 * @throws APIException APIException
	 * @return 删除信息的ID
	 */
	public static String getID(Modify modify)
		throws APIException {
		String id = "";
		//会员信息删除_常用乘机人
		LCCPassengerInfo[] passengerInfo = modify.getLCCUserInfo()
				.getLCCPassengerInfoArray();
		if(null != passengerInfo && passengerInfo.length>0){
			id =passengerInfo[0].getPassengerID(); 
			if(!StringUtils.hasLength(id)){ 
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_PASSENGER_ID, 
						ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_PASSENGER_ID);
			}
		}
		else{
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_PASSENGER_ID, 
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(
					ErrCodeConstants.API_NULL_PASSENGER_ID);
		}	
		return id;
	}
	
}
