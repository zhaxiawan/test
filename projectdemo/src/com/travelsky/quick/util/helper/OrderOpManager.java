package com.travelsky.quick.util.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.TimeZoneUtil;
import com.cares.sh.comm.Unit;
import com.cares.sh.comm.Util;
import com.cares.sh.constant.Constant;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.RedisUtil;
import com.travelsky.quick.util.StatusUtil;

public class OrderOpManager {
	private static Logger logger = LoggerFactory.getLogger(OrderOpManager.class);
	String language = ApiServletHolder.getApiContext().getLanguage();
	public static final String SERVICE = "SERVICE";
	public static final String LOCK = "LOCK";
	public static final String TWO_THOUSAND_THIRTEEN = "2013";
	public static final String TWO_THOUSAND_SIXTY_ONE = "2061";
	/**
	 * 1e
	 * @param input 1e增删人航段
	 * @param context
	 * @return
	 */
	public CommandRet ePaxflt(CommandData input, SelvetContext<ApiContext> context) {
		CommandInput lInput = new CommandInput("com.cares.sh.order.update.epaxflt");
		CommandRet lRet =new CommandRet("");
		lInput.addParm("flag", input.getParm("flag").getStringColumn());
		lInput.addParm("pnr", input.getParm("pnr").getStringColumn());
		lInput.addParm("paxflts", input.getParm("paxflts").getTableColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(),lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}
	/**
	 * 追加特殊服务+餐食等
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet esubmarket(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.essr");
		CommandRet lRet =new CommandRet("");
		lInput.addParm("flag", input.getParm("flag").getStringColumn());
		lInput.addParm("pnr", input.getParm("pnr").getStringColumn());
		Table tab = input.getParm("spaxsubmarkets").getTableColumn();
		lInput.addParm("tab" ,tab );
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			//针对追加辅营座位失败时，需要在订单详情中显示
			CommandData data=new CommandData();
			data.addParm("tab", tab);
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("tab"),JsonUnit.toJson(data) ,60);
			lRet.setError(lRet.getErrorCode(),lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}
	/**
	 * 座位操作  
	 */
	public CommandRet eseat(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.eseat");
		CommandRet lRet =new CommandRet("");
		lInput.addParm("flag", input.getParm("flag").getStringColumn());
		lInput.addParm("pnr", input.getParm("pnr").getStringColumn());
		Table seatsTable = input.getParm("seats").getTableColumn();
		lInput.addParm("seats", seatsTable);
		//input.copyTo(lInput);
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			//针对追加辅营座位失败时，需要在订单详情中显示
			CommandData data=new CommandData();
			data.addParm("seats", seatsTable);
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("seats"),JsonUnit.toJson(data) ,60);
			lRet.setError(lRet.getErrorCode(),lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}
	/**
	 * 修改旅客信息
	 */
	public CommandRet updateie(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.update.ie");
		CommandRet lRet =new CommandRet("");
		lInput.addParm("flag", input.getParm("flag").getStringColumn());
		lInput.addParm("pnr", input.getParm("pnr").getStringColumn());
		lInput.addParm("paxs", input.getParm("paxs").getTableColumn());
		//input.copyTo(lInput);
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(),lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}

	/**
	 * 释放invReleaseNotif
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet invReleaseNotif(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.guanrantee.cancel");
		CommandRet lRet =new CommandRet("");
		input.copyTo(lInput);
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}
	
	/**
	 * 订单创建ONEE接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet createorderONEE(CommandData input,SelvetContext<ApiContext> context){
		String guanranteeno = context.getInput().getParm("guanranteeno").getStringColumn();
		String commde ="";
		if("".equals(guanranteeno)){
			commde ="com.cares.sh.order.order.jdscreate";
		}else{
			commde ="com.cares.sh.order.create.forguanrantee";
		}
		CommandInput lInput = new CommandInput(commde);
		input.copyTo(lInput); 
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.toOrderCreate(input, lInput);//lValidate.checkOrderCreate(input, lInput)
		//返回参数
		CommandRet lRet = new CommandRet("");
		if (!info.isEmpty()) {
			lRet.setError(info,TipMessager.getMessage(info, language));
			return lRet;
		}
		//保险信息
		Table	guanrantee = input.getParm("guanrantee").getTableColumn();
		lInput.addParm("guanrantee", guanrantee);
		// create order
		lInput.addParm("pnr", context.getInput().getParm("pnr").getStringColumn());
		lInput.addParm("channelid", context.getContext().getChannelNo());
	//	lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lInput.addParm("isoCode", input.getParm("isoCode").getStringColumn());
		
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			//将ibe错误信息封装成通用错误提示
			if(TWO_THOUSAND_THIRTEEN.equals(lRet.getErrorCode())
					|| TWO_THOUSAND_SIXTY_ONE.equals(lRet.getErrorCode())){
				lRet.setError(lRet.getErrorCode(), TipMessager.getMessage(
						ErrCodeConstants.API_ORDER_CREATED_ERROR, language));
			}
			return lRet;
		}
		/*CommandRet result = new CommandRet("");
		result.addParm("orderno", lRet.getParm("orderno").getStringColumn());
		result.addParm("pnr", lRet.getParm("pnr").getStringColumn());*/
		lRet.addParm("owner", context.getInput().getParm("owner").getStringColumn());
		return lRet;
	}
	
