package com.travelsky.quick.util.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.iata.iata.edist.CouponInfoType.AdditionalServicesInfo;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.CommandInputRet;
import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
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
import com.travelsky.quick.framework.util.MergeableCallWorker;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.RedisUtil;
import com.travelsky.quick.util.Utils;

/**
 *
 * @author ZHANGJIABIN
 *
 */
public class ShoppingManager {
	 private static final  int PLACELENGTH=3;
	 public static final String REG_ENGLISH = "[a-zA-Z]{1,20}";
	 private static String language = ApiServletHolder.getApiContext().getLanguage();
	 public static final String SERVICE = "SERVICE";
	 public static final String LOOK = "LOOK";
	 private static Map<String,Integer>flightPrice=new HashMap<String,Integer>();
	 private String[] fligtharr=new String[]{"travelTime","oridate","chdValid","destcitycode","oriTime",
			 "invVersion","destDateTime","airlinecd","destDay","oriterminal","mileage", "flightRoute",
			 "display","ssrs","infValid","destname","destterminal","destcountry","tktno","y100price",
			 "cabin","dayChange", "oriDateTime","oriDay","carrisuffix","carricd","destdate",
			 "carriflightno","flightDay","weightmode","brandinfos","passby","destTimeZone", "meal",
			 "currencyCode","precision","weightUnit","destTime","oricode","airlineCountry","suffix",
			 "oricityname","destcode","oricitycode","oricountry","oriTimeZone","stpnum","planestype",
			 "destcityname","flightid","flightno"};
	 private int totalprice=0;
	 private int adultprice=0;
	 private int chdprice=0;
	 private int infprice=0;
	 private String display="";
	 private String isCode="";
      private int refundid=0;
	 
	/**
	  * 获取支付时限
	  * @param input
	  * @param context
	  * @return CommandRet
	  */
	 public CommandRet payTime(CommandData input,SelvetContext<ApiContext> context){
			CommandRet lRet = new CommandRet("");
			String code = input.getParm("code").getStringColumn();
			if(code==null){
				lRet.setError(ErrCodeConstants.API_NULL_CHANNEL,
						TipMessager.getMessage(ErrCodeConstants.API_NULL_CHANNEL, language));
				return lRet;
			}
			CommandInput lInput = new CommandInput("com.cares.sh.shopping.channel.show");
			lInput.addParm("code", code);
			lRet = context.doOther(lInput, false);
			if(lRet.isError()){
				lRet.addParm(ErrCodeConstants.API_UNKNOW_PAYTIME, lRet.getErrorCode());
				return lRet;
			}
			CommandRet result = new CommandRet("");
			result.addParm("paytime",lRet.getParm("paytime").getStringColumn());
			Date servertime = new Date();
			result.addParm("servertime", Unit.getString(servertime, "yyyy-MM-dd HH:mm:ss"));
			return result;
	 }


