package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.OrderRetrieveRQDocument;
import org.iata.iata.edist.OrderViewRSDocument;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response;
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
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 订单详情信息接口
 * 
 * @author ZHANGWENLONG 2017年7月31日
 * @version 0.1
 */
@Service("LCC_PAYDETAIL_SERVICE")
public class APIPayDetailNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 订单详情
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIPayDetailNDCBusiness.class);

	/**
	 * @param context
	 *            SelvetContext<ApiContext>
	 * @throws Exception
	 *             Exception
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 获取xml
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_BUILD_PAYINFO_ERROR,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}

	}

	/**
	 * 
	 * @param input
	 *            CommandData
	 * @param context
	 *            SelvetContext
	 * @return CommandRet
	 */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.payDetail(input, context);
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
		OrderRetrieveRQDocument rootDoc = OrderRetrieveRQDocument.Factory
				.parse(xmlInput);

		OrderRetrieveRQDocument.OrderRetrieveRQ reqDoc = rootDoc
				.getOrderRetrieveRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		// 获取语言 
		String language = ApiServletHolder.getApiContext().getLanguage();
		String orderID = reqDoc.getQuery().getFilters().getOrderID()
				.getStringValue();
		if (!StringUtils.hasLength(orderID)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_ID, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_ID);
		}
		// 订单号
		input.addParm("orderid", orderID);
	}
	
	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @return XmlObject
	 */
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		OrderViewRSDocument doc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRSDocument.OrderViewRS root = doc.addNewOrderViewRS();
		try {
			if (processError(commandRet, root)) {
				return doc;
			}
			String status = commandRet.getParm("payresult").getStringColumn();
			root.addNewSuccess();
			Response response = root.addNewResponse();
			response.addNewOrder().addNewStatus().addNewStatusCode().setCode(status);
		}
		catch (Exception e) {
			LOGGER.error(ErrCodeConstants.API_BUILD_PAYINFO_ERROR, e);
			doc = OrderViewRSDocument.Factory.newInstance();
			root = doc.addNewOrderViewRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM,
					TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
							ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}
		return doc;
	}
	
	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * @param ret
	 * @param root
	 * @return
	 */
	private boolean processError(CommandRet ret, OrderViewRS root) {
		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setShortText(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}
}
