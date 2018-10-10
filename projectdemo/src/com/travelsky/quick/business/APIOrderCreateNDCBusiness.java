package com.travelsky.quick.business;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.OrderCreateRQDocument;
import org.iata.iata.edist.OrderCreateRQDocument.OrderCreateRQ.Query.OrderItems;
import org.iata.iata.edist.OrderViewRSDocument;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS;
import org.iata.iata.edist.ShoppingResponseOrderType.Offers.Offer;
import org.iata.iata.edist.ShoppingResponseOrderType.Offers.Offer.OfferItems.OfferItem.Details;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.APIAirShoppingNDCB2C;
import com.travelsky.quick.util.helper.APIAirShoppingNDCONEE;
import com.travelsky.quick.util.helper.APIOrderCreateNDCB2C;
import com.travelsky.quick.util.helper.APIOrderCreateNDCDA;
import com.travelsky.quick.util.helper.APIOrderCreateNDCONEE;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:订单创建接口
 * 
 * @author huxizhun
 *
 */
@Service("LCC_ORDERCREATE_SERVICE")
public class APIOrderCreateNDCBusiness extends AbstractService<ApiContext> {

	private static final Logger LOGGER = LoggerFactory.getLogger(APIOrderCreateNDCBusiness.class);

	/**
	 * 在多线程时，此类本身是单例的，如果全局的数据是变量的话，第一个线程将A变量赋值成1；
	 * 第二个线程在第一个线程获取A的值之前将A变量赋值成2；
	 * 那么第一个线程获取A的值就是第二个线程中A的值。
	 */
	private static final long serialVersionUID = -6558548297387411743L;
	private static final String B2C = "B2C";
	private static final String TYPEA = "TYPEA";
	private static final String TYPEB = "TYPEB";
	// 用于存放APIOrderCreateNDCONEE对象，如果该对象是全局数据的话，在多线程访问该单例接口时，会出现此对象中的数据错乱。
	private Map<String, APIOrderCreateNDCONEE> orderCreateONEEMap = new HashMap<String, APIOrderCreateNDCONEE>();

	// 获取ResponseBean