	 /**
	  * 日历运价
	  * @param input
	  * @param context
	  * @return CommandRet
	  */
	public CommandRet scheduleshopping(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		Date begindate = input.getParm("begindate").getDateColumn();
		String depart = input.getParm("depart").getStringColumn();
		String arrive = input.getParm("arrive").getStringColumn();
		String isoCode = input.getParm("isoCode").getStringColumn();

		if(begindate==null){
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		//如果出发日期早于当前日期
		if(begindate.before(Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd"))){
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		if(depart.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, depart)){
			lRet.setError(ErrCodeConstants.API_ORICODE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
			return lRet;
		}
		if(arrive.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, arrive)){
			lRet.setError(ErrCodeConstants.API_DESTCODE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_DESTCODE_ERROR, language));
			return lRet;
		}

		ArrayList<CommandInputRet> l_list = new ArrayList<CommandInputRet>();
		for(int i =0; i < 7;i++){
			CommandInputRet l_item = new CommandInputRet("com.cares.sh.shopping.lowerpricequery");
			l_item.addParm("depart", depart);
			l_item.addParm("arrive", arrive);
			l_item.addParm("isoCode", isoCode);
			l_item.addParm("departdate", getNextDay(begindate,i-3));
			//  Y 只要最低价   N 要查询当天详细的航班信息
			if(i==3){
				l_item.addParm("onlylowprice", "N");
			}else{
				l_item.addParm("onlylowprice", "Y");
			}
			l_item.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_list.add(l_item);
		}
		context.doOther(l_list);
		Table l_tab = new Table(new String[]{"flightdate","price","flights"});
		for(CommandInputRet item :l_list){
			Row lRow = l_tab.addRow();
			if(item.getRet().isError()){
				String err = item.getRet().getErrorCode();
				//2014 没有航班记录
				if(err.equals("2014")){
					lRow.addColumn("flightdate", item.getParm("departdate").getStringColumn());
					lRow.addColumn("price", "-1");
					lRow.addColumn("flights","");
					continue;
				}else{
					//2009缓存记录不存在   2019规则执行出错
					lRet.setError(item.getRet().getErrorCode(), item.getRet().getErrorDesc());
					return lRet;
				}
			}
			lRow.addColumn("flightdate", item.getRet().getParm("date").getStringColumn());
			lRow.addColumn("price", item.getRet().getParm("price").getStringColumn());
			lRow.addColumn("flights", item.getRet().getParm("flights").getTableColumn());
		};
		lRet.addParm("rates", l_tab);

		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
//			String type=LOOK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}

		return lRet;
	}

	 /**
	  * 日历运价 json
	  * @param input
	  * @param context
	  * @return CommandRet
	  */
	 /*public CommandRet scheduleshopping(CommandData input,SelvetContext<ApiContext> context){
			CommandRet lRet = new CommandRet("");
			Date begindate = input.getParm("begindate").getDateColumn();
			String depart = input.getParm("depart").getStringColumn();
			String arrive = input.getParm("arrive").getStringColumn();
			String isoCode = input.getParm("isoCode").getStringColumn();
			if(begindate==null){
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}
			//如果出发日期早于当前日期
			if(begindate.before(Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd"))){
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}
			if(depart.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, depart)){
				lRet.setError(ErrCodeConstants.API_ORICODE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
				return lRet;
			}
			if(arrive.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, arrive)){
				lRet.setError(ErrCodeConstants.API_DESTCODE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_DESTCODE_ERROR, language));
				return lRet;
			}
			CommandInputRet l_item = new CommandInputRet("com.cares.sh.shopping.lowerpricequery");
				l_item.addParm("depart", depart);
				l_item.addParm("arrive", arrive);
				l_item.addParm("isoCode", isoCode);
				l_item.addParm("departdate", begindate);
				l_item.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			lRet = context.doOther(l_item, false);
			if(lRet.isError()){
				lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
				return lRet;
			}
			Table pricesTable = lRet.getParm("").getTableColumn();
			Table l_tab = new Table(new String[]{"flightdate","price","flights"});
			for(Row item :pricesTable){
				Row lRow = l_tab.addRow();
				lRow.addColumn("flightdate", item.getColumn("date").getStringColumn());
				lRow.addColumn("price", item.getColumn("price").getStringColumn());
				lRow.addColumn("flights","");
			}
			lRet.addParm("rates", l_tab);
			//对B2C访问次数进行计数（XML请求/JSON请求）
			if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
				String type=LOOK;
				RedisUtil redisUtil =new RedisUtil();
				redisUtil.docount(type, context);
			}
			return lRet;
		}*/


	 /**
	  * 日历运价 NDC
	  * @param input
	  * @param context
	  * @return CommandRet
	  */
	 public CommandRet scheduleshoppingNDC(CommandData input,SelvetContext<ApiContext> context){
			CommandRet lRet = new CommandRet("");
			final Date begindate = input.getParm("begindate").getDateColumn();
			final String depart = input.getParm("depart").getStringColumn();
			 final String arrive = input.getParm("arrive").getStringColumn();
			 final String isoCode = input.getParm("isoCode").getStringColumn();
			final String adt = input.getParm("ADT").getStringColumn();
			final String inf = input.getParm("INF").getStringColumn();
			final String chd = input.getParm("CHD").getStringColumn();
			final String tktdeptid = input.getParm("tktdeptid").getStringColumn();
			if(begindate==null){
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}
			//如果出发日期早于当前日期
			if(begindate.before(Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd"))){
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}
			if(depart.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, depart)){
				lRet.setError(ErrCodeConstants.API_ORICODE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
				return lRet;
			}
			if(arrive.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, arrive)){
				lRet.setError(ErrCodeConstants.API_DESTCODE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_DESTCODE_ERROR, language));
				return lRet;
			}
			String redisKey = input.getParm("redisKey").getStringColumn();
			String redisValue = RedisManager.getManager().get(redisKey);
			if (redisValue!=null&&!"".equals(redisValue)) {
				lRet.addParm("LCC_PRICECALENDAR_SERVICE", redisValue);
				return lRet;
			}else {
				final SelvetContext<ApiContext> mergeableContext=context;
				MergeableCallWorker<CommandRet> mcw = new MergeableCallWorker<CommandRet>(redisKey, new Callable<CommandRet>() {
					@Override
					public CommandRet call() throws Exception {
						CommandRet result = new CommandRet("");
						CommandInputRet l_item = new CommandInputRet("com.cares.sh.shopping.sevendaysquery");
						l_item.addParm("adt", adt);
						l_item.addParm("inf", inf);
						l_item.addParm("chd", chd);
						l_item.addParm("depart", depart);
						l_item.addParm("arrive", arrive);
						l_item.addParm("isoCode", isoCode);
						SimpleDateFormat adf = new SimpleDateFormat("yyyyMMdd");
						l_item.addParm("departdate", adf.format(begindate));
						l_item.addParm("ticketdeptid", tktdeptid);
						result = mergeableContext.doOther(l_item, false);
						return result;
					}
				});
				//调用匿名内部类中方法
				lRet = mcw.getResultNoThrowErr();
				
			}
			if(lRet.isError()){
				lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
				return lRet;
			}
			Table pricesTable = lRet.getParm("prices").getTableColumn();
			Table l_tab = new Table(new String[]{"flightdate","price","flights"});
			for(Row item :pricesTable){
				Row lRow = l_tab.addRow();
				lRow.addColumn("flightdate", item.getColumn("date").getStringColumn());
				lRow.addColumn("price", item.getColumn("price").getStringColumn());
				lRow.addColumn("flights","");
			}
			lRet.addParm("rates", l_tab);

			//对B2C访问次数进行计数（XML请求/JSON请求）
//			if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
//				String type=LOOK;
//				RedisUtil redisUtil =new RedisUtil();
//				redisUtil.docount(type, context);
//			}
			return lRet;
		}
	 /**
	  * 30天日历 NDC
	  * @param input
	  * @param context
	  * @return CommandRet
	  */
	 public CommandRet thirtyShoppingNDC(CommandData input,SelvetContext<ApiContext> context){
			CommandRet lRet = new CommandRet("");
			final Date begindate = input.getParm("begindate").getDateColumn();
			final String depart = input.getParm("depart").getStringColumn();
			final String arrive = input.getParm("arrive").getStringColumn();
			final String tktdeptid = input.getParm("tktdeptid").getStringColumn();
			if(begindate==null){
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}
			//如果出发日期早于当前日期
			/*if(begindate.before(Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd"))){
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}*/
			//时间上限校验
		    int oriDate = Integer.parseInt(DateUtils.getInstance().formatDate(begindate, "yyyy"));
			if (oriDate>2100||oriDate<2018) {
				lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
				return lRet;
			}
			
			if(depart.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, depart)){
				lRet.setError(ErrCodeConstants.API_ORICODE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
				return lRet;
			}
			if(arrive.length()!=PLACELENGTH || !Pattern.matches(REG_ENGLISH, arrive)){
				lRet.setError(ErrCodeConstants.API_DESTCODE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_DESTCODE_ERROR, language));
				return lRet;
			}
			//拼接redis的key
			String date = DateUtils.getInstance().formatDate(begindate, "yyyy-MM");
			String redisKey=RedisNamespaceEnum.api_service_schedule.code()+":"+depart+"-"+arrive+"-"+date;
			String redisValue = RedisManager.getManager().get(redisKey);
			if (redisValue!=null&&!"".equals(redisValue)) {
				lRet.addParm("LCC_THIRTYCALENDAR_SERVICE", redisValue);
				return lRet;
			}else {
				final SelvetContext<ApiContext> mergeableContext=context;
				MergeableCallWorker<CommandRet> mcw = new MergeableCallWorker<CommandRet>(redisKey, new Callable<CommandRet>() {
					@Override
					public CommandRet call() throws Exception {
						CommandRet result = new CommandRet("");
						CommandInputRet l_item = new CommandInputRet("com.cares.sh.shopping.av.calendar");
					    //出发地
						l_item.addParm("oriCode", depart);
						//目的地
						l_item.addParm("destCode", arrive);
						SimpleDateFormat adf = new SimpleDateFormat("yyyy-MM-dd");
						l_item.addParm("flightDate", adf.format(begindate));
						l_item.addParm("ticketdeptid", tktdeptid);
						result = mergeableContext.doOther(l_item, false);
						return result;
					}
				});
				//调用匿名内部类中方法
				lRet = mcw.getResultNoThrowErr();
				 
			}
			if(lRet.isError()){
				lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
				return lRet;
			}
			//对B2C访问次数进行计数（XML请求/JSON请求）
//			if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
//				String type=LOOK;
//				RedisUtil redisUtil =new RedisUtil();
//				redisUtil.docount(type, context);
//			}
			return lRet;
		}

	/**
	 * 座位查询
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet seatshopping(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput tInput = new CommandInput("com.travelsky.quick.shop.seatmap");
		input.copyTo(tInput);
		lRet = context.doOther(tInput, false);
		//座位详情未知异常
		if(lRet.isError()){
			lRet.addParm(ErrCodeConstants.API_UNKNOW_SEAT_DETAIL, lRet.getErrorCode());
			return lRet;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
//			String type=LOOK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		//得到座位图
		CommandData data = lRet.getParm("seatmap").getObjectColumn();
		CommandData seatMap = data.getParm("AB").getObjectColumn();
		Table refundRulesTable = lRet.getParm("refundRules").getTableColumn();
		if (seatMap != null) {
			//座位详细信息
			Table seatMapTable = seatMap.getParm("seatMap").getTableColumn();
			Table layoutTable = seatMap.getParm("layout").getTableColumn();
			Table seatOffersTable = seatMap.getParm("seatOffers").getTableColumn();
			lRet.addParm("seatMap", seatMapTable);
			lRet.addParm("layout", layoutTable);
			lRet.addParm("seatOffers", seatOffersTable);
		}
		lRet.addParm("AB", seatMap);
		lRet.removeParm("seatmap");
		lRet.addParm("refundRules", refundRulesTable);
		return lRet;
	}



	/**
	 * 辅营 带参数
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet productshopping(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		//flight下面有airlinecd,flightno,carricd(必填),carriflightno(必填),oricode(必填),destcode(必填),oridate(必填),destdate(必填),
		//oriteminal,destterminal,planestype,stpnum,meal,brandid(必填),brandname(必填),cabin(必填),price(必填)
		String orderno = input.getParm("orderno").getStringColumn();
//		String memberid = input.getParm("memberid").getStringColumn();
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
		lRet = context.doOther(lInput, false);
		if (lRet.isError()) {
			//没有订单！
			lRet.setError(ErrCodeConstants.API_ORDER_IS_NULL,
					TipMessager.getMessage(ErrCodeConstants.API_ORDER_IS_NULL, language));
			return lRet;
		}
		String isoCode = lRet.getParm("isoCode").getStringColumn();
		Table lFlights = lRet.getParm("flights").getTableColumn();
		Table lResult = new Table(new String[]{"flightid","airlinecd","oricode","destcode","oridate","destdate",
				"flightno","routtype","products","oriname","destname","oriTime","destTime"});
		CommandRet result = new CommandRet("");
		result.addParm("productlist", lResult);
		String language = ApiServletHolder.getApiContext().getLanguage();
		if(lFlights != null && lFlights.getRowCount() > 0){
			for(int i = 0 ; i < lFlights.getRowCount() ; i++){
				Row lRow = lFlights.getRow(i);
				String oricode = lRow.getColumn("oricode").getStringColumn();
				String destcode = lRow.getColumn("destcode").getStringColumn();
				String oridate = lRow.getColumn("oridate").getStringColumn();
				String destdate = lRow.getColumn("destdate").getStringColumn(); 
				String oriTime = lRow.getColumn("oriTime").getStringColumn();
				String destTime = lRow.getColumn("destTime").getStringColumn();
				String airlinecd = lRow.getColumn("airlinecd").getStringColumn();
				String flightid = lRow.getColumn("id").getStringColumn();
				String routtype = lRow.getColumn("routtype").getStringColumn();
				String flightno = lRow.getColumn("flightno").getStringColumn();
				CommandData l_data = this.getProductShoppingInput(lRow,lRet);
				if(lRet.isError()){
					return lRet;
				}
				CommandInput tInput = new CommandInput("com.cares.sh.shopping.productsquery");
				tInput.addParm("flight", l_data);
				tInput.addParm("isoCode", isoCode);
				lInput.addParm("memberid", memberid);
				tInput.addParm("sysdate", new Date());
				lInput.addParm("channelid", input.getParm("channelid").getStringColumn());
				lRet = context.doOther(tInput, false);
				//对B2C访问次数进行计数（XML请求/JSON请求）
//				if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
//					String type=LOOK;
//					RedisUtil redisUtil =new RedisUtil();
//					redisUtil.docount(type, context);
//				}
				if(lRet.isError()){
					//获取辅营信息失败
					lRet.addParm(ErrCodeConstants.API_UNKNOW_AUXILIARY_QUERY,
							TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_AUXILIARY_QUERY, language));
					return lRet;
				}
				Table lProducts = lRet.getParm("products").getTableColumn();
				Table products = new Table(new String[]{"code","typecode","name","remark","maxnum","price","refunded",
						"currencyCode","atime","ssrtype","display"});
				if(lProducts == null || lProducts.getRowCount() < 1){
					//没有可销售辅营
					lRet.addParm(ErrCodeConstants.API_SETSUBMARKET_IS_NULL,
							TipMessager.getMessage(ErrCodeConstants.API_SETSUBMARKET_IS_NULL, language));
					return lRet;
				}
				for(int j = 0 ; j < lProducts.getRowCount() ; j++){
					Row rRow = lProducts.getRow(j);
					String status = rRow.getColumn("status").getStringColumn();
					if(status.equals(Constant.Submarket.IsOk)){
						Row row = products.addRow();
						row.addColumn("code", rRow.getColumn("code").getStringColumn());
						row.addColumn("typecode", rRow.getColumn("typecode").getStringColumn());
						CommandData data = rRow.getColumn("name").getObjectColumn();
						String name="";
						if (data != null) {
							 name = data.getParm(language).getStringColumn();
						}
						//单价
						row.addColumn("name", name);
						row.addColumn("remark", rRow.getColumn("remark").getStringColumn());
						row.addColumn("maxnum", rRow.getColumn("maxnum").getStringColumn());
						row.addColumn("price", rRow.getColumn("price").getStringColumn());
						CommandData objectColumn = rRow.getColumn("otherinfos").getObjectColumn();
						row.addColumn("atime", objectColumn.getParm("atime").getStringColumn());
						CommandData refunded = rRow.getColumn("refunded").getObjectColumn();
						row.addColumn("refunded", refunded==null?"":refunded.toString());
						row.addColumn("currencyCode", rRow.getColumn("currencyCode").getStringColumn());
						row.addColumn("ssrtype", rRow.getColumn("ssrtype").getStringColumn());
						row.addColumn("display", rRow.getColumn("display").getStringColumn());
					}
				}
				//将shopping结果放到结果集里
				Row tRow = lResult.addRow();
				tRow.addColumn("flightid", flightid);
				tRow.addColumn("airlinecd", airlinecd);
				tRow.addColumn("oricode", oricode);
				tRow.addColumn("destcode", destcode);
				tRow.addColumn("oridate", oridate);
				tRow.addColumn("destdate", destdate);
				tRow.addColumn("flightno", flightno);
				tRow.addColumn("routtype", routtype);
				tRow.addColumn("products", products);
				tRow.addColumn("oriTime", oriTime);
				tRow.addColumn("destTime", destTime);
				String oriname = lRow.getColumn("oriname").getStringColumn();
				String destname = lRow.getColumn("destname").getStringColumn();
				tRow.addColumn("oriname", oriname);
				tRow.addColumn("destname", destname);
			}
		}
		return result;
	}


	/**
	 * 辅营 不带参数
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet query(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput tInput = new CommandInput("com.cares.sh.shopping.products.service.query");
		lRet = context.doOther(tInput, false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if ("".equals(lRet.getErrorCode())||null==lRet.getErrorCode()) {
//			String type=LOOK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		if(lRet.isError()){
			//获取辅营信息失败
			lRet.addParm(ErrCodeConstants.API_UNKNOW_AUXILIARY_QUERY,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_AUXILIARY_QUERY, language));
			return lRet;
		}
		Table products = new Table(new String[]{"code","typecode","name","remark","maxnum","price","refunded",
				"currencyCode","atime","ssrtype","display"});
		CommandRet result = new CommandRet("");
		result.addParm("products", products);
		Table lProducts = lRet.getParm("products").getTableColumn();
		if(lProducts == null || lProducts.getRowCount() < 1){
			//没有可销售辅营
			lRet.addParm(ErrCodeConstants.API_SETSUBMARKET_IS_NULL,
					TipMessager.getMessage(ErrCodeConstants.API_SETSUBMARKET_IS_NULL, language));
			return lRet;
		}
		for(int j = 0 ; j < lProducts.getRowCount() ; j++){
			Row rRow = lProducts.getRow(j);
			Row row = products.addRow();
			row.addColumn("code", rRow.getColumn("code").getStringColumn());
			row.addColumn("typecode", rRow.getColumn("typecode").getStringColumn());
			row.addColumn("name", rRow.getColumn("name").getStringColumn());
			row.addColumn("remark", rRow.getColumn("remark").getStringColumn());
			row.addColumn("maxnum", rRow.getColumn("maxnum").getStringColumn());
			row.addColumn("price", rRow.getColumn("price").getStringColumn());
			CommandData objectColumn = rRow.getColumn("otherinfos").getObjectColumn();
			row.addColumn("atime", objectColumn.getParm("atime").getStringColumn());
			CommandData refundCmdData = rRow.getColumn("refunded").getObjectColumn();
			row.addColumn("refunded", refundCmdData==null?"":refundCmdData.toString());
			row.addColumn("currencyCode", rRow.getColumn("currencyCode").getStringColumn());
			row.addColumn("ssrtype", rRow.getColumn("ssrtype").getStringColumn());
			row.addColumn("display", rRow.getColumn("display").getStringColumn());
		}
		return result;
	}

	/**
	 * 退辅营
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet refundproduct(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		Table paxs = input.getParm("paxs").getTableColumn();
		String orderno = input.getParm("orderno").getStringColumn();
		//订单号是否为空
		if(orderno.isEmpty()){
			lRet.setError(ErrCodeConstants.API_NULL_ORDER_NO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ORDER_NO, language));
			return lRet;
		}
		if(null == paxs || "".equals(paxs)){
			//辅营信息不能为空
			lRet.addParm(ErrCodeConstants.API_NULL_SUBMARKET,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SUBMARKET, language));
			return lRet;
		}
		CommandInput tInput = new CommandInput("com.cares.sh.order.inu.refund");
		tInput.addParm("orderno", orderno);
		tInput.addParm("paxs", paxs);
		tInput.addParm("mode", input.getParm("type").getTableColumn());
		tInput.addParm("cancelflag", "0");
		lRet = context.doOther(tInput, false);
		return lRet;
	}

	/**
	 * 组织辅营shopping入参
	 * @param lRow
	 * @param lRet
	 * @return
	 */
	private CommandData getProductShoppingInput(Row lRow,CommandRet lRet){
		String airlinecd = lRow.getColumn("airlinecd").getStringColumn();
		String flightno = lRow.getColumn("flightno").getStringColumn();
		String carricd = lRow.getColumn("carricd").getStringColumn();
		String carriflightno = lRow.getColumn("carriflightno").getStringColumn();
		String oricode = lRow.getColumn("oricode").getStringColumn();
		String destcode = lRow.getColumn("destcode").getStringColumn();
		String oridate = lRow.getColumn("oridate").getStringColumn();
		String oriDateTime = lRow.getColumn("oriDateTime").getStringColumn();
		String destdate = lRow.getColumn("destdate").getStringColumn();
		String destDateTime = lRow.getColumn("destDateTime").getStringColumn();
		String oriteminal = lRow.getColumn("oriterminal").getStringColumn();
		String destterminal = lRow.getColumn("destterminal").getStringColumn();
		String planestype = lRow.getColumn("planestype").getStringColumn();
		String stpnum = lRow.getColumn("stopnum").getStringColumn();
		String meal = lRow.getColumn("hasmeal").getStringColumn();
		String brandid = lRow.getColumn("familycode").getStringColumn();
		CommandData familyData = lRow.getColumn("familyname").getObjectColumn();
		String brandname = familyData.getParm(language).getStringColumn();
//		String brandname = lRow.getColumn("familyname").getStringColumn();
		String cabin = lRow.getColumn("cabin").getStringColumn();
		String price = lRow.getColumn("familyprice").getStringColumn();
		if(carricd.isEmpty()){
			//订单异常,没有carricd
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_CARRICD,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_CARRICD, language));
		}
		if(carriflightno.isEmpty()){
			//订单异常,没有carriflightno
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_CARRIFLIGHTNO,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_CARRIFLIGHTNO, language));
		}
		if(oricode.isEmpty()){
			//订单异常,没有oricode
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_ORICODE,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_ORICODE, language));
		}
		if(destcode.isEmpty()){
			//订单异常,没有destcode
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_DESTCODE,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_DESTCODE, language));
		}
		if(oridate.isEmpty()){
			//订单异常,没有oridate
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_ORIDATE,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_ORIDATE, language));
		}
		if(destdate.isEmpty()){
			//订单异常,没有destdate
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_DESTDATE,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_DESTDATE, language));
		}
		if(brandid.isEmpty()){
			//订单异常,没有familycode
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_FAMILYCODE,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_FAMILYCODE, language));
		}
		if(brandname.isEmpty()){
			//订单异常,没有familyname
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_FAMILYNAME,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_FAMILYNAME, language));
		}
		if(cabin.isEmpty()){
			//订单异常,没有cabin
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_CABIN,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_CABIN, language));
		}
		if(price.isEmpty()){
			//订单异常,没有familyprice
			lRet.cleanAll();
			lRet.setError(ErrCodeConstants.API_UNKNOW_FAMILYPRICE,
					TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_FAMILYPRICE, language));
		}

		CommandData tData = new CommandData();
		if(!lRet.isError()){
			tData.addParm("airlinecd", airlinecd);
			tData.addParm("flightno", flightno);
			tData.addParm("carricd", carricd);
			tData.addParm("carriflightno", carriflightno);
			tData.addParm("oricode", oricode);
			tData.addParm("destcode", destcode);
			tData.addParm("oridate", oridate);
			tData.addParm("oriDateTime", oriDateTime);
			tData.addParm("destdate", destdate);
			tData.addParm("destDateTime", destDateTime);
			tData.addParm("oriteminal", oriteminal);
			tData.addParm("destterminal", destterminal);
			tData.addParm("planestype", planestype);
			tData.addParm("stpnum", stpnum);
			tData.addParm("meal", meal);
			tData.addParm("brandid", brandid);
			tData.addParm("brandname", brandname);
			tData.addParm("cabin", cabin);
			tData.addParm("price", price);
			tData.addParm("oriTimeZone", lRow.getColumn("oriTimeZone").getStringColumn());
			tData.addParm("destTimeZone", lRow.getColumn("destTimeZone").getStringColumn());
			tData.addParm("flightDay", lRow.getColumn("flightDay").getStringColumn());
		}

		return tData;
	}

	/**
	 * 运价查询
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet shopping(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		//航班号
		String flightno = input.getParm("flightno").getStringColumn();
		//航空公司cod
		String code = input.getParm("airlineCode").getStringColumn();
		//航班后缀
		String suffix = input.getParm("suffix").getStringColumn();
		Date lDepartdate = input.getParm("deptdate").getDateColumn();
		String lDepart = input.getParm("depart").getStringColumn();
		String lArrive = input.getParm("arrive").getStringColumn();
		Item lAdtItem = input.getParm("adt");
		Item lInfItem=input.getParm("inf");
		Item lChdItem=input.getParm("chd");
		int lChd = StringUtils.hasLength(lChdItem.getStringColumn())? lChdItem.getIntegerColumn() : 0;
		int lAdt = StringUtils.hasLength(lAdtItem.getStringColumn())? lAdtItem.getIntegerColumn() : 0;
		int lInf = StringUtils.hasLength(lInfItem.getStringColumn())? lInfItem.getIntegerColumn() : 0;
		int lSeat = lAdt+lChd;
		if(lInf > lAdt || lInf + lChd > lAdt * 2){
			//1 名成人旅客只能携带 2 名儿童或同时携带 1 名儿童和 1 名婴儿
			lRet.setError(ErrCodeConstants.API_PRICE_CALENDAR_NUM,
					TipMessager.getMessage(ErrCodeConstants.API_PRICE_CALENDAR_NUM, language));
			return lRet;
		}
		if(lDepartdate == null){
			//出发日期不是有效格式！
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		//转时间格式
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			lDepartdate = sdf.parse(sdf.format(lDepartdate));
		} catch (ParseException e) {
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		if(lDepartdate.before(Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd"))){
			//出发日期有误
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		if(lDepart.length() != 3 || !Pattern.matches(REG_ENGLISH, lDepart)){
			//出发地不是三字码！
			lRet.setError(ErrCodeConstants.API_ORICODE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
			return lRet;
		}
		if(lArrive.length() != 3 || !Pattern.matches(REG_ENGLISH, lArrive)){
			//目的地不是三字码
			lRet.setError(ErrCodeConstants.API_ARRIVE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ARRIVE_ERROR, language));
			return lRet;
		}
		if(lAdt < 1){
			//成人数量不能为空
			lRet.setError(ErrCodeConstants.API_NULL_ADT_NUM,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ADT_NUM, language));
			return lRet;
		}
		if(lSeat < 1){
			//至少需要一人出行！
			lRet.setError(ErrCodeConstants.API_NULL_PAXS,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_PAXS, language));
			return lRet;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.shopping.brandinfosquery");
		lInput.addParm("airlineCode", code);
		lInput.addParm("flightNo", flightno);
		lInput.addParm("suffix", suffix);
		lInput.addParm("depart", lDepart);
		lInput.addParm("arrive", lArrive);
		lInput.addParm("departdate", Unit.getString(lDepartdate));
		lInput.addParm("channel", context.getContext().getChannelNo());
		lInput.addParm("adt", lAdt);
		lInput.addParm("chd", lChd);
		lInput.addParm("inf", lInf);
		lInput.addParm("ticketdeptid", context.getContext().getTicketDeptid());
		lInput.addParm("isoCode", input.getParm("isoCode").getStringColumn());
		lInput.addParm("oriCodeType", input.getParm("oriCodeType").getStringColumn());
		lInput.addParm("destCodeType", input.getParm("destCodeType").getStringColumn());
        lInput.addParm("cabinClass", input.getParm("cabinClass").getStringColumn());
        
        lInput.addParm("cabin", input.getParm("cabin").getStringColumn());
        lInput.addParm("direct", input.getParm("direct").getStringColumn());
        lInput.addParm("nonStop", input.getParm("nonStop").getStringColumn());
        lInput.addParm("connectionCode", input.getParm("connectionCode").getStringColumn());
        lInput.addParm("connectionNum", input.getParm("connectionNum").getStringColumn());

		lInput.addParm("isnew", "Y");
		lRet =  doShopping(lInput,false);
        if ("".equals(lRet)||lRet==null) {
        	 lRet.setError(ErrCodeConstants.API_NULL_SHOPPINGRESULT,
     				TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_SHOPPINGRESULT,
     						ApiServletHolder.getApiContext().getLanguage()));
		}
		//对B2C访问次数进行计数（XML请求|JSON请求）
//		if (!lRet.isError()) {
//			String type=LOOK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}
	
	private CommandRet doShopping(CommandInput lInput, boolean b) {
		CommandRet ret= new CommandRet(lInput.getId());
		String oriCodeType=lInput.getParm("oriCodeType").getStringColumn();
		String destCodeType=lInput.getParm("destCodeType").getStringColumn();
		String departdate=lInput.getParm("departdate").getStringColumn();
		String isoCode=lInput.getParm("isoCode").getStringColumn();
		String arrive=lInput.getParm("arrive").getStringColumn();
		String depart=lInput.getParm("depart").getStringColumn();
		String adt=lInput.getParm("adt").getStringColumn();
		String chd=lInput.getParm("chd").getStringColumn();
		String inf=lInput.getParm("inf").getStringColumn();
		//获取该航班成人价格
		int adultPrice = getAdultPrice(depart+arrive,isoCode);
		totalprice=adultPrice*Integer.parseInt(adt)+(adultPrice/2)*Integer.parseInt(chd)+(adultPrice/4)*Integer.parseInt(inf);
		adultPrice=adultPrice;
		chdprice=adultPrice/2;
        infprice=adultPrice/4;
        isCode=isoCode;
        refundid=getRandomData(10000, 2000);
		ret.addParm("ruledate", String.valueOf(new Date().getTime()));
		getFlight(lInput,ret);
		ret.addParm("price", totalprice);
		ret.addParm("totalNoCharge", totalprice);
		ret.addParm("minhour", "2");
		ret.addParm("currencyCode",isoCode);
		ret.addParm("totalprice", "totalprice");
		ret.addParm("isGroup", "0");
		getRefundList(ret);
		ret.addParm("display", display);
		ret.addParm("isUmnr", "0");
		return ret;
	}


	private void getRefundList( CommandRet ret) {
		CommandData refund=new CommandData();
		CommandData refundlist=new CommandData();
		refundlist.addParm("currencySign",display);
		String []infoarray=new String[]{"time","price","lowerprice"};
		Table infchangeinfo = new Table(infoarray);
		Row inf1 = infchangeinfo.addRow();
		Row inf2 = infchangeinfo.addRow();
	    addinf(inf1,inf2,"1");
		refundlist.addParm("infchangeinfo", infchangeinfo);
		refundlist.addParm("currencyCode", isCode);
		Table refundedinfo = new Table(infoarray);
		Row rinf1 = infchangeinfo.addRow();
		Row rinf2 = infchangeinfo.addRow();
	    addinf(rinf1,rinf2,"2");
		refundlist.addParm("refundedinfo", refundedinfo);
		refundlist.addParm("status", "1");
		refundlist.addParm("calcbase", "P");
		refundlist.addParm("refundedflag", "1");
		refundlist.addParm("chdrefundedinfo", refundedinfo);
		refundlist.addParm("id", refundid);
		refundlist.addParm("rate", "2");
		refundlist.addParm("changeflag", "1");
		refundlist.addParm("name", getName("shoppingtest", "shoppingtest", "shoppingtest"));
		refundlist.addParm("chdchangeinfo", infchangeinfo);
		refundlist.addParm("changeinfo", infchangeinfo);
		refundlist.addParm("infrefundedinfo", "refundedinfo");
		refund.addParm(String.valueOf(refundid), refundlist);
		ret.addParm("refundList", refund);
	}


	private void getFlight(CommandInput lInput, CommandRet ret) {
		Table fligths = new Table(fligtharr);
		Row row = fligths.addRow();
		//起飞时间
		int oritime=getRandomData(12,0);
		String oriTimeS=oritime+":00";
		//飞行小时
		int orihour=getRandomData(12,0);
		row.addColumn("travelTime", String.valueOf(orihour*60));
		//出发时间
		String oridata=lInput.getParm("departdate").getStringColumn();
		String oridata2=oridata.substring(0, 4)+"-"+oridata.substring(4, 6)+"-"+oridata.substring(6, 8);
		row.addColumn("chdValid", "1");
		row.addColumn("oridate", oridata+" "+oriTimeS);
		//到达三字码
		String destCodeType=lInput.getParm("destCodeType").getStringColumn();
		row.addColumn("destcitycode", destCodeType);
		row.addColumn("oriTime", oriTimeS);
		row.addColumn("invVersion", "");
		String desttime=(orihour+oritime)+":00";
		String destDateTime=oridata2+desttime+"+8:00";
		row.addColumn("destDateTime",destDateTime);
		CommandData oriname=new CommandData();
		//出发地三字码
		String oriCodeType=lInput.getParm("oriCodeType").getStringColumn();
		oriname.addParm("en_US", Utils.getenUSName(oriCodeType)+"Airport");
		oriname.addParm("zh_CN", Utils.getzhCNName(oriCodeType)+"机场");
		row.addColumn("oriname", oriname);
		row.addColumn("airlinecd", "ZA");
		row.addColumn("destDay", oridata);
		row.addColumn("mileage", getRandomData(500,300));
		row.addColumn("flightRoute", "D");
		String isoCode=lInput.getParm("isoCode").getStringColumn();
		display= Utils.getisoCode(isoCode);
		row.addColumn("display", Utils.getisoCode(isoCode));
		row.addColumn("ssrs", "");
		row.addColumn("infValid", "1");
		CommandData destname=new CommandData();
		oriname.addParm("en_US", Utils.getenUSName(destCodeType)+"Airport");
		oriname.addParm("zh_CN", Utils.getzhCNName(destCodeType)+"机场");
		row.addColumn("destname", destname);
		row.addColumn("destterminal", "");
		row.addColumn("destcountry", destCodeType);
		row.addColumn("y100price", "100.00");
		row.addColumn("cabin", "");
		row.addColumn("dayChange", "1");
		String oriDateTime=oridata2 +desttime+"8:00";
		row.addColumn("oriDateTime",oriDateTime);
		row.addColumn("oriDay", oridata);
		row.addColumn("carrisuffix", "");
		row.addColumn("carricd", "ZA");
		String destdate=oridata+(orihour+oritime)+":00";
		row.addColumn("destdate", destdate);
		int flightno=getRandomData(500, 300);
		row.addColumn("carriflightno", flightno);
		row.addColumn("weightmode", "");
		getBrandinfos(row);
		row.addColumn("passby", new Table());
		row.addColumn("destTimeZone", "+8:00");
		row.addColumn("meal", "N");
		row.addColumn("currencyCode", isCode);
		row.addColumn("precision", "");
		row.addColumn("weightUnit", "KG");
		row.addColumn("destTime", desttime);
		row.addColumn("oricode",oriCodeType);
		row.addColumn("airlineCountry", "CN");
		row.addColumn("oricityname", getName("", Utils.getenUSName(oriCodeType), Utils.getzhCNName(oriCodeType)));
		row.addColumn("destcode", destCodeType);
		row.addColumn("oricitycode", Utils.getzhCNName(oriCodeType));
		row.addColumn("oriTimeZone", "+8：00");
		row.addColumn("stpnum", "0");
		row.addColumn("planestype", "CCC");
		row.addColumn("destcityname", getName("", Utils.getenUSName(destCodeType), Utils.getzhCNName(destCodeType)));
		getLegs(row);
		row.addColumn("flightid",  getRandomData(10000, 1000));
		row.addColumn("flightno", flightno);
		ret.addParm("flights", fligths);
	}

private void getLegs(Row flightrow) {
	Table legs = new Table(new String[]{"ckiStatus","destTerminalCode","oriTimezone","flightLegName",
			"emlock","destTime","destDate","oriCode","oriTime",
			"oriName","destTimezone","destDayChange","destName","flightSuffix","flightNo",
			"airlineCode","invVersion","oriTerminalCode","oriDate","overbook","destCode","oriDayChange","legId",
			"flightLeg"});
	Row row = legs.addRow();
	row.addColumn("ckiStatus", "");
	row.addColumn("destTerminalCode", "");
	row.addColumn("oriTimezone", "+8:00");
	String oricode = flightrow.getColumn("oricode").getStringColumn();
	String destcode = flightrow.getColumn("destcode").getStringColumn();
	row.addColumn("flightLegName", oricode+"-"+destcode);
	row.addColumn("emlock", "0");
	row.addColumn("destTime",  flightrow.getColumn("destTime").getStringColumn());
	String destDay = flightrow.getColumn("destDay").getStringColumn();
	String destDate=destDay.substring(0, 4)+"-"+destDay.substring(4, 6)+"-"+destDay.substring(6, 8);
	row.addColumn("destDate", destDate);
	row.addColumn("oriCode",oricode);
	row.addColumn("oriTime", flightrow.getColumn("oriTime").getStringColumn());
	row.addColumn("oriName", flightrow.getColumn("oriName").getObjectColumn());
	row.addColumn("destTimezone", flightrow.getColumn("destTimezone").getStringColumn());
	row.addColumn("destName", flightrow.getColumn("destName").getStringColumn());
	row.addColumn("flightNo", flightrow.getColumn("carriflightno").getStringColumn());
	row.addColumn("destDayChange", "1");
	row.addColumn("flightSuffix", "");
	row.addColumn("airlineCode", "ZA");
	row.addColumn("invVersion", "0");
	row.addColumn("oriTerminalCode", "");
	row.addColumn("oriDate", destDate);
	row.addColumn("overbook", "0");
	row.addColumn("destCode", destcode);
	row.addColumn("oriDayChange", "0");
	row.addColumn("legId", getRandomData(10000, 1000));
	row.addColumn("flightLeg", "1");
	flightrow.addColumn("legs", legs);
	}


private void getBrandinfos(Row flightrow) {
	Table brandinfos = new Table(new String[]{"hasMeal","fares","id","freeproduct","describe","name","isseatfree","lowestprice"});
	Row row = brandinfos.addRow();
	row.addColumn("hasMeal", 2);
	getFares(row);
	row.addColumn("id", "Y");
	getFreePro(row);
	row.addColumn("describe", "Flight, Bag, Meal and Insurance");
	row.addColumn("name", getName("Y 전 가격", "Full Economy", "Y 全价"));
	row.addColumn("isseatfree", "N");
	row.addColumn("lowestprice", totalprice);
	flightrow.addColumn("brandinfos",brandinfos);
	}

private CommandData getName(String ko,String en,String zh) {
	CommandData name=new CommandData();
	name.addParm("ko_KR", ko);
	name.addParm("en_US", en);
	name.addParm("zh_CN", zh);
	return name;
	
}
private void getFreePro(Row brandinfosrow) {
	Table freeproduct = new Table(new String[]{"ssrtype","valid","invalidReason","describe","name","attr","ssrinfo","code","type"});
	addFrePro(new String[]{"OTHS","1","","","","WWWW","WWWW","","","","WWWW","INSU"},freeproduct.addRow());
	addFrePro(new String[]{"XXML","0","3","","cc","cc","基础餐食","","","","BBML","XXML"},freeproduct.addRow());
	addFrePro(new String[]{"OTHS","1","","","","Trip Curtailment","行程缩减赔偿","","","","TC","INSU"},freeproduct.addRow());
	addFrePro(new String[]{"OTHS","1","","","bx1","bx1","bx1","","","","BX1","INSU"},freeproduct.addRow());
	addFrePro(new String[]{"XBAG","1","","","","abb","20KG","20","KG","","20KG","XBAG"},freeproduct.addRow());
	brandinfosrow.addColumn("freeproduct", freeproduct);
}


private void addFrePro(String[] value,Row freeprorow) {
	freeprorow.addColumn("ssrtype", value[0]);
	freeprorow.addColumn("valid", value[1]);
	freeprorow.addColumn("invalidReason", value[2]);
	freeprorow.addColumn("describe", value[3]);
	freeprorow.addColumn("name", getName(value[4], value[5], value[6]));
	CommandData attr=new CommandData();
	attr.addParm("weight", value[7]);
	attr.addParm("weightUnit", value[8]);
	freeprorow.addColumn("attr", attr);
	freeprorow.addColumn("ssrinfo", value[9]);
	freeprorow.addColumn("code", value[10]);
	freeprorow.addColumn("type", value[11]);
	
}


private void getFares(Row brandinfosrow) {
	Table fares = new Table(new String[]{"brandPrice","cabin","adultluggage","infCabinRate",
			"rulecode","totalprice","pricecabin","chdCabinRate","adtCabinRate",
			"display","farebasis","babyluggage","basiccabin","chdCabinPrice","cabinnum",
			"fee","noChargePrice","refunded","price","tax","cabinname","chdprice","childluggage",
			"weightmode","infCabinPrice","infprice","basicname","refundid"});
	Row row = fares.addRow();
	row.addColumn("brandPrice", totalprice);
	row.addColumn("cabin", "Y");
	row.addColumn("adultluggage", "20");
	row.addColumn("infCabinRate", "10");
	row.addColumn("rulecode", "");
	row.addColumn("totalprice", totalprice);
	row.addColumn("pricecabin", "100");
	row.addColumn("chdCabinRate", "50");
	row.addColumn("adtCabinRate", "100");
	row.addColumn("display",display);
	row.addColumn("farebasis", "1000");
	row.addColumn("babyluggage", "5");
	row.addColumn("basiccabin", "Y");
	row.addColumn("chdCabinPrice", "50.00");
	row.addColumn("cabinnum", "A");
	row.addColumn("fee", "");
	row.addColumn("noChargePrice", totalprice);
	getRefunded(row);
	row.addColumn("price", totalprice);
	row.addColumn("tax", "");
	CommandData cabinname=new CommandData();
	cabinname.addParm("ko_KR", "shoppingtest");
	cabinname.addParm("en_US", "Economy class");
	cabinname.addParm("zh_CN", "Y舱");
	row.addColumn("cabinname",cabinname);
	row.addColumn("chdprice", chdprice);
	row.addColumn("childluggage", "20");
	row.addColumn("weightmode", "");
	row.addColumn("infCabinPrice", "10.00");
	row.addColumn("infprice", infprice);
	CommandData basicname=new CommandData();
	basicname.addParm("ko_KR", "이코노미 석");
	basicname.addParm("en_US", "Economy class");
	basicname.addParm("zh_CN", "经济舱");
	row.addColumn("basicname", basicname);
	row.addColumn("refundid", refundid);
	brandinfosrow.addColumn("fares",fares);
}


private void getRefunded(Row faresrow) {
	CommandData refunded=new CommandData();
	refunded.addParm("currencySign",display);
	String []infoarray=new String[]{"time","price","lowerprice"};
	Table infchangeinfo = new Table(infoarray);
	Row inf1 = infchangeinfo.addRow();
	Row inf2 = infchangeinfo.addRow();
    addinf(inf1,inf2,"1");
	refunded.addParm("infchangeinfo", infchangeinfo);
	refunded.addParm("currencyCode", isCode);
	Table refundedinfo = new Table(infoarray);
	Row rinf1 = infchangeinfo.addRow();
	Row rinf2 = infchangeinfo.addRow();
    addinf(rinf1,rinf2,"2");
	refunded.addParm("refundedinfo", refundedinfo);
	refunded.addParm("status", "1");
	refunded.addParm("calcbase", "P");
	refunded.addParm("refundedflag", "1");
	//其中数据一致
	refunded.addParm("chdrefundedinfo", refundedinfo);
	
	refunded.addParm("id", refundid);
	refunded.addParm("rate", "2");
	refunded.addParm("changeflag", "1");
	CommandData name=new CommandData();
	name.addParm("ko_KR", "shoppingtest");
	name.addParm("en_US", "shoppingtest");
	name.addParm("zh_CN", "shoppingtest");
	refunded.addParm("name", name);
	refunded.addParm("chdchangeinfo", infchangeinfo);
	refunded.addParm("infrefundedinfo", refundedinfo);
	refunded.addParm("changeinfo", infchangeinfo);
	faresrow.addColumn("refunded", refunded);
}
private void addinf(Row inf1, Row inf2,String key) {
	if ("1".equals(key)) {
		inf1.addColumn("time", "12");
	}else if ("2".equals(key)) {
		inf1.addColumn("time", "24");
	}
	inf1.addColumn("price", "20");
	inf1.addColumn("lowerprice", "20.00");
	inf2.addColumn("time", "-1");
	inf2.addColumn("price", "40");
	inf2.addColumn("lowerprice", "40.00");
}


private int  getRandomData(int key,int range) {
	int pri = new Random().nextInt(key);
	int lastpri=pri+range;
	return lastpri;
}
	private int getAdultPrice(String key,String iso) {
		try {
			if (flightPrice.containsKey(key+iso)) {
				Integer pri = flightPrice.get(key+iso);
				if (pri!=null) {
					return pri;
				}
			}
			int adupri = getRandomData(1000,200);
			if (flightPrice.size()>100) {
				synchronized (flightPrice) {
					flightPrice.clear();
				}
			}else {
				flightPrice.put(key+iso, adupri);
				if (iso.contains("CNY")) {
					flightPrice.put(key+"USD", adupri/8);
				}else {
					flightPrice.put(key+"CNY", adupri*8);
				}
			}
			return adupri;
		} catch (Exception e) {
			flightPrice.put(key, 200);
			return 200;
		}
	}


	/**
	 * 运价查询(FOR ONEE)
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet shoppingForOneE(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		//航班号
		String flightno = input.getParm("flightno").getStringColumn();
		//航空公司cod
		String code = input.getParm("airlineCode").getStringColumn();
		//航班后缀
		String suffix = input.getParm("suffix").getStringColumn();
		Date lDepartdate = input.getParm("deptdate").getDateColumn();
		String lDepart = input.getParm("depart").getStringColumn();
		String lArrive = input.getParm("arrive").getStringColumn();
		Item lAdtItem = input.getParm("adt");
		Item lInfItem=input.getParm("inf");
		Item lChdItem=input.getParm("chd");
		int lChd = StringUtils.hasLength(lChdItem.getStringColumn())? lChdItem.getIntegerColumn() : 0;
		int lAdt = StringUtils.hasLength(lAdtItem.getStringColumn())? lAdtItem.getIntegerColumn() : 0;
		int lInf = StringUtils.hasLength(lInfItem.getStringColumn())? lInfItem.getIntegerColumn() : 0;
		int lSeat = lAdt+lChd;
		if(lInf > lAdt || lInf + lChd > lAdt * 2){
			//1 名成人旅客只能携带 2 名儿童或同时携带 1 名儿童和 1 名婴儿
			lRet.setError(ErrCodeConstants.API_PRICE_CALENDAR_NUM,
					TipMessager.getMessage(ErrCodeConstants.API_PRICE_CALENDAR_NUM, language));
			return lRet;
		}
		if(lDepartdate == null){
			//出发日期不是有效格式！
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		//转时间格式
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			lDepartdate = sdf.parse(sdf.format(lDepartdate));
		} catch (ParseException e) {
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		if(lDepartdate.before(Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd"))){
			//出发日期有误
			lRet.setError(ErrCodeConstants.API_ORIDATE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			return lRet;
		}
		if(lDepart.length() != 3 || !Pattern.matches(REG_ENGLISH, lDepart)){
			//出发地不是三字码！
			lRet.setError(ErrCodeConstants.API_ORICODE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
			return lRet;
		}
		if(lArrive.length() != 3 || !Pattern.matches(REG_ENGLISH, lArrive)){
			//目的地不是三字码
			lRet.setError(ErrCodeConstants.API_ARRIVE_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ARRIVE_ERROR, language));
			return lRet;
		}
		if(lAdt < 1){
			//成人数量不能为空
			lRet.setError(ErrCodeConstants.API_NULL_ADT_NUM,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_ADT_NUM, language));
			return lRet;
		}
		if(lSeat < 1){
			//至少需要一人出行！
			lRet.setError(ErrCodeConstants.API_NULL_PAXS,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_PAXS, language));
			return lRet;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.shopping.brandinfosquery.nopricing");
		lInput.addParm("airlineCode", code);
		lInput.addParm("flightNo", flightno);
		lInput.addParm("suffix", suffix);
		lInput.addParm("depart", lDepart);
		lInput.addParm("arrive", lArrive);
		lInput.addParm("departdate", Unit.getString(lDepartdate));
		lInput.addParm("channel", context.getContext().getChannelNo());
		lInput.addParm("adt", lAdt);
		lInput.addParm("chd", lChd);
		lInput.addParm("inf", lInf);
		lInput.addParm("ticketdeptid", context.getContext().getTicketDeptid());
		lInput.addParm("oriCodeType", input.getParm("oriCodeType").getStringColumn());
		lInput.addParm("destCodeType", input.getParm("destCodeType").getStringColumn());
        lInput.addParm("cabinClass", input.getParm("cabinClass").getStringColumn());
        
        lInput.addParm("cabin", input.getParm("cabin").getStringColumn());
        lInput.addParm("direct", input.getParm("direct").getStringColumn());
        lInput.addParm("nonStop", input.getParm("nonStop").getStringColumn());
        lInput.addParm("connectionCode", input.getParm("connectionCode").getStringColumn());
        lInput.addParm("connectionNum", input.getParm("connectionNum").getStringColumn());

		lInput.addParm("isnew", "Y");
		lRet = context.doOther(lInput,false);

		//对B2C访问次数进行计数（XML请求|JSON请求）
//		if (!lRet.isError()) {
//			String type=LOOK;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return lRet;
	}

	/**
	 * 运价查询ONEE
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet shoppingONEE(CommandData input,SelvetContext<ApiContext> context){
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.cares.sh.order.guanrantee");
		input.copyTo(lInput);
		lRet = context.doOther(lInput,false);
		return lRet;
	}
	

	/**
	 *
	 * @param date 当前日期
	 * @param num 变更天数 -1 前一天  0 当前日期  1第二天
	 * @return
	 */
	public static Date getNextDay(Date date,int num) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, num);
		date = calendar.getTime();
		return date;
	}
}