	/**
	 * 42.  订单创建接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet createorder(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.create");
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkOrderCreate(input, lInput);
		//返回参数
		CommandRet lRet = new CommandRet("");
		if (!info.isEmpty()) {
			lRet.setError(info,TipMessager.getMessage(info, language));
			return lRet;
		}
		//保险信息
		Table	submarkets = input.getParm("submarkets").getTableColumn();
		lInput.addParm("submarkets", submarkets);
		// create order
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lInput.addParm("isoCode", input.getParm("isoCode").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			//将ibe错误信息封装成通用错误提示
			if(TWO_THOUSAND_THIRTEEN.equals(lRet.getErrorCode())
					|| TWO_THOUSAND_SIXTY_ONE.equals(lRet.getErrorCode())){
				lRet.setError(lRet.getErrorCode(), TipMessager.getMessage(
						ErrCodeConstants.API_ORDER_CREATED_ERROR, language));
			}
			return lRet;
		}
		if (lRet.getParm("orderno").getStringColumn().isEmpty()) {
			lRet.setError(ErrCodeConstants.API_CREATED_ORDER_FAILED,
					TipMessager.getMessage(ErrCodeConstants.API_CREATED_ORDER_FAILED,
									language));
			return lRet;
		}
		CommandRet result = new CommandRet("");
		result.addParm("orderno", lRet.getParm("orderno").getStringColumn());
		result.addParm("pnr", lRet.getParm("pnr").getStringColumn());
		return result;
	}

	/**
	 * 43.  订单提交辅营接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet setsubmarket(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.setsubmarket");
		CommandRet lRet =new CommandRet("");
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		/*String memeberId = ApiServletHolder.getApiContext().getUserID();
		input.addParm("memberid", memeberId);*/
		String info = lValidate.checkSetsubmarket(input,lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info, language));
			return lRet;
		}
		// set submarket
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(),lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		CommandRet result = new CommandRet("");
		//获取订单详情
		result = detail(input, context);
		
		return result;
	}

	/**
	 * 44.  订单提交座位接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet setseat(CommandData input,SelvetContext<ApiContext> context) {
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.setseat");
		CommandRet lRet = new CommandRet("");

		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkSetseat(input,lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info,language));
			return lRet;
		}
		// set seat
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		CommandRet result = new CommandRet("");
		//获取订单详情
		result = detail(input, context);
		
		return result;
	}

	/**
	 * 45.  订单提交接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet submit(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.submit");
		CommandRet lRet = new CommandRet("");

		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkSubmit(input,lInput);
		if (!info.isEmpty()) {
			lRet.setError(info,TipMessager.getMessage(info, language));
			return lRet;
		}

		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}

		CommandRet result = new CommandRet("");
		//获取订单详情
		result = detail(input, context);
		return result;
	}

	/**
	 * 46.订单支付接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet orderpay(CommandData input,SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		CommandData ipt = context.getInput();
		if (ipt == null) {
			ret.setError(ErrCodeConstants.API_ORDERPAY_PARAM_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORDERPAY_PARAM_ERROR, language));
			return ret;
		}

		// 订单号
		String orderNo = ipt.getParm("orderno").getStringColumn();
		if (!StringUtils.hasLength(orderNo)) {
			ret.setError(ErrCodeConstants.API_NULL_ORDER_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_NO, language));
			return ret;
		}

		// 银行id
		String bankId = ipt.getParm("bankid").getStringColumn();
		if (!StringUtils.hasLength(bankId)) {
			ret.setError(ErrCodeConstants.API_NULL_BANK_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_BANK_ID, language));
			return ret;
		}

		// 获取支付信息
		CommandInput input0 = new CommandInput("com.travelsky.quick.easypay.buildPayInfo");
		input0.addParm("orderno", orderNo);
		input0.addParm("bankid", bankId);
		input0.addParm("rtnidkey", ipt.getParm("rtnidkey").getStringColumn());

		ret = context.doOther(input0,false);
		if (ret.isError()) {
			ret.setError(ret.getErrorCode(), ret.getErrorDesc());
			return ret;
		}
		logger.debug("支付信息:{}", JsonUnit.toJson(ret));

		//对B2C访问次数进行计数（XML请求/JSON请求）
//		String type=LOCK;
//		RedisUtil redisUtil =new RedisUtil();
//		redisUtil.docount(type, context);

		// 只获取需要的结果
		CommandRet result = new CommandRet("");
		CommandData data = ret.getParm("pay").getObjectColumn();
		if (data == null) {
			ret.setError(ErrCodeConstants.API_BUILD_PAYINFO_ERROR, ret.getErrorDesc());
			return ret;
		}
		for (String key : data.getItemList()) {
			result.addParm(key, data.getParm(key));
		}

		return result;
	}
	
	/**
	 * 46.订单支付接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet edyenpay(CommandData input,SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		CommandData ipt = context.getInput();
		// 订单号
		String orderNo = ipt.getParm("orderno").getStringColumn();
		if (!StringUtils.hasLength(orderNo)) {
			ret.setError(ErrCodeConstants.API_NULL_ORDER_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_NO, language));
			return ret;
		}
		// 获取订单详情
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.travelsky.quick.order.payresult");
		lInput.addParm("orderid", orderNo);
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			ret.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return ret;
		}
		String transaction_id = lRet.getParm("transaction_id").getStringColumn();

		CommandInput tInput = new CommandInput("com.travelsky.quick.order.dopay");
		tInput.addParm("transaction_id", transaction_id);
		tInput.addParm("payfun", "order");
		tInput.addParm("currency_code", input.getParm("code"));
		tInput.addParm("pay_type", "online");
		//支付网关(adyen、worldpay)
		tInput.addParm("pay_gateway", input.getParm("pay_gateway").getStringColumn());
		//支付方式（5中支付方式）
		tInput.addParm("pay_method", input.getParm("pay_method").getStringColumn());
		tInput.addParm("channel_no", "B2C");
		tInput.addParm("total_amount", input.getParm("money"));
		tInput.addParm("user_id", context.getContext().getUserID());
		Table items = new Table(new String[] {"money_type","amount"});
		Row row = items.addRow();
		row.addColumn("money_type", "cash");
		row.addColumn("amount", input.getParm("money"));
		tInput.addParm("items", items);
		ret = context.doOther(tInput, false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (ret.getErrorCode().equals("")||ret.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		
	
		
/*		{
			transaction_id : orderid#bookid
			!payfun : order
			!currency_code : 币种三字母码
			!pay_type : online
			pay_gateway : adyen
			!pay_method : 
			!ref_code : 
			!total_amount : 总金额
			!user_id : 用户id
			!items:[
			{
				money_type : cash
				amount : 金额
				quantity : 
			}]
		}
		*/
				
		return ret;
	}

	/**
	 * 订单线下支付
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet offlinepay(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		if (lOrderno.isEmpty()) {
			lRet.setError(ErrCodeConstants.API_NULL_ORDER_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_NO, language));
			return lRet;
		}
		if (lMemberid.isEmpty()) {
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		//根据订单号获取以下参数
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.detail");
		lInput.addParm("orderno", lOrderno);
		lInput.addParm("memberid", lMemberid);
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		String lPaysucno = "";
		String lMoney = "";
		Table lTable = lRet.getParm(Constant.Order.ITEM_NAME_PAYS).getTableColumn();
		if (lTable != null && lTable.getRowCount() > 0) {
			for (int i = 0; i < lTable.getRowCount(); i++) {
				Row row = lTable.getRow(i);
				if (Constant.Order.PAY_STATUS_TOPAY.equals(row.getColumn("status").getStringColumn())) {
					lPaysucno = row.getColumn("id").getStringColumn();
					lMoney = row.getColumn("price").getStringColumn();
					break;
				}
			}
		}

		CommandInput tInput = new CommandInput("com.cares.sh.order.order.pay");
		tInput.addParm("orderno", lOrderno);
		tInput.addParm("memberid", lMemberid);
		tInput.addParm("money", lMoney);
		tInput.addParm("paytype", "money");
		tInput.addParm("paysucno", lPaysucno);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(tInput, false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if(lRet.isError()){
			lRet.addParm(ErrCodeConstants.API_ORDER_PAY_ERROR, lRet.getErrorCode());
			return lRet;
		}

		lRet = new CommandRet("");
		return lRet;
	}
	
	/**
	 * 订单线下支付
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet payABBAndTKT(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String orderno = input.getParm("orderno").getStringColumn();
		if (orderno.isEmpty()) {
			lRet.setError(ErrCodeConstants.API_NULL_ORDER_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_NO, language));
			return lRet;
		}
		CommandInput tInput = new CommandInput("com.travelsky.quick.order.directpay");
		input.copyTo(tInput);
 		lRet = context.doOther(tInput, false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode() == null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if(lRet.isError()){
			lRet.addParm(ErrCodeConstants.API_ORDER_PAY_ERROR, lRet.getErrorCode());
			return lRet;
		}
		return lRet;
	}

	/**
	 * 订单取消
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet cancel(CommandData input,SelvetContext<ApiContext> context) {
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.cancel");
		CommandRet lRet = new CommandRet("");
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		if(lOrderno.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_ORDER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_ID, language));
			return lRet;
		}
		if(lMemberid.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		lInput.addParm("orderno", lOrderno);
		lInput.addParm("memberid", lMemberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}

		CommandRet result = new CommandRet("");
		return result;
	}

	/**
	 * 51.  订单查询接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet query(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.query");
		CommandRet lRet = new CommandRet("");
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		Date lFlightstart = input.getParm("flightstart").getDateColumn();
		Date lFlightend = input.getParm("flightend").getDateColumn();
		String lCreatestart = input.getParm("createstart").getStringColumn();
		String lCreateend = input.getParm("createend").getStringColumn();
		String lStatus = input.getParm("status").getStringColumn();

		Date startTime = null;
		Date endTime = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if(lFlightstart != null){
			try {
				startTime = sdf.parse(sdf.format(lFlightstart));
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE, language));
				return lRet;
			}
		}
		if(lFlightend != null){
			try {
				endTime = sdf.parse(sdf.format(lFlightend));
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE, language));
				return lRet;
			}
		}
		if(startTime != null && endTime != null){
			if(startTime.compareTo(endTime) > 0){
				lRet.setError(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR, language));
				return lRet;
			}
		}
		if(!lStatus.isEmpty()){
			if(!lStatus.equals("A") && (lStatus.compareTo("1") < 0 || lStatus.compareTo("8") > 0 )){
				lRet.setError(ErrCodeConstants.API_ORDER_STATUS_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORDER_STATUS_ERROR, language));
				return lRet;
			}
		}
		if(lMemberid.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		//分页参数
		String lPageNum = input.getParm("pagenum").getStringColumn();
		String lPageSize = input.getParm("pagesize").getStringColumn();
		//当前页数
		lInput.addParm("pagenum", lPageNum);
		//每页显示个数
		lInput.addParm("pagesize", lPageSize);
		//总页数
		lInput.addParm("recordnum", input.getParm("recordnum").getStringColumn());
		lInput.addParm("orderby", input.getParm("orderby").getStringColumn());
		//asc=Y 代表正序. 其他或者不传 代表倒序
		lInput.addParm("asc", input.getParm("asc").getStringColumn());
		lInput.addParm("orderno", lOrderno);
		lInput.addParm("flightstart", lFlightstart);
		lInput.addParm("flightend", lFlightend);
		lInput.addParm("createstart", lCreatestart);
		lInput.addParm("createend", lCreateend);
		lInput.addParm("status", lStatus);
		lInput.addParm("channelid", input.getParm("channelid").getStringColumn());
		lInput.addParm("memberid", lMemberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		Table lOrders = lRet.getParm("order").getTableColumn();
		if(lOrders == null || lOrders.getRowCount() < 1){
			lRet.setError(ErrCodeConstants.API_ORDER_IS_NULL,
					TipMessager.getMessage(ErrCodeConstants.API_ORDER_IS_NULL, language));
			return lRet;
		}
		//TODO 只查出待确认,待支付,已支付,部分待支付，已取消,已退订，已使用的订单?
		CommandRet result = new CommandRet("");
		Table tOrder = new Table(new String[]{"orderid","createtime","ori","dest","routtype","totalprice","status","oriname","oricitycode","oricityname","oricountry","destname",
			"destcitycode","destcityname",
				"destcountry","flightdate","flightno","flightseg","flighttime","paxname","maxpaytime","maxpaysecond"});
		result.addParm("order", tOrder);
		result.addParm("recordnum", lRet.getParm("recordnum").getStringColumn());
		result.addParm("pagenum", lPageNum);
		result.addParm("pagesize", lPageSize);
		for(int i = 0 ; i < lOrders.getRowCount() ; i++){
			Row lRow = lOrders.getRow(i);
			Row row = tOrder.addRow();
			String orderid = lRow.getColumn("orderid").getStringColumn();
			String createtime = lRow.getColumn("createtime").getStringColumn();
			String flightseg = lRow.getColumn("flightseg").getStringColumn();
			String ori = flightseg.split("-")[0];
			String dest = flightseg.split("-")[1];
			String routtype = lRow.getColumn("routtype").getStringColumn();
			String totalprice = lRow.getColumn("totalprice").getStringColumn();
			String status = lRow.getColumn("status").getStringColumn();
			row.addColumn("orderid", orderid);
			row.addColumn("createtime", createtime);
			row.addColumn("ori", ori);
			row.addColumn("dest", dest);
			row.addColumn("routtype", routtype);
			row.addColumn("totalprice", totalprice);
			row.addColumn("flightdate", lRow.getColumn("flightdate").getStringColumn());
			row.addColumn("flighttime", lRow.getColumn("flighttime").getStringColumn());
			row.addColumn("status", status);
			row.addColumn("oriname", lRow.getColumn("oriname").getStringColumn());
			row.addColumn("oricitycode", lRow.getColumn("oricitycode").getStringColumn());
			row.addColumn("oricityname", lRow.getColumn("oricityname").getStringColumn());
			row.addColumn("oricountry", lRow.getColumn("oricountry").getStringColumn());
			row.addColumn("destname", lRow.getColumn("destname").getStringColumn());
			row.addColumn("destcitycode", lRow.getColumn("destcitycode").getStringColumn());
			row.addColumn("destcityname", lRow.getColumn("destcityname").getStringColumn());
			row.addColumn("destcountry", lRow.getColumn("destcountry").getStringColumn());
			row.addColumn("flightno", lRow.getColumn("flightno").getStringColumn());
			row.addColumn("flightseg", lRow.getColumn("flightseg").getStringColumn());
			row.addColumn("paxname", lRow.getColumn("paxname").getStringColumn());
			//支付时间倒计时（单位：秒）
			String maxpaysecond ="";
			//支付到达时间(格式yyyyMMdd HH:mm:ss)（API的服务器时间）
			String maxpaytime="";
			//如果是1待确认或者是3待支付状态的订单 去获取支付倒计时。
//			if("1".equals(status)||"3".equals(status)){
//				CommandInput paytimeInput = new CommandInput("com.travelsky.quick.order.order.liteDetail");
//				paytimeInput.addParm("orderid", orderid);
//				CommandRet paytimeRet = context.doOther(paytimeInput,false);
//				//判断是否出错
//				if (paytimeRet.isError()) {
//					lRet.setError(paytimeRet.getErrorCode(), paytimeRet.getErrorDesc());
//					return lRet;
//				}
//				Table lPays = paytimeRet.getParm("pays").getTableColumn();
//				if(lPays != null && lPays.getRowCount() > 0){
//					for(int j = 0 ; j < lPays.getRowCount() ; j++){
//						maxpaysecond = lPays.getRow(j).getColumn("maxpaysecond").getStringColumn();
//						maxpaytime = lPays.getRow(j).getColumn("maxpaytime").getStringColumn();
//					}
//				}
//			}
			row.addColumn("maxpaytime", maxpaytime);
			row.addColumn("maxpaysecond", maxpaysecond);
		}
		return result;
	}
	public CommandRet querytest(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.query.my.order.list");
		CommandRet lRet = new CommandRet("");
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		Date lFlightstart = input.getParm("flightstart").getDateColumn();
		Date lFlightend = input.getParm("flightend").getDateColumn();
		String lCreatestart = input.getParm("createstart").getStringColumn();
		String lCreateend = input.getParm("createend").getStringColumn();
		String lStatus = input.getParm("status").getStringColumn();

		Date startTime = null;
		Date endTime = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if(lFlightstart != null){
			try {
				startTime = sdf.parse(sdf.format(lFlightstart));
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE, language));
				return lRet;
			}
		}
		if(lFlightend != null){
			try {
				endTime = sdf.parse(sdf.format(lFlightend));
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE, language));
				return lRet;
			}
		}
		if(startTime != null && endTime != null){
			if(startTime.compareTo(endTime) > 0){
				lRet.setError(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR, language));
				return lRet;
			}
		}
		if(!lStatus.isEmpty()){
			if(!lStatus.equals("A") && (lStatus.compareTo("1") < 0 || lStatus.compareTo("8") > 0 )){
				lRet.setError(ErrCodeConstants.API_ORDER_STATUS_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORDER_STATUS_ERROR, language));
				return lRet;
			}
		}
		if(lMemberid.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		//分页参数
		String lPageNum = input.getParm("pagenum").getStringColumn();
		String lPageSize = input.getParm("pagesize").getStringColumn();
		//当前页数
		lInput.addParm("pagenum", lPageNum);
		//每页显示个数
		lInput.addParm("pagesize", lPageSize);
		//总页数
		lInput.addParm("recordnum", input.getParm("recordnum").getStringColumn());
		lInput.addParm("orderby", input.getParm("orderby").getStringColumn());
		//asc=Y 代表正序. 其他或者不传 代表倒序
		lInput.addParm("asc", input.getParm("asc").getStringColumn());
		lInput.addParm("orderno", lOrderno);
		lInput.addParm("flightstart", lFlightstart);
		lInput.addParm("flightend", lFlightend);
		lInput.addParm("createstart", lCreatestart);
		lInput.addParm("createend", lCreateend);
		lInput.addParm("status", lStatus);
		lInput.addParm("channelid", input.getParm("channelid").getStringColumn());
		lInput.addParm("memberid", lMemberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
		CommandRet result = new CommandRet("");
		Table tOrder = new Table(new String[]{"orderid","createtime","ori","dest","routtype","totalprice","status","oriname","oricitycode","oricityname","oricountry","destname",
				"destcitycode","destcityname",
				"destcountry","flightdate","flightno","flightseg","flighttime","paxname","maxpaytime","maxpaysecond"});
		result.addParm("order", tOrder);
		result.addParm("recordnum", lRet.getParm("recordnum").getStringColumn());
		result.addParm("pagenum", lPageNum);
		result.addParm("pagesize", lPageSize);
		return result;
	}
	/**
	 * 51.  订单查询接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet queryforNDC(CommandData input,SelvetContext<ApiContext> context){
		CommandInput lInput = new CommandInput("com.cares.sh.order.query.my.order.list");
		CommandRet lRet = new CommandRet("");
		String lMemberid = input.getParm("memberid").getStringColumn();
		Date lFlightstart = input.getParm("flightstart").getDateColumn();
		Date lFlightend = input.getParm("flightend").getDateColumn();
		String lCreatestart = input.getParm("createstart").getStringColumn();
		String lCreateend = input.getParm("createend").getStringColumn();
		String lStatus = input.getParm("status").getStringColumn();
		Date startTime = null;
		Date endTime = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if(lFlightstart != null){
			try {
				startTime = sdf.parse(sdf.format(lFlightstart));
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE, language));
				return lRet;
			}
		}
		if(lFlightend != null){
			try {
				endTime = sdf.parse(sdf.format(lFlightend));
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE, language));
				return lRet;
			}
		}
		if(startTime != null && endTime != null){
			if(startTime.compareTo(endTime) > 0){
				lRet.setError(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR, language));
				return lRet;
			}
		}
		if(!lStatus.isEmpty()){
			if(!lStatus.equals("A") && (lStatus.compareTo("1") < 0 || lStatus.compareTo("8") > 0 )){
				lRet.setError(ErrCodeConstants.API_ORDER_STATUS_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORDER_STATUS_ERROR, language));
				return lRet;
			}
		}
		if(lMemberid.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		//分页参数
		String lPageNum = input.getParm("pagenum").getStringColumn();
		String lPageSize = input.getParm("pagesize").getStringColumn();
		//当前页数
		lInput.addParm("pagenum", lPageNum);
		//每页显示个数
		lInput.addParm("pagesize", lPageSize);
		lInput.addParm("psort", input.getParm("psort").getStringColumn());
		//pasc 1为正序,0为倒序.为空正序.前提是传了psort
		lInput.addParm("pasc", input.getParm("pasc").getStringColumn());
		lInput.addParm("flightstart", lFlightstart);
		lInput.addParm("flightend", lFlightend);
		if(!"".equals(lCreatestart) && lCreatestart.length()>=10){
			lInput.addParm("createstart", lCreatestart.substring(0, 10));
		}
		if(!"".equals(lCreateend) && lCreateend.length()>=10){
			lInput.addParm("createend", lCreateend.substring(0, 10));
		}
		lInput.addParm("status", lStatus);
		lInput.addParm("channelid", input.getParm("channelid").getStringColumn());
		lInput.addParm("memberid", lMemberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet; 
		}
		
		//总条数
		//String totalPage = lRet.getParm("totalPage").getStringColumn();  
		//总页数
		//String recordnum = String.valueOf((Integer.parseInt(totalPage)%Integer.parseInt(lPageSize)>0?1:0)+Integer.parseInt(totalPage)/Integer.parseInt(lPageSize));
		Table lOrders = lRet.getParm("orders").getTableColumn();
//		if(lOrders == null || lOrders.getRowCount() < 1){
//			lRet.setError(ErrCodeConstants.API_ORDER_IS_NULL,
//					TipMessager.getMessage(ErrCodeConstants.API_ORDER_IS_NULL, language));
//			return lRet;
//		}
		//TODO 只查出待确认,待支付,已支付,部分待支付，已取消,已退订，已使用的订单?
		CommandRet result = new CommandRet("");
		result.addParm("order", lOrders);
		result.addParm("recordnum", lRet.getParm("recordnum").getStringColumn());
		result.addParm("pagenum", lPageNum);
		result.addParm("pagesize", lPageSize);
//		for(int i = 0 ; i < lOrders.getRowCount() ; i++){
//			Row lRow = lOrders.getRow(i);
//			Row row = tOrder.addRow();
//			String orderid = lRow.getColumn("orderid").getStringColumn();
//			String createtime = lRow.getColumn("createtime").getStringColumn();
//			String flightseg = lRow.getColumn("flightseg").getStringColumn();
//			String ori = flightseg.split("-")[0];
//			String dest = flightseg.split("-")[1];
//			String routtype = lRow.getColumn("routtype").getStringColumn();
//			String totalprice = lRow.getColumn("totalprice").getStringColumn();
//			String status = lRow.getColumn("status").getStringColumn();
//			row.addColumn("orderid", orderid);
//			row.addColumn("createtime", createtime);
//			row.addColumn("ori", ori);
//			row.addColumn("dest", dest);
//			row.addColumn("routtype", routtype);
//			row.addColumn("totalprice", totalprice);
//			row.addColumn("flightdate", lRow.getColumn("flightdate").getStringColumn());
//			row.addColumn("flighttime", lRow.getColumn("flighttime").getStringColumn());
//			row.addColumn("status", status);
//			row.addColumn("oriname", lRow.getColumn("oriname").getStringColumn());
//			row.addColumn("oricitycode", lRow.getColumn("oricitycode").getStringColumn());
//			row.addColumn("oricityname", lRow.getColumn("oricityname").getStringColumn());
//			row.addColumn("oricountry", lRow.getColumn("oricountry").getStringColumn());
//			row.addColumn("destname", lRow.getColumn("destname").getStringColumn());
//			row.addColumn("destcitycode", lRow.getColumn("destcitycode").getStringColumn());
//			row.addColumn("destcityname", lRow.getColumn("destcityname").getStringColumn());
//			row.addColumn("destcountry", lRow.getColumn("destcountry").getStringColumn());
//			row.addColumn("flightno", lRow.getColumn("flightno").getStringColumn());
//			row.addColumn("flightseg", lRow.getColumn("flightseg").getStringColumn());
//			row.addColumn("paxname", lRow.getColumn("paxname").getStringColumn());
//			//支付时间倒计时（单位：秒）
//			String maxpaysecond ="";
//			//支付到达时间(格式yyyyMMdd HH:mm:ss)（API的服务器时间）
//			String maxpaytime="";
//			//如果是1待确认或者是3待支付状态的订单 去获取支付倒计时。
////			if("1".equals(status)||"3".equals(status)){
////				CommandInput paytimeInput = new CommandInput("com.travelsky.quick.order.order.liteDetail");
////				paytimeInput.addParm("orderid", orderid);
////				CommandRet paytimeRet = context.doOther(paytimeInput,false);
////				//判断是否出错
////				if (paytimeRet.isError()) {
////					lRet.setError(paytimeRet.getErrorCode(), paytimeRet.getErrorDesc());
////					return lRet;
////				}
////				Table lPays = paytimeRet.getParm("pays").getTableColumn();
////				if(lPays != null && lPays.getRowCount() > 0){
////					for(int j = 0 ; j < lPays.getRowCount() ; j++){
////						maxpaysecond = lPays.getRow(j).getColumn("maxpaysecond").getStringColumn();
////						maxpaytime = lPays.getRow(j).getColumn("maxpaytime").getStringColumn();
////					}
////				}
////			}
//			row.addColumn("maxpaytime", maxpaytime);
//			row.addColumn("maxpaysecond", maxpaysecond);
//		}
		return result;
	}
	/**
	 * 查询订单支付
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet queryPay(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		String lPayid = input.getParm("payid").getStringColumn();
		if(lOrderno.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_ORDER_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_NO, language));
			return lRet;
		}
		if(lMemberid.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.querypay");

		lInput.addParm("orderno", lOrderno);
		lInput.addParm("memberid", lMemberid);
		lInput.addParm("payid", lPayid);
		lRet = context.doOther(lInput,false);
		return lRet;
	}
	/**
	 * 52. 订单明细查询接口B2C
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet detail(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput=null;
		lInput = new CommandInput("com.cares.sh.order.order.detail");
		input.copyTo(lInput); 
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkDetail(input, lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info, language));
			return lRet;
		}
	//	lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		 return organizeDetailResult(lRet,context);
		
	}
	/**
	 * 52. 订单明细查询接口 1E
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet orderDetail(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput=null;
		lInput = new CommandInput("com.cares.sh.order.order.jdsdetail");
		input.copyTo(lInput); 
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkDetail(input, lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info, language));
			return lRet;
		}
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		lRet.addParm("dcspnr", lRet.getParm("paxflights").
				getTableColumn().getRow(0).getColumn("dcspnr").getStringColumn());
		lRet.addParm("owner", context.getInput().getParm("owner").getStringColumn());
		 return lRet;
	}
	/**
	 * 获取订单支付信息
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet payDetail(CommandData input,SelvetContext<ApiContext> context){
		CommandRet ret = new CommandRet("");
		CommandInput payinput = new CommandInput("com.travelsky.quick.order.payresult");
		input.copyTo(payinput); 
		ret = context.doOther(payinput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (ret.getErrorCode().equals("")||ret.getErrorCode()==null) {
//			String type=LOCK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return ret;
	}

	/**
	 * 订单明细的结果
	 * @param lRet order返回的订单明细
	 * @return
	 */
	protected CommandRet organizeDetailResult(CommandRet lRet, SelvetContext<ApiContext> context){
		CommandRet result = new CommandRet("");
		//订单号
		String orderno = lRet.getParm("orderid").getStringColumn();
		//创建时间
		String  createtime = lRet.getParm("createtime").getStringColumn().substring(0, 19);
		//往返程类型(S单程 R往返程)
		String routtype = lRet.getParm("routtype").getStringColumn();
		//订单总价
		String totalprice = lRet.getParm("totalprice").getStringColumn();
		//订单状态
		String orderstatus = lRet.getParm("status").getStringColumn();
		String currencyCode= lRet.getParm("isoCode").getStringColumn();
		//最大支付时限
		String maxOrderTime = "";
		//保险信息
		Table	subinsurTable = lRet.getParm("subinsurance").getTableColumn();
		//航班信息Table
		Table lFlights = lRet.getParm("flights").getTableColumn();
		Table flights = new Table(new String[]{"id","airlinecd","vatRate","flightno","oricode","destcode","carricd","oriDay","oriTime","destDateTime","destTime","oridate","destdate","oriteminal","destterminal","routtype","familycode","familyname","status","refundinfo","childrefundinfo","babyrefundinfo","pnr","oriname","oricitycode","oricityname","oricountry","destname","destcitycode","destcityname","destcountry","cabin","cabinName","planestype","isseatfree","adultluggage","childluggage","babyluggage","traveltime","weightUnit"});
		//乘机人信息Table
		Table lPaxs = lRet.getParm("paxs").getTableColumn();
		Table paxs = new Table(new String[]{"id","lastname","firstname","paxtype","birth","paxsex","contactprefix","telephone","email","guardian","passtype","passno","pnr","paxname","docexpiry","issuecountry","birthcountry"});
		//乘机人航段信息Table
		Table lPaxflights = lRet.getParm("paxflights").getTableColumn();
		Table paxflights = new Table(new String[]{"id","paxid","flightid","pnr","ticketprice","etStatus","cnfee","yqfee","tax","status","invstatus","salestatus","ticketno","ticketsegno","taxs"});
		//辅营信息Table
		Table lSubmarkets = lRet.getParm("submarkets").getTableColumn();
		Table submarkets = new Table(new String[]{"id","paxid","flightid","submarketcode","submarkettype","submarketname","submarketdesc","currencyCode","isfree","unitprice","buynum","refundinfo","status","familycode","currencySign","ssrtype","invstatus","salestatus","weight"});
		//座位信息Table
		Table lSeats = lRet.getParm("seats").getTableColumn();
		Table seats = new Table(new String[]{"paxid","flightid","isfree","seatno","price","seatstatus","invstatus","salestatus","refundinfo","seatname"});
		//联系人信息
		Table lContacts = lRet.getParm("contacts").getTableColumn();
		Table contacts = new Table(new String[]{"name","contactprefix","telephone","email"});
		//支付信息
		Table lPays = lRet.getParm("pays").getTableColumn();
		Table pays = new Table(new String[]{"id","createtime","price","status","paychannelno","currencyCode","bankid","billno","orgid","apptype","maxpaysecond","maxpaytime"});
		//支付时限（支付时间限制）
		Table lAttl = lRet.getParm("attls").getTableColumn();
		//票号信息
		Table lTickets = lRet.getParm("tickets").getTableColumn();
		Table tickets = new Table(new String[]{"paxid","flightid","flightid2","status","status2","tktno","dispatchid"});
		//退票信息
		Table lRefunds = lRet.getParm("refunds").getTableColumn();
		Table refunds = new Table(new String[]{"id","pnr","refundtype","refundreasontype","refundreason","applicant","telephone","tktam","airporttax","fueltax","taxam","submarketam","seatam","refundam","checkdate","attachment","status"});
		//退票明细信息
		Table lRefunddetails = lRet.getParm("refunddetails").getTableColumn();
		Table refunddetails = new Table(new String[]{"id","refundid","paxid","flightid","feetype","code","refundfee","detailid","servicetype","chargetype"});
		Table deliverSeat=lRet.getParm("deliverSeat").getTableColumn();
		Table deliverSubmarkt=lRet.getParm("deliverSubmarkt").getTableColumn();
		Table deliverCabin=lRet.getParm("deliverCabin").getTableColumn();
		Table costs = lRet.getParm("costs").getTableColumn();
		Table changeApply = lRet.getParm("changeApply").getTableColumn();
		result.addParm("orderno", orderno);
		result.addParm("createtime", createtime);
		result.addParm("routtype", routtype);
		result.addParm("totalprice", totalprice);
		result.addParm("orderstatus", orderstatus);
		result.addParm("maxOrderTime", maxOrderTime);
		result.addParm("serverTime", Unit.getString(new Date(), "yyyy/MM/dd HH:mm:ss"));
		result.addParm("flights", flights);
		result.addParm("paxs", paxs);
		result.addParm("paxflights", paxflights);
		result.addParm("submarkets", submarkets);
		result.addParm("seats", seats);
		result.addParm("contacts", contacts);
		result.addParm("pays", pays);
		result.addParm("tickets", tickets);
		result.addParm("refunds", refunds);
		result.addParm("refunddetails", refunddetails);
		result.addParm("subinsurance", subinsurTable);
		result.addParm("deliverSeat", deliverSeat);
		result.addParm("deliverSubmarkt", deliverSubmarkt);
		result.addParm("deliverCabin", deliverCabin);
		result.addParm("currencyCode", currencyCode);
		result.addParm("costs", costs);
		result.addParm("changeApply", changeApply);
		result.addParm("owner", context.getInput().getParm("owner").getStringColumn());
		//航班信息
		if(lFlights != null && lFlights.getRowCount() > 0){
			for(int i = 0 ; i < lFlights.getRowCount() ; i++){
				Row lRow = lFlights.getRow(i);
				Row row = flights.addRow();
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("airlinecd", lRow.getColumn("airlinecd").getStringColumn());
				row.addColumn("flightno", lRow.getColumn("flightno").getStringColumn());
				row.addColumn("oricode", lRow.getColumn("oricode").getStringColumn());
				row.addColumn("destcode", lRow.getColumn("destcode").getStringColumn());
				row.addColumn("carricd", lRow.getColumn("carricd").getStringColumn());
				row.addColumn("oriDay", lRow.getColumn("oriDay").getStringColumn());
				row.addColumn("vatRate", lRow.getColumn("vatRate").getStringColumn());
				row.addColumn("oriTime", lRow.getColumn("oriTime").getStringColumn());
				row.addColumn("destDateTime", lRow.getColumn("destDateTime").getStringColumn());
				row.addColumn("destTime", lRow.getColumn("destTime").getStringColumn());
				row.addColumn("oridate", lRow.getColumn("oridate").getStringColumn());
				row.addColumn("destdate", lRow.getColumn("destdate").getStringColumn());
				row.addColumn("oriteminal", lRow.getColumn("oriterminal").getStringColumn());
				row.addColumn("destterminal", lRow.getColumn("destterminal").getStringColumn());
				row.addColumn("isseatfree", lRow.getColumn("isseatfree").getStringColumn());
				//来回程类型(G去程 R回程)
				row.addColumn("routtype", lRow.getColumn("routtype").getStringColumn());
				row.addColumn("familycode", lRow.getColumn("familycode").getStringColumn());
				row.addColumn("familyname", lRow.getColumn("familyname").getObjectColumn());
				//状态(1未支付 2已支付 3退票)
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				row.addColumn("refundinfo", lRow.getColumn("refundinfo").getStringColumn());
				row.addColumn("childrefundinfo", lRow.getColumn("childrefundinfo").getStringColumn());
				row.addColumn("babyrefundinfo", lRow.getColumn("babyrefundinfo").getStringColumn());
				row.addColumn("pnr", lRow.getColumn("pnr").getStringColumn());
				row.addColumn("oriname", lRow.getColumn("oriname").getStringColumn());
				row.addColumn("oricitycode", lRow.getColumn("oricitycode").getStringColumn());
				row.addColumn("oricityname", lRow.getColumn("oricityname").getStringColumn());
				row.addColumn("oricountry", lRow.getColumn("oricountry").getStringColumn());
				row.addColumn("destname", lRow.getColumn("destname").getStringColumn());
				row.addColumn("destcitycode", lRow.getColumn("destcitycode").getStringColumn());
				row.addColumn("destcityname", lRow.getColumn("destcityname").getStringColumn());
				row.addColumn("destcountry", lRow.getColumn("destcountry").getStringColumn());
				row.addColumn("cabin", lRow.getColumn("cabin").getStringColumn());
				row.addColumn("cabinName", getCabinName(context, lRow.getColumn("basiccabin").getStringColumn()));
				row.addColumn("planestype", lRow.getColumn("planestype").getStringColumn());
				row.addColumn("adultluggage", lRow.getColumn("adultluggage").getStringColumn());
				row.addColumn("childluggage", lRow.getColumn("childluggage").getStringColumn());
				row.addColumn("babyluggage", lRow.getColumn("babyluggage").getStringColumn());
				row.addColumn("weightUnit", lRow.getColumn("weightUnit").getStringColumn());
				row.addColumn("traveltime", getTimeInterVal(lRow));
				
			}
		}
		//乘机人信息
		if(lPaxs != null && lPaxs.getRowCount() > 0){
			//ADT成人 CHD儿童  INF婴儿
			List<Row> listADT = new ArrayList<>();
			List<Row> listCHD = new ArrayList<>();
			List<Row> listINF = new ArrayList<>();
			List<Row> listQT = new ArrayList<>();
			for(int i = 0 ; i < lPaxs.getRowCount() ; i++){
				Row lRow = lPaxs.getRow(i);
				String paxtype = lRow.getColumn("paxtype").getStringColumn();
				if("ADT".equals(paxtype)){
					listADT.add(lRow);
				}else if("CHD".equals(paxtype)){
					listCHD.add(lRow);
				}else if("INF".equals(paxtype)){
					listINF.add(lRow);
				}else{
					//非成人、儿童 、婴儿的
					listQT.add(lRow);
				}
			}
			if(null != listADT && listADT.size()>0){
				for(int i = 0 ; i < listADT.size() ; i++){
					setPaxs(paxs,listADT.get(i));
				}
			}
			if(null != listCHD && listCHD.size()>0){
				for(int i = 0 ; i < listCHD.size() ; i++){
					setPaxs(paxs,listCHD.get(i));
				}
			}
			if(null != listINF && listINF.size()>0){
				for(int i = 0 ; i < listINF.size() ; i++){
					setPaxs(paxs,listINF.get(i));
				}
			}
			if(null != listQT && listQT.size()>0){
				for(int i = 0 ; i < listQT.size() ; i++){
					setPaxs(paxs,listQT.get(i));
				}
			}
			//如果这里 错误 不为空则乘机人加密出现异常
			if(!"".equals(lRet.getErrorCode())){
				return lRet;
			}

		}
		if(lPaxflights != null && lPaxflights.getRowCount() > 0){
			for(int i = 0 ; i < lPaxflights.getRowCount() ; i++){
				Row lRow = lPaxflights.getRow(i);
				Row row = paxflights.addRow();
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("pnr", lRow.getColumn("pnr").getStringColumn());
				row.addColumn("ticketno", lRow.getColumn("ticketno").getStringColumn());
				row.addColumn("ticketsegno", lRow.getColumn("ticketsegno").getStringColumn());
				//状态(1待确认、2已拒绝、3待支付、4已取消、5已支付、6已出票、7待退款、8待退款审核、
				//9已退款、10已值机、11已交付、12已使用、13已使用 )
//				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				row.addColumn("invstatus", lRow.getColumn("invstatus").getStringColumn());
				row.addColumn("salestatus", lRow.getColumn("salestatus").getStringColumn());
				row.addColumn("etStatus", lRow.getColumn("etStatus").getStringColumn());
				Table taxs = new Table(new String[]{"code","name","price"});
				Table lCosts = lRet.getParm("costs").getTableColumn();
				if(lCosts != null && lCosts.getRowCount() > 0){
					for(int j = 0 ; j < lCosts.getRowCount() ; j++){
						Row costsRow = lCosts.getRow(j);
						String mappingid = costsRow.getColumn("mappingid").getStringColumn();
						String flightid = costsRow.getColumn("flightid").getStringColumn();
						String serviceType = costsRow.getColumn("serviceType").getStringColumn();
						String chargeType = costsRow.getColumn("chargeType").getStringColumn();
						String chargeCode = costsRow.getColumn("chargeCode").getStringColumn();
						String vatSource = costsRow.getColumn("vatSource").getStringColumn();
						String price = costsRow.getColumn("price").getStringColumn();
						if (mappingid.equals(lRow.getColumn("paxid").getStringColumn()) && flightid.equals(lRow.getColumn("flightid").getStringColumn())) {
							if("FARE".equals(serviceType) && "Fare".equals(chargeType)){
								row.addColumn("ticketprice", price);
							} else if("FARE".equals(serviceType) && !StringUtils.hasLength(vatSource) && ("Fee".equals(chargeType) || "Tax".equals(chargeType))){
								Row tax = taxs.addRow();
								tax.addColumn("code", chargeCode);
								tax.addColumn("name", StatusUtil.getLanguageName(costsRow.getColumn("name").getObjectColumn(), language));
								tax.addColumn("price", price);
							}
						}

					}
				}
				row.addColumn("taxs", taxs);
			}
		}
		String language=ApiServletHolder.getApiContext().getLanguage();
		if(lSubmarkets != null && lSubmarkets.getRowCount() > 0){
			for(int i = 0 ; i < lSubmarkets.getRowCount() ; i++){
				Row lRow = lSubmarkets.getRow(i);
				Row row = submarkets.addRow();
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("submarketcode", lRow.getColumn("submarketcode").getStringColumn());
				row.addColumn("submarkettype", lRow.getColumn("submarkettype").getStringColumn());
				row.addColumn("isfree", lRow.getColumn("isfree").getStringColumn());
				row.addColumn("unitprice", lRow.getColumn("unitprice").getStringColumn());
				row.addColumn("buynum", lRow.getColumn("buynum").getStringColumn());
				row.addColumn("refundinfo", lRow.getColumn("refundinfo").getStringColumn());
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				if ("zh_CN".equals(language)) {
					row.addColumn("submarketname", lRow.getColumn("submarketname").getObjectColumn().getParm("zh_CN").getStringColumn());
				}else{
					row.addColumn("submarketname", lRow.getColumn("submarketname").getObjectColumn().getParm("en_US").getStringColumn());
				}
				row.addColumn("submarketdesc", lRow.getColumn("submarketdesc").getStringColumn());
				row.addColumn("familycode", lRow.getColumn("familycode").getStringColumn());
				row.addColumn("currencySign", lRow.getColumn("currencyCode").getStringColumn());
				row.addColumn("ssrtype", lRow.getColumn("ssrtype").getStringColumn());
				row.addColumn("invstatus", lRow.getColumn("invstatus").getStringColumn());
				row.addColumn("salestatus", lRow.getColumn("salestatus").getStringColumn());
				CommandData commandData = lRow.getColumn("attr").getObjectColumn();
				if(commandData != null && !"".equals(commandData)){
					String weight = commandData.getParm("weight").getStringColumn();
					row.addColumn("weight", weight);
				}
			}
		}
		if(lSeats != null && lSeats.getRowCount() > 0){
			for(int i = 0 ; i < lSeats.getRowCount() ; i++){
				Row lRow = lSeats.getRow(i);
				Row row = seats.addRow();
				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("seatno", lRow.getColumn("seatno").getStringColumn());
				row.addColumn("isfree", lRow.getColumn("isfree").getStringColumn());
				row.addColumn("price", lRow.getColumn("price").getStringColumn());
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				row.addColumn("invstatus", lRow.getColumn("invstatus").getStringColumn());
				row.addColumn("seatstatus", lRow.getColumn("seatstatus").getStringColumn());
				row.addColumn("salestatus", lRow.getColumn("salestatus").getStringColumn());
				row.addColumn("refundinfo", lRow.getColumn("refundinfo").getStringColumn());
				row.addColumn("seatname", lRow.getColumn("seatname").getStringColumn());
			}
		}
		if(lContacts != null && lContacts.getRowCount() > 0){
			for(int i = 0 ; i < lContacts.getRowCount() ; i++){
				Row lRow = lContacts.getRow(i);
				Row row = contacts.addRow();
				row.addColumn("name", lRow.getColumn("name").getStringColumn());
				row.addColumn("contactprefix", lRow.getColumn("contactprefix").getStringColumn());
				row.addColumn("telephone", lRow.getColumn("telephone").getStringColumn());
				row.addColumn("email", lRow.getColumn("email").getStringColumn());
			}
		}
		if(lPays != null && lPays.getRowCount() > 0){
			for(int i = 0 ; i < lPays.getRowCount() ; i++){
				Row lRow = lPays.getRow(i);
				Row row = pays.addRow();
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("createtime", lRow.getColumn("createtime").getStringColumn());
				row.addColumn("price", lRow.getColumn("price").getStringColumn());
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				row.addColumn("paychannelno", lRow.getColumn("paychannelno").getStringColumn());
				row.addColumn("bankid", lRow.getColumn("bankid").getStringColumn());
				row.addColumn("billno", lRow.getColumn("billno").getStringColumn());
				row.addColumn("orgid", lRow.getColumn("orgid").getStringColumn());
				row.addColumn("apptype", lRow.getColumn("apptype").getStringColumn());
				row.addColumn("maxpaysecond", lRow.getColumn("maxpaysecond").getStringColumn());
				row.addColumn("maxpaytime", lRow.getColumn("maxpaytime").getStringColumn());
				if (Constant.Order.PAY_STATUS_TOPAY.equals(row.getColumn("status").getStringColumn())) {
					//获取最大支付时限
					if(lAttl != null && lAttl.getRowCount() > 0){
						for(int j = 0 ; j < lAttl.getRowCount() ; j++){
							Row mRow = lAttl.getRow(j);
							String payid = mRow.getColumn("payid").getStringColumn();
							if(!payid.isEmpty()&&payid.equals(lRow.getColumn("id").getStringColumn())){
								Date maxpaytime = lRow.getColumn("maxpaytime").getDateColumn();
								result.addParm("maxOrderTime", Unit.getString(maxpaytime, "yyyy/MM/dd HH:mm:ss"));
							}

						}
					}
				}
			}
		}
		if(lTickets != null && lTickets.getRowCount() > 0){
			for(int i = 0 ; i < lTickets.getRowCount() ; i++){
				Row lRow = lTickets.getRow(i);
				Row row = tickets.addRow();
				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("tktno", lRow.getColumn("tktno").getStringColumn());
				row.addColumn("dispatchid", lRow.getColumn("dispatchid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("flightid2", lRow.getColumn("flightid2").getStringColumn());
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				row.addColumn("status2", lRow.getColumn("status2").getStringColumn());
			}
		}
		if(lRefunds != null && lRefunds.getRowCount() > 0){
			for(int i = 0 ; i < lRefunds.getRowCount() ; i++){
				Row lRow = lRefunds.getRow(i);
				Row row = refunds.addRow();
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("pnr", lRow.getColumn("pnr").getStringColumn());
				row.addColumn("refundtype", lRow.getColumn("refundtype").getStringColumn());
				row.addColumn("refundreasontype", lRow.getColumn("refundreasontype").getStringColumn());
				row.addColumn("refundreason", lRow.getColumn("refundreason").getStringColumn());
				row.addColumn("applicant", lRow.getColumn("applicant").getStringColumn());
				row.addColumn("telephone", lRow.getColumn("telephone").getStringColumn());
				row.addColumn("tktam", lRow.getColumn("tktam").getStringColumn());
				row.addColumn("airporttax", lRow.getColumn("airporttax").getStringColumn());
				row.addColumn("fueltax", lRow.getColumn("fueltax").getStringColumn());
				row.addColumn("taxam", lRow.getColumn("taxam").getStringColumn());
				row.addColumn("submarketam", lRow.getColumn("submarketam").getStringColumn());
				row.addColumn("seatam", lRow.getColumn("").getStringColumn());
				row.addColumn("refundam", lRow.getColumn("refundam").getStringColumn());
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				//TODO 返回结果里没有
				row.addColumn("checkdate", lRow.getColumn("lastedittime").getStringColumn());
				//获取对应refund的id的附件
				StringBuffer files = new StringBuffer();
				Table filesTab = lRet.getParm("files").getTableColumn();
				for(int n=0; filesTab!=null && n<filesTab.getRowCount();n++){
					if(lRow.getColumn("id").getStringColumn()!=null &&
							lRow.getColumn("id").getStringColumn()
							.equals(filesTab.getRow(n).getColumn("refundid").getStringColumn())){
						if("".equals(files)){
							files.append(filesTab.getRow(n).getColumn("fileid").getStringColumn());
						}else{
							files.append(",").append(filesTab.getRow(n).getColumn("fileid").getStringColumn());
						}
					}
				}
				row.addColumn("attachment", files.toString());
			}
		}
		if(lRefunddetails != null && lRefunddetails.getRowCount() > 0){
			for(int i = 0 ; i < lRefunddetails.getRowCount() ; i++){
				Row lRow = lRefunddetails.getRow(i);
				Row row = refunddetails.addRow();
				String servicetype = lRow.getColumn("servicetype").getStringColumn();
				String chargetype = lRow.getColumn("chargetype").getStringColumn();
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("refundfee", lRow.getColumn("refundfee").getStringColumn());
				row.addColumn("refundid", lRow.getColumn("refundid").getStringColumn());
				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("detailid", lRow.getColumn("detailid").getStringColumn());
				row.addColumn("servicetype", servicetype);
				row.addColumn("chargetype", chargetype);
				//从costs和submckets里取出对应相关信息放入对应字段
				Table lCosts = lRet.getParm("costs").getTableColumn();
				String submarketcode = null;
				if (lCosts.getRowCount()>0) {
					for (int k = 0; k < lCosts.getRowCount(); k++) {
						Row costsRow=lCosts.getRow(k);
						String costsId = costsRow.getColumn("id").getStringColumn();
						if (lRow.getColumn("detailid").getStringColumn().equals(costsId)) {
							String mappingId = costsRow.getColumn("mappingid").getStringColumn();
							if("FARE".equals(servicetype) && "Fare".equals(chargetype)){//机票
								 
							} else if("FARE".equals(servicetype) && ("Fee".equals(chargetype) || "Tax".equals(chargetype))){//税费/
								submarketcode = StatusUtil.getLanguageName(costsRow.getColumn("name").getObjectColumn(), language);
							}else if("SEAT".equals(servicetype)) {
								for (int j = 0; j < lSeats.getRowCount(); j++) {
									Row lSeatsRow=lSeats.getRow(j);
									if (mappingId.equals(lSeatsRow.getColumn("id").getStringColumn())) {
										submarketcode = lSeatsRow.getColumn("seatname").getStringColumn();//seatname
									}
								}
								
							}else {
								for (int l = 0; l < lSubmarkets.getRowCount(); l++) {
									Row submarketsRow=lSubmarkets.getRow(l);
										if (mappingId.equals(submarketsRow.getColumn("id").getStringColumn())) {
											 submarketcode = submarketsRow.getColumn("submarketcode").getStringColumn();
										}
								}
							}
						}
					}
				}
				row.addColumn("code", submarketcode);
			}
		}

		return result;
	}

	/**
	 *  订单详情乘机人信息载入
	 * @param paxs json节点
	 * @param lRow  乘机人数据
	 */
	public static void setPaxs(Table paxs,Row lRow){
		Row row = paxs.addRow();
		row.addColumn("id", lRow.getColumn("id").getStringColumn());
		row.addColumn("lastname", lRow.getColumn("lastname").getStringColumn());
		row.addColumn("firstname", lRow.getColumn("firstname").getStringColumn());
		//人员类型(ADT成人 CHD儿童 INF婴儿)
		row.addColumn("paxtype", lRow.getColumn("paxtype").getStringColumn());
		row.addColumn("birth", lRow.getColumn("birth").getStringColumn());
		//性别(M男 F女)
		row.addColumn("paxsex", lRow.getColumn("paxsex").getStringColumn());
		row.addColumn("contactprefix", lRow.getColumn("contactprefix").getStringColumn());
		row.addColumn("telephone", lRow.getColumn("telephone").getStringColumn());
		row.addColumn("email", lRow.getColumn("email").getStringColumn());
		row.addColumn("guardian", lRow.getColumn("guardian").getStringColumn());
		//证件类型(NI身份证 PP护照 OT其他)
		row.addColumn("passtype", lRow.getColumn("passtype").getStringColumn());
		row.addColumn("passno", lRow.getColumn("passno").getStringColumn());
		row.addColumn("pnr", lRow.getColumn("pnr").getStringColumn());
		//证件签发国，有效期，出生国
		row.addColumn("docexpiry", lRow.getColumn("docexpiry").getStringColumn());
		row.addColumn("issuecountry", lRow.getColumn("issuecountry").getStringColumn());
		row.addColumn("birthcountry", lRow.getColumn("birthcountry").getStringColumn());
	}


	/**
	 * 53.	退票预算接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet refundbudget(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.refund");

		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkRefundtest(input,lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info, language));
			return lRet;
		}
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("mode", "1");
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		Table retList = lRet.getParm("refundedlist").getTableColumn();
		if(retList == null || retList.getRowCount() < 1){
			lRet.setError(ErrCodeConstants.API_REFUNDBUDGET_PROCESS_FAILD,
					TipMessager.getMessage(ErrCodeConstants.API_REFUNDBUDGET_PROCESS_FAILD, language));
			return lRet;
		}
		//重构结果集
		Table lPax = new Table(new String[]{"paxid","flightid","txtprice","cnprice","yqprice","seatprice","txtcharge","cncharge","yqcharge","seatcharge","submarkets","itemlist"});
		for(int i = 0 ; i < retList.getRowCount() ; i++){
			Row row = retList.getRow(i);
			Row lRow = lPax.addRow();
			lRow.addColumn("itemlist", row.getColumn("itemlist").getTableColumn());
			lRow.addColumn("paxid", row.getColumn("paxid").getStringColumn());
			lRow.addColumn("flightid", row.getColumn("flightid").getStringColumn());
			lRow.addColumn("txtprice", row.getColumn("price").getStringColumn());
			lRow.addColumn("cnprice", row.getColumn("pricecn").getStringColumn());
			lRow.addColumn("yqprice", row.getColumn("priceyq").getStringColumn());
			lRow.addColumn("seatprice", row.getColumn("seatprice").getStringColumn());//qiqiqi没有这个
			lRow.addColumn("txtcharge", row.getColumn("commisionchargemoney").getStringColumn());
			lRow.addColumn("cncharge", row.getColumn("commisionchargecnmoney").getStringColumn());
			lRow.addColumn("yqcharge", row.getColumn("commisionchargeyqmoney").getStringColumn());
			lRow.addColumn("seatcharge", row.getColumn("seatcharge").getStringColumn());//qiqiqi没有这个
			Table submarkets = row.getColumn("itemlist").getTableColumn();
			Table lSubmarkets = new Table(new String[]{"id","paxid","flightid","submarkettype","submarketname","unitprice","buynum","unitpricecharge"});
			lRow.addColumn("submarkets", lSubmarkets);
			if(submarkets != null && submarkets.getRowCount() > 0){
				for(int j = 0 ; j < submarkets.getRowCount() ; j++){
					Row sRow = submarkets.getRow(j);
					Row lSrow = lSubmarkets.addRow();
					lSrow.addColumn("id", sRow.getColumn("itemid").getStringColumn());
					lSrow.addColumn("paxid", row.getColumn("paxid").getStringColumn());
					lSrow.addColumn("flightid", row.getColumn("flightid").getStringColumn());
					lSrow.addColumn("submarkettype", sRow.getColumn("itemtype").getStringColumn());
					lSrow.addColumn("submarketname", sRow.getColumn("itemname").getStringColumn());
					lSrow.addColumn("unitprice", sRow.getColumn("totalmoney").getStringColumn());
					lSrow.addColumn("buynum", sRow.getColumn("buynum").getStringColumn());
					lSrow.addColumn("unitpricecharge", sRow.getColumn("commisionchargemoney").getStringColumn());
				}
			}
		}


		CommandRet result = new CommandRet("");
		result.addParm("paxs", lPax);
		result.addParm("totalmoney", lRet.getParm("totalmoney").getStringColumn());
		result.addParm("chargetotalmoney", lRet.getParm("chargetotalmoney").getStringColumn());
		result.addParm("refundmoney", lRet.getParm("refundmoney").getStringColumn());
		return result;
	}

	/**
	 * 54.	提交退票申请接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet refund(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String cmdName = "com.cares.sh.order.order.refund";
		CommandInput lInput = new CommandInput(cmdName);
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkRefund(input,lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info, language));
			return lRet;
		}
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("mode", "2");
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		Table retList = lRet.getParm("refundedlist").getTableColumn();
		if(retList == null || retList.getRowCount() < 1){
			lRet.setError(ErrCodeConstants.API_REFUNDSUMIT_PROCESS_FAILD,
					TipMessager.getMessage(ErrCodeConstants.API_REFUNDSUMIT_PROCESS_FAILD, language));
			return lRet;
		}
		//重构结果集
		Table lPax = new Table(new String[]{"paxid","flightid","txtprice","cnprice","yqprice","seatprice","txtcharge","cncharge","yqcharge","seatcharge","submarkets"});
		for(int i = 0 ; i < retList.getRowCount() ; i++){
			Row row = retList.getRow(i);
			Row lRow = lPax.addRow();
			lRow.addColumn("paxid", row.getColumn("paxid").getStringColumn());
			lRow.addColumn("flightid", row.getColumn("flightid").getStringColumn());
			lRow.addColumn("txtprice", row.getColumn("price").getStringColumn());
			lRow.addColumn("cnprice", row.getColumn("pricecn").getStringColumn());
			lRow.addColumn("yqprice", row.getColumn("priceyq").getStringColumn());
			lRow.addColumn("seatprice", row.getColumn("seatprice").getStringColumn());
			lRow.addColumn("txtcharge", row.getColumn("commisionchargemoney").getStringColumn());
			lRow.addColumn("cncharge", row.getColumn("commisionchargecnmoney").getStringColumn());
			lRow.addColumn("yqcharge", row.getColumn("commisionchargeyqmoney").getStringColumn());
			lRow.addColumn("seatcharge", row.getColumn("seatcharge").getStringColumn());
			Table submarkets = row.getColumn("itemlist").getTableColumn();
			Table lSubmarkets = new Table(new String[]{"id","paxid","flightid","submarkettype","submarketname","unitprice","buynum","unitpricecharge"});
			lRow.addColumn("submarkets", lSubmarkets);
			if(submarkets != null && submarkets.getRowCount() > 0){
				for(int j = 0 ; j < submarkets.getRowCount() ; j++){
					Row sRow = submarkets.getRow(j);
					Row lSrow = lSubmarkets.addRow();
					lSrow.addColumn("id", sRow.getColumn("itemid").getStringColumn());
					lSrow.addColumn("paxid", row.getColumn("paxid").getStringColumn());
					lSrow.addColumn("flightid", row.getColumn("flightid").getStringColumn());
					lSrow.addColumn("submarkettype", sRow.getColumn("itemtype").getStringColumn());
					lSrow.addColumn("submarketname", sRow.getColumn("itemname").getObjectColumn());
					lSrow.addColumn("unitprice", sRow.getColumn("totalmoney").getStringColumn());
					lSrow.addColumn("buynum", sRow.getColumn("buynum").getStringColumn());
					lSrow.addColumn("unitpricecharge", sRow.getColumn("commisionchargemoney").getStringColumn());
				}
			}
		}


		CommandRet result = new CommandRet("");
		result.addParm("paxs", lPax);
		return result;

	}

	/**
	 * 55.   我的行程查询接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet querttravel(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String flightstart = input.getParm("flightstart").getStringColumn();
		String flightend = input.getParm("flightend").getStringColumn();
		String memberid = input.getParm("memberid").getStringColumn();
		if (memberid.isEmpty()) {
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		Date startTime = null;
		Date endTime = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if(!"".equals(flightstart) && flightstart != null){
			try {
				startTime = sdf.parse(flightstart);
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_STDATE, language));
				return lRet;
			}
		}
		if(!"".equals(flightend) && flightend != null){
			try {
				endTime = sdf.parse(flightend);
			} catch (ParseException e) {
				lRet.setError(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE,
						TipMessager.getMessage(ErrCodeConstants.API_CONVERT_FLIGHT_EDDATE, language));
				return lRet;
			}
		}
		if(startTime != null && endTime != null){
			if(startTime.compareTo(endTime) > 0){
				lRet.setError(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_STARTTIME_ENDTIME_ERROR, language));
				return lRet;
			}
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.querttravel.lt");
		lInput.addParm("flightstart",flightstart);
		lInput.addParm("flightend",flightend);
		lInput.addParm("usedflag",input.getParm("usedflag").getStringColumn());
		lInput.addParm("sortflag",input.getParm("sortflag").getStringColumn());
		lInput.addParm("pagesize",input.getParm("pagesize").getStringColumn());
		lInput.addParm("pagenum",input.getParm("pagenum").getStringColumn());
		lInput.addParm("memberid",memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}

	/**
	 * 前置条件查询
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet precondition(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if (memberid.isEmpty()) {
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.precondition");
		lInput.addParm("memberid",memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		//{"unpayOrderLimit ":"最大的未支付订单个数","unpayOrderNum ":"当前未支付订单个数"}
		return lRet;
	}
	
	/**
	 * 订单删除接口 
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet orderDelete(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		String orderno = input.getParm("orderno").getStringColumn();
		String memberid =  ApiServletHolder.getApiContext().getUserID();
		if(orderno.isEmpty()){
			//订单号为空
			lRet.setError(ErrCodeConstants.API_ORDER_NUMBER_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORDER_NUMBER_ERROR, language));
			return lRet;
		}
		if(memberid.isEmpty()){
			//请检查会员帐号
			lRet.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return lRet;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.detail");
		lInput.addParm("orderno", orderno);
		lInput.addParm("memberid", memberid);
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput, false);
		if (lRet.isError()) {
			//没有订单！
			lRet.setError(ErrCodeConstants.API_ORDER_IS_NULL,
					TipMessager.getMessage(ErrCodeConstants.API_ORDER_IS_NULL, language));
			return lRet;
		}
		Table paxflights = lRet.getParm("paxflights").getTableColumn();
		boolean status = true;
		if(paxflights != null &&  paxflights.getRowCount() < 1){
			for (Row row : paxflights) {
				//库存状态
				String invstatus = row.getColumn("invstatus").getStringColumn();
				//销售状态
				String salestatus = row.getColumn("salestatus").getStringColumn();
				if(!invstatus.equals("CANCELLED") && !salestatus.equals("NN")){
					status = false;
				}
			}
		}
		if(!status){
			lRet.setError(ErrCodeConstants.API_ORDER_STATUS_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORDER_STATUS_ERROR, language));
			return lRet;
		}
		CommandInput deleteInput = new CommandInput("com.cares.sh.order.order.update.deleteflag");
		deleteInput.addParm("orderno", orderno);
		deleteInput.addParm("memberid", memberid);
		lRet = context.doOther(deleteInput,false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		lRet.addParm("orderno", orderno);
		return lRet;
	}
	
	public CommandRet updateFile(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		// refundop表示退款操作. 1表示修改退款申请文件。否则表示退款申请
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.updatebusinessinfo");
		//输入参数验证
		OrderValidate lValidate = new OrderValidate();
		String info = lValidate.checkRefund(input,lInput);
		if (!info.isEmpty()) {
			lRet.setError(info, TipMessager.getMessage(info, language));
			return lRet;
		}
		lInput.addParm("channelid", context.getContext().getChannelNo());
		lInput.addParm("mode", "2");
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		return lRet;
	}
	
	/**
	 * 出票接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet outTicket(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.travelsky.quick.order.directpay");
		input.copyTo(lInput);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		return lRet;
	}
	
	/**
	 * 查询票面接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet queryTicket(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.cares.sh.order.queryticket");
		String ticketNo = input.getParm("ticketNo").getStringColumn();
		if (!StringUtils.hasLength(ticketNo)) {
			//票号号为空
			lRet.setError(ErrCodeConstants.API_NULL_TICKET_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_TICKET_NO, language));
			return lRet;
		}
		lInput.addParm("ticketNo", ticketNo);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		return lRet;
	}
	
	/**
	 * 退票接口(1E)
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet refundTicket(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.refund");
		Table refundtkts = input.getParm("refundtkts").getTableColumn();
		if(refundtkts != null && refundtkts.getRowCount() > 0){
			for (int i = 0; i < refundtkts.getRowCount(); i++) {
				Row refundtktRow = refundtkts.getRow(i);
				String tktno = refundtktRow.getColumn("tktno").getStringColumn();
				if(!StringUtils.hasLength(tktno)){
					lRet.setError(ErrCodeConstants.API_NULL_TICKET_NO,
							TipMessager.getMessage(ErrCodeConstants.API_NULL_TICKET_NO, language));
					return lRet;
				}
			}
		}else{
			lRet.setError(ErrCodeConstants.API_NULL_TICKET_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_TICKET_NO, language));
			return lRet;
		}
		lInput.addParm("refundtkts", refundtkts);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		return lRet;
	}
	
	/**
	 * cancelET
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet cancelTicket(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.travelsky.quick.order.onee.cancel.ticket");
		Table tkts = input.getParm("tkts").getTableColumn();
		if(tkts != null && tkts.getRowCount() > 0){
			for (int i = 0; i < tkts.getRowCount(); i++) {
				Row tktRow = tkts.getRow(i);
				String tktno = tktRow.getColumn("tktno").getStringColumn();
				if(!StringUtils.hasLength(tktno)){
					lRet.setError(ErrCodeConstants.API_NULL_TICKET_NO,
							TipMessager.getMessage(ErrCodeConstants.API_NULL_TICKET_NO, language));
					return lRet;
				}
			}
		}else{
			lRet.setError(ErrCodeConstants.API_NULL_TICKET_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_TICKET_NO, language));
			return lRet;
		}
		lInput.addParm("tkts", tkts);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lRet = context.doOther(lInput,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (lRet.getErrorCode().equals("")||lRet.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		return lRet;
	}
	
	public String getCabinName(SelvetContext<ApiContext> context, String cabin){
		CommandInput input = new CommandInput("com.cares.sh.shopping.basiccabin.view");
		input.addParm("cabin", cabin);
		CommandRet ret = context.doOther(input, true);
		if (ret.isError()) {
			return null;
		}
		return StatusUtil.getLanguageName(ret.getParm("name").getObjectColumn(), language);
	}
	/**
	 * @Title:getTimeInterVal
	 * @Description:根据起始到达日期获得旅行时间间隔
	 * @param row Row 航段信息
	 * @return 旅行时间
	 * @throws ParseException 
	 */
	private String getTimeInterVal(Row row){
		//起飞日期 20180519
		String oriDate = row.getColumn("oriDay").getStringColumn();
		oriDate=oriDate.substring(0, 4)+"-"+oriDate.substring(4, 6)+"-"+oriDate.substring(6, 8);
		//到达日期
		String destDate = row.getColumn("destDateTime").getStringColumn().substring(0, 10);
		//起飞机场所在时区
		String oriTimezone = row.getColumn("oriTimeZone").getStringColumn();
		//到达机场所在时区
		String destTimezone = row.getColumn("destTimeZone").getStringColumn();
		//起飞时间
		String oriTime = row.getColumn("oriTime").getStringColumn();
		//到达时间
		String destTime = row.getColumn("destTime").getStringColumn();		
		//计算时间之前先对数据进行校验，通过正常返回，不通过更换获取方式
		//起飞日期校验
		String oriDateTime = row.getColumn("oriDateTime").getStringColumn();
		String oridate = row.getColumn("oridate").getStringColumn();
		String destDateTime = row.getColumn("destDateTime").getStringColumn();
		String destdate = row.getColumn("destdate").getStringColumn();
		if (!StringUtils.hasLength(oriDate)&&!"".equals(oriDateTime)) {
			oriDate=oriDateTime.substring(0, 10);
		}else if (!StringUtils.hasLength(oriDate)&&!"".equals(oridate)) {
			String oridatel = oridate.substring(0, 8);
			oriDate=oridatel.substring(0, 4)+"-"+oridatel.substring(4, 6)+"-"+oridatel.substring(6, 8);
		} 
		//到达日期校验
		if (!StringUtils.hasLength(destDate)&&!"".equals(destdate)) {
			String oridatel = destdate.substring(0, 8);
			oriDate=oridatel.substring(0, 4)+"-"+oridatel.substring(4, 6)+"-"+oridatel.substring(6, 8);
		} 
		//起飞时间校验
		if (!StringUtils.hasLength(oriTime)&&!"".equals(oriDateTime)) {
			 oriTime = oriDateTime.split(" ")[1].substring(0, 8);
		}else if (!StringUtils.hasLength(oriTime)&&!"".equals(oridate)) {
			oriTime = oridate.split(" ")[1];
		} 
		//到达时间校验
		if (!StringUtils.hasLength(destTime)&&!"".equals(destDateTime)) {
			destTime = destDateTime.split(" ")[1].substring(0, 8);
		}else if (!StringUtils.hasLength(oriTime)&&!"".equals(destDateTime)) {
			destTime = destdate.split(" ")[1];
		} 
		if(oriTimezone == null ){
			oriTimezone = "+00:00";
		}
		if(destTimezone == null ){
			destTimezone = "+00:00";
		}
		
		//起飞日期
		Date oriFlightDate=TimeZoneUtil.getTimeZoneDateByTimezone(oriDate+" "+oriTime, "yyyy-MM-dd HH:mm", oriTimezone);
		//到达日期
		Date destFlightDate=TimeZoneUtil.getTimeZoneDateByTimezone(destDate+" "+destTime, "yyyy-MM-dd HH:mm", destTimezone);
		//把到达日期转化为起始日期所在时区的时间
		//destDate=UTCTimeUtil.getTargetTimezoneTime(destDate, destTimeZone, oriTimeZone);
			long oriSecond=oriFlightDate.getTime();
			long destSecond=destFlightDate.getTime();
			//计算飞行时长的分钟数
			long minutes=(destSecond-oriSecond)/(1000*60);
			return String.valueOf(minutes);
	}	
}