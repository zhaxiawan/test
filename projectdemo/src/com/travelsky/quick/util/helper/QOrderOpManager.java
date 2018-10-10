package com.travelsky.quick.util.helper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.constant.Constant;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.RedisUtil;

public class QOrderOpManager extends OrderOpManager{
	@Override
	public CommandRet createorder(CommandData input, SelvetContext<ApiContext> context) {
		String language = ApiServletHolder.getApiContext().getLanguage();
		CommandInput lInput = new CommandInput("com.cares.sh.order.order.create");
		//输入参数验证
		QOrderValidate lValidate = new QOrderValidate();
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
	
	@Override
	protected CommandRet organizeDetailResult(CommandRet lRet, SelvetContext<ApiContext> context) {

		CommandRet result = new CommandRet("");
		//订单号
		String orderno = lRet.getParm("orderid").getStringColumn();
		//创建时间
		Date createtime = lRet.getParm("createtime").getDateColumn();
		//往返程类型(S单程 R往返程)
		String routtype = lRet.getParm("routtype").getStringColumn();
		//订单总价
		String totalprice = lRet.getParm("totalprice").getStringColumn();
		//订单状态
		String orderstatus = lRet.getParm("status").getStringColumn();
		//最大支付时限
		String maxOrderTime = "";
		//保险信息
		Table	subinsurTable = lRet.getParm("subinsurance").getTableColumn();
		//航班信息Table
		Table lFlights = lRet.getParm("flights").getTableColumn();
		Table flights = new Table(new String[]{"id","airlinecd","flightno","oricode","destcode","oridate","destdate","oriteminal","destterminal","routtype","familycode","familyname","status","refundinfo","childrefundinfo","babyrefundinfo","pnr","oriname","oricitycode","oricityname","oricountry","destname","destcitycode","destcityname","destcountry","cabin","cabinName","planestype"});
		//乘机人信息Table
		Table lPaxs = lRet.getParm("paxs").getTableColumn();
		Table paxs = new Table(new String[]{"id","lastname","firstname","paxtype","birth","paxsex","contactprefix","telephone","email","guardian","passtype","passno","pnr","paxname","docexpiry","issuecountry","birthcountry"});
		//乘机人航段信息Table
		Table lPaxflights = lRet.getParm("paxflights").getTableColumn();
		Table paxflights = new Table(new String[]{"id","paxid","flightid","pnr","ticketprice","cnfee","yqfee","tax","status","invstatus","salestatus","ticketno","ticketsegno"});
		//辅营信息Table
		Table lSubmarkets = lRet.getParm("submarkets").getTableColumn();
		Table submarkets = new Table(new String[]{"id","paxid","flightid","submarketcode","submarkettype","submarketname","submarketdesc","isfree","unitprice","buynum","refundinfo","status","familycode","currencySign","ssrtype","invstatus","salestatus"});
		//座位信息Table
		Table lSeats = lRet.getParm("seats").getTableColumn();
		Table seats = new Table(new String[]{"paxid","flightid","seat","price","status","invstatus","salestatus","refundinfo"});
		//联系人信息
		Table lContacts = lRet.getParm("contacts").getTableColumn();
		Table contacts = new Table(new String[]{"name","contactprefix","telephone","email"});
		//支付信息
		Table lPays = lRet.getParm("pays").getTableColumn();
		Table pays = new Table(new String[]{"id","createtime","price","status","paychannelno","bankid","billno","orgid","apptype","maxpaysecond","maxpaytime"});
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
		Table refunddetails = new Table(new String[]{"id","refundid","paxid","flightid","feetype","submarkettype","submarketname","seat","buynum","unitprice","refundfee"});
		result.addParm("orderno", orderno);
		result.addParm("createtime", Unit.getString(createtime, "yyyy/MM/dd HH:mm:ss"));
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
				row.addColumn("oridate", lRow.getColumn("oridate").getStringColumn());
				row.addColumn("destdate", lRow.getColumn("destdate").getStringColumn());
				row.addColumn("oriteminal", lRow.getColumn("oriterminal").getStringColumn());
				row.addColumn("destterminal", lRow.getColumn("destterminal").getStringColumn());
				//来回程类型(G去程 R回程)
				row.addColumn("routtype", lRow.getColumn("routtype").getStringColumn());
				row.addColumn("familycode", lRow.getColumn("familycode").getStringColumn());
				row.addColumn("familyname", lRow.getColumn("familyname").getStringColumn());
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
				row.addColumn("planestype", lRow.getColumn("planestype").getStringColumn());
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
				//状态(1待确认、2已拒绝、3待支付、4已取消、5已支付、6已出票、7待退款、8待退款审核、
				//9已退款、10已值机、11已交付、12已使用、13已使用 )
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
				Table lCosts = lRet.getParm("costs").getTableColumn();
				if(lCosts != null && lCosts.getRowCount() > 0){
					for(int j = 0 ; j < lCosts.getRowCount() ; j++){
						Row costsRow = lCosts.getRow(j);
						String mappingid = costsRow.getColumn("mappingid").getStringColumn();
						String flightid = costsRow.getColumn("flightid").getStringColumn();
						String costtype = costsRow.getColumn("costtype").getStringColumn();
						String price = costsRow.getColumn("price").getStringColumn();
						if (mappingid.equals(lRow.getColumn("paxid").getStringColumn()) && flightid.equals(lRow.getColumn("flightid").getStringColumn())) {
							if (Constant.Order.COST_TYPE_TICKET.equals(costtype) ) {
								row.addColumn("ticketprice", price);
							} else if (Constant.Order.COST_TYPE_CN.equals(costtype) ) {
								row.addColumn("cnfee", price);
							} else if (Constant.Order.COST_TYPE_YQ.equals(costtype) ) {
								row.addColumn("yqfee", price);
							} else if (Constant.Order.COST_TYPE_TAX.equals(costtype)){
								row.addColumn("tax", price);
							}
						}

					}
				}
			}
		}
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
				row.addColumn("submarketname", lRow.getColumn("submarketname").getStringColumn());
				row.addColumn("submarketdesc", lRow.getColumn("submarketdesc").getStringColumn());
			}
		}
		if(lSeats != null && lSeats.getRowCount() > 0){
			for(int i = 0 ; i < lSeats.getRowCount() ; i++){
				Row lRow = lSeats.getRow(i);
				Row row = seats.addRow();
				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("seat", lRow.getColumn("seatno").getStringColumn());
				row.addColumn("price", lRow.getColumn("price").getStringColumn());
				row.addColumn("status", lRow.getColumn("status").getStringColumn());
			}
		}
		if(lContacts != null && lContacts.getRowCount() > 0){
			for(int i = 0 ; i < lContacts.getRowCount() ; i++){
				Row lRow = lContacts.getRow(i);
				Row row = contacts.addRow();
				row.addColumn("name", lRow.getColumn("name").getStringColumn());
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
				row.addColumn("id", lRow.getColumn("id").getStringColumn());
				row.addColumn("refundfee", lRow.getColumn("refundfee").getStringColumn());
				row.addColumn("refundid", lRow.getColumn("refundid").getStringColumn());

				row.addColumn("paxid", lRow.getColumn("paxid").getStringColumn());
				row.addColumn("flightid", lRow.getColumn("flightid").getStringColumn());
				row.addColumn("feetype", lRow.getColumn("detailtype").getStringColumn());
				//TODO 返回结果里没有,且附营暂不支持
				row.addColumn("submarkettype", lRow.getColumn("").getStringColumn());
				row.addColumn("submarketname", lRow.getColumn("").getStringColumn());
				row.addColumn("seat", lRow.getColumn("").getStringColumn());
				row.addColumn("buynum", lRow.getColumn("").getStringColumn());
				row.addColumn("unitprice", lRow.getColumn("").getStringColumn());
			}
		}

		return result;
	
	}
}
