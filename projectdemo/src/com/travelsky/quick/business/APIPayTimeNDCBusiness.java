package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCPayPreRQDocument;
import org.iata.iata.edist.LCCPayPreRSDocument;
import org.iata.iata.edist.LCCPayPreRSDocument.LCCPayPreRS.Response.PayPreInfo;
import org.iata.iata.edist.LCCPayPreRSDocument.LCCPayPreRS.Response.PayPreInfo.OrderInfo;
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
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 支付时限接口
 * 
 * @author LHzhi
 *
 */

@Service("LCC_PAYTIME_SERVICE")
public class APIPayTimeNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIPayPreNDCBusiness.class);
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
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
						ErrCodeConstants.API_UNKNOW_PAY_PRE, 
						ApiServletHolder.getApiContext().getLanguage()), e);
				throw e;
			}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		CommandRet xmlOutput = (CommandRet)commandRet;
		LCCPayPreRSDocument doc = LCCPayPreRSDocument.Factory.newInstance();
		LCCPayPreRSDocument.LCCPayPreRS rs = doc.addNewLCCPayPreRS();
		try{
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}
			 //反回正确的值
			else{   
				rs.addNewSuccess();
				PayPreInfo payPreInfo = rs.addNewResponse().addNewPayPreInfo();
				OrderInfo orderInfo = payPreInfo.addNewOrderInfo();
				String servertime = xmlOutput.getParm("servertime").getStringColumn();
				orderInfo.setDate(servertime);
				String paytime = xmlOutput.getParm("paytime").getStringColumn();
				orderInfo.setTime(paytime);
			}
		} 
		catch (Exception e) {
			//初始化XML节点
			doc = LCCPayPreRSDocument.Factory.newInstance();
			rs = doc.addNewLCCPayPreRS();
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
	 * 
	 * @param context SelvetContext
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCPayPreRQDocument rootDoc = null;
		rootDoc = LCCPayPreRQDocument.Factory.parse(xmlInput);
		// 部门ID
//		String deptno = NdcXmlHelper.getDeptNo(reqdoc.getParty());
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		input.addParm("memberid", context.getContext().getUserID());
		//MOBAPP手机APP渠道   MOBWEB手机web渠道
		String appid = ApiServletHolder.getApiContext().getChannelNo();
		input.addParm("code",appid);
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
		ShoppingManager lManager = new ShoppingManager();
		return lManager.payTime(input, context);
	}
	
	
}