	protected void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderCreateRQDocument rootDoc = OrderCreateRQDocument.Factory.parse(xmlInput);
		OrderCreateRQDocument.OrderCreateRQ reqDoc = rootDoc.getOrderCreateRQ();
		OrderItems orderItemsArry = reqDoc.getQuery().getOrderItems();
		//报错信息，返回
		Table errorTable=new Table(new String[]{"errorcode","errordesc"});
		String agree;
		CommandRet ret;
		if (null == orderItemsArry) {
			agree = TYPEB;
			input.addParm("agree", agree);
			APIOrderCreateNDCDA orderCreateDA = new APIOrderCreateNDCDA();
			orderCreateDA.doServletDA(context);
			OrderOpManager orderOpManager = new OrderOpManager();
			// 创建订单
			ret = new CommandRet("");
			ret = orderOpManager.createorderONEE(input, context);
			if (!ret.isError() && !"1".equals(ret.getParm("flag").getStringColumn())) {
				// 追加 辅营
				Table spaxsubmarkets = input.getParm("spaxsubmarkets").getTableColumn();	
				if (null != spaxsubmarkets && spaxsubmarkets.getRowCount() > 0 ) {
					ret = orderOpManager.esubmarket(input, context);
					//辅营失败，返回值有显示
					if (ret.isError()) {
						Row row = errorTable.addRow();
						row.addColumn("errorcode", ret.getErrorCode());
						row.addColumn("errordesc", ret.getErrorDesc());
					}
				}
				ret.setError("", "");
				Table seats = input.getParm("seats").getTableColumn();
				if (null != seats && seats.getRowCount() > 0) {
					// 预定座位
					ret = orderOpManager.eseat(input, context);
					//座位失败，返回值有显示
					if (ret.isError()) {
						Row row = errorTable.addRow();
						row.addColumn("errorcode", ret.getErrorCode());
						row.addColumn("errordesc", ret.getErrorDesc());
					}
				}
				ret.setError("", "");
			}
			// 订单明细
			if (!ret.isError()) {
				context.getInput().addParm("orderno", ret.getParm("orderno").getStringColumn());
				ret = orderOpManager.orderDetail(context.getInput(), context);
			}
			ret.addParm("errorTable", errorTable);
			context.setRet(ret);
		} else {
			Offer[] offerArry = orderItemsArry.getShoppingResponse().getOffers().getOfferArray();
			Details details = offerArry[0].getOfferItems().getOfferItemArray(0).getDetails();
			if (null == details) {
				agree = B2C;
				input.addParm("agree", agree);
				ret = new CommandRet("");
				APIOrderCreateNDCB2C orderCreateB2C = new APIOrderCreateNDCB2C();
				orderCreateB2C.doServletB2C(context);
				OrderOpManager orderOpManager = new OrderOpManager();
				ret = orderOpManager.createorder(context.getInput(), context);
				if (!ret.isError()) {
					//订单创建成功后，需要清空redis中的shopping缓存
//					String delRedis = context.getInput().getParm("delRedis").getStringColumn();
//					if(StringUtils.hasLength(delRedis) && delRedis.contains(",")){
//						String[] delKey = delRedis.split(",");
//						for (String key : delKey) {
//							RedisManager.getManager().del(key);
//						}
//					}else if (StringUtils.hasLength(delRedis)&& !delRedis.contains(",")) {
//						RedisManager.getManager().del(delRedis);
//					}
//					//订单创建成功后，需要清空redis中的shoppingxml数据缓存，下次shopping才能继续缓存航班数据
//					String shoppingreidskey = RedisManager.getManager().get("shoppingreidskey");
//					if (StringUtils.hasLength(shoppingreidskey)) {
//						RedisManager.getManager().del(shoppingreidskey);
//						RedisManager.getManager().del(shoppingreidskey+"_json");
//					}
					context.getInput().addParm("orderno", ret.getParm("orderno").getStringColumn());
					ret = new CommandRet("");
					ret = orderOpManager.detail(context.getInput(), context);
				}
				context.setRet(ret);
			} else {
				agree = TYPEA;
				input.addParm("agree", agree);
				ret = new CommandRet("");
				APIOrderCreateNDCONEE orderCreateONEE = new APIOrderCreateNDCONEE();
				orderCreateONEE.doServletONEE(context);
				orderCreateONEEMap.put("orderCreateONEE" + context.getTransactions(), orderCreateONEE);
				OrderOpManager orderOpManager = new OrderOpManager();
				ret = orderOpManager.createorderONEE(input, context);
				context.getInput().addParm("pnr", input.getParm("pnr").getStringColumn());
				if (!ret.isError() && !"1".equals(ret.getParm("flag").getStringColumn())) {
					//订单创建成功后，需要清空redis中的shopping缓存
					String delRedis = context.getInput().getParm("delRedis").getStringColumn();
					if(StringUtils.hasLength(delRedis) && delRedis.contains(",")){
						String[] delKey = delRedis.split(",");
						for (String key : delKey) {
							RedisManager.getManager().del(key);
						}
					}
					Table spaxsubmarkets = input.getParm("spaxsubmarkets").getTableColumn();
					if (null != spaxsubmarkets && spaxsubmarkets.getRowCount() > 0) {
						// 辅营
						ret = orderOpManager.esubmarket(input, context);
						//辅营失败，返回值有显示
						if (ret.isError()) {
							Row row = errorTable.addRow();
							row.addColumn("errorcode", ret.getErrorCode());
							row.addColumn("errordesc", ret.getErrorDesc());
						}
					}
					ret.setError("", "");
					Table seats = input.getParm("seats").getTableColumn();
					if (null != seats && seats.getRowCount() > 0) {
						// 座位预定
						ret = orderOpManager.eseat(input, context);
						//座位失败，返回值有显示
						if (ret.isError()) {
							Row row = errorTable.addRow();
							row.addColumn("errorcode", ret.getErrorCode());
							row.addColumn("errordesc", ret.getErrorDesc());
						}
					}
					ret.setError("", "");
				}
				// 订单明细
				if (!ret.isError()) {
					context.getInput().addParm("orderno", ret.getParm("orderno").getStringColumn());
					ret = orderOpManager.orderDetail(context.getInput(), context);
				}
				ret.addParm("errorTable", errorTable);
				context.setRet(ret);
			}
		}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		String agree = input.getParm("agree").getStringColumn();
		if (B2C.equals(agree)) {
			APIOrderDetailNDCBusiness orderDetailNDC = new APIOrderDetailNDCBusiness();
			return orderDetailNDC.transResponseBeanToXmlBean(commandRet, input);
		}
		if (TYPEB.equals(agree)) {
			APIOrderCreateNDCONEE orderCreateONEE = new APIOrderCreateNDCONEE();
			return orderCreateONEE.transResponseBeanToXmlBean(commandRet, input);
		}
		if (TYPEA.equals(agree)) {
			String orderCreateONEEKey = "orderCreateONEE" + ApiServletHolder.get().getTransactions();
			  APIOrderCreateNDCONEE orderCreateONEE = orderCreateONEEMap.get(orderCreateONEEKey);
			if (orderCreateONEEMap != null) {
				orderCreateONEEMap.remove(orderCreateONEE);
			}
			return orderCreateONEE.transResponseBeanToXmlBean(commandRet, input);
		}
		OrderViewRSDocument doc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRSDocument.OrderViewRS root = doc.addNewOrderViewRS();
		doc = OrderViewRSDocument.Factory.newInstance();
		root = doc.addNewOrderViewRS();
		commandRet.setError(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM),
				TipMessager.getMessage(ErrCodeConstants.API_SYSTEM, ApiServletHolder.getApiContext().getLanguage()));
		processError(commandRet, root);
		return doc;
	}

	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * 
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
			error.setCode(
					TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}
}
