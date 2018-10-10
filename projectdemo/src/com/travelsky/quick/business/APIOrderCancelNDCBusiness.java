package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.OrderCancelRQDocument;
import org.iata.iata.edist.OrderCancelRSDocument;
import org.iata.iata.edist.OrderCancelRSDocument.OrderCancelRS;
import org.iata.iata.edist.OrderCancelRSDocument.OrderCancelRS.Response;
import org.iata.iata.edist.OrderIDType;
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
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * @author 作者:LiHz
 * @version 0.1 类说明: 取消订单接口
 *
 */
@Service("LCC_ORDERCANCEL_SERVICE")
public class APIOrderCancelNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(APIOrderCancelNDCBusiness.class);

	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 获取xml
		// 转换 xml-->Reqbean
		try {
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResponseBean());
		}
		// 请求 xm转换CommandData 异常
		catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_ORDER_CANCEL, ApiServletHolder
							.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	// 将CommadRet 转为 xmlbean
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		// 转换ResponseBean-->XmlBean
		return transRespBeanToXmlBean(commandRet, input);
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
		OrderCancelRQDocument rootDoc = null;
		rootDoc = OrderCancelRQDocument.Factory.parse(xmlInput);

		OrderCancelRQDocument.OrderCancelRQ reqdoc = rootDoc.getOrderCancelRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid", deptno);
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		// 订单id
		String orderNo = "";
		OrderIDType[] orderIDType = reqdoc.getQuery().getOrderIDArray();
		if (null != orderIDType && orderIDType.length > 0) {
			orderNo = orderIDType[0].getStringValue();
		} else {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_NO, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
		}
		input.addParm("memberid", context.getContext().getUserID());
		input.addParm("orderno", orderNo);
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
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.cancel(input, context);
	}

	/**
	 * 拼接B2C所用的XML
	 * 
	 * @param commandRet
	 *            后台返回的结果集
	 * @param input
	 *            B2C请求的XML
	 * @return 请求后台返回的对象
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet, CommandData input) {
		CommandRet xmlOutput = (CommandRet) commandRet;

		OrderCancelRSDocument doc = OrderCancelRSDocument.Factory.newInstance();
		OrderCancelRS rs = doc.addNewOrderCancelRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));			}
			// 反回无吴
			else {
				// 订单号
				String orderno = input.getParm("orderno").getStringColumn();
				rs.addNewDocument();
				rs.addNewSuccess();
				Response response = rs.addNewResponse();
//				// 暂时无用，但是NDC标准要求必须有，请忽略
//				response.addNewOrderCancelProcessing();
				response.setOrderReference(orderno);
			}
		} catch (Exception e) {
			// 初始化XML节点
			doc = OrderCancelRSDocument.Factory.newInstance();
			rs = doc.addNewOrderCancelRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(
					ErrCodeConstants.API_SYSTEM, ApiServletHolder
							.getApiContext().getLanguage()));
		}
		return doc;

	}

}
