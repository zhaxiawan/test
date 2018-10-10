package com.travelsky.quick.util.helper;

import java.util.Date;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;

/**
 *
 * @author ZHANGJIABIN
 *
 */
public class OrderValidate {
//	public static final String REG_MOBILE = "^[1][0-9]{10}$";
	//只校验数字
	public static final String REG_MOBILE = "^[0-9]*$";
	
	public static final String REG_EMAIL="^([a-zA-Z0-9_\\.-]+)@([\\da-zA-Z\\.-]+)\\.([a-zA-Z\\.]{2,6})$";
	public static final String REG_CHINESE = "[\u4E00-\u9FA5]{1,35}";
	public static final String REG_ENGLISH = "[a-zA-Z]{1,35}";
	public static String langage = ApiServletHolder.getApiContext().getLanguage();
	/**
	 * 订单创建信息验证
	 * @param input input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkOrderCreate(CommandData input,CommandData lInput){
		String errmsg = "";
		//会员帐号
		String lMemberid = input.getParm("memberid").getStringColumn();
		//出发地机场三字码
		String lOricode = input.getParm("oricode").getStringColumn();
		//目的地机场三字码
		String lDestcode = input.getParm("destcode").getStringColumn();
		//出发日期	格式yyyyMMdd
		Date lOridate = input.getParm("oridate").getDateColumn();
		//返程日期 (格式yyyyMMdd)
		Date lDestdate = input.getParm("destdate").getDateColumn();
		//往返程类型(S单程 R往返程)
		String lRouttype = input.getParm("routtype").getStringColumn();
		//航班信息
		Table lFlights = input.getParm("flights").getTableColumn();
		//乘机人信息
		Table lPaxs = input.getParm("paxs").getTableColumn();
		//联系人信息
		Table lContacts = input.getParm("contacts").getTableColumn();
		Date now=Unit.getDate(Unit.getString(new Date(),"yyyy-MM-dd"), "yyyy-MM-dd");
		if(lMemberid.isEmpty()){
			errmsg = ErrCodeConstants.API_NULL_MEMBER_ID;
			return errmsg;
		}
		if(lOricode.isEmpty() || lOricode.length() != 3 || !Pattern.matches(REG_ENGLISH, lDestcode)){
			errmsg = ErrCodeConstants.API_ORICODE_ERROR;
			return errmsg;
		}
		if(lDestcode.isEmpty() || lDestcode.length() != 3 || !Pattern.matches(REG_ENGLISH, lDestcode)){
			errmsg = ErrCodeConstants.API_DESTCODE_ERROR;
			return errmsg;
		}
		if(lOridate == null || lOridate.before(now)){
			errmsg = ErrCodeConstants.API_ORIDATE_ERROR;
			return errmsg;
		}
		if(!"S".equals(lRouttype) && !"R".equals(lRouttype)){
			errmsg = ErrCodeConstants.API_ROUTTYPE_ERROR;
			return errmsg;
		}
//		if("R".equals(lRouttype)){
//			if(lDestdate == null || lDestdate.before(lOridate)){
//				errmsg = ErrCodeConstants.API_DESTDATE_ERROR;
//				return errmsg;
//			}
//		}
		if(lFlights == null || lFlights.getRowCount() < 1){
			errmsg = ErrCodeConstants.API_NULL_FLIGHT;
			return errmsg;
		}
		if(lPaxs == null || lPaxs.getRowCount() < 1){
			errmsg = ErrCodeConstants.API_NULL_PAXS;
			return errmsg;
		}
		if(lContacts == null || lContacts.getRowCount() < 1){
			errmsg = ErrCodeConstants.API_NULL_CONTACT;
			return errmsg;
		}
		for(int i = 0 ; i < lFlights.getRowCount() ; i++){
			Row lRow = lFlights.getRow(i);
			String rAirlinecd = lRow.getColumn("airlinecd").getStringColumn();	//销售航空公司
			String rFlightno = lRow.getColumn("flightno").getStringColumn();		//销售航班号
			Date rOridate = lRow.getColumn("oridate").getDateColumn();			//起飞时间(格式yyyyMMdd HH:mm:ss)
			String rRouttype = lRow.getColumn("routtype").getStringColumn();		//来回程类型(G去程 R回程)
			String rFamilycode = lRow.getColumn("familycode").getStringColumn();	//品牌代码

			if(rAirlinecd.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_FLIGHT_AIRLINECD;
				return errmsg;
			}
			if(rFlightno.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_FLIGHT_NO;
				return errmsg;
			}
			if(rOridate == null || rOridate.before(new Date())){
				errmsg = ErrCodeConstants.API_ORIDATE_ERROR;
				return errmsg;
			}
			if(!"G".equals(rRouttype) && !"R".equals(rRouttype)){
				errmsg = ErrCodeConstants.API_ROUTTYPE_ERROR;
				return errmsg;
			}
//			if(rFamilycode.isEmpty()){
//				errmsg = ErrCodeConstants.API_NULL_BRAND_CODE;
//				return errmsg;
//			}
		}
		for(int i = 0 ; i < lPaxs.getRowCount() ; i++){
			Row lRow = lPaxs.getRow(i);
			//编号
			String rId = lRow.getColumn("id").getStringColumn();
			//姓
			String rLastname = lRow.getColumn("lastname").getStringColumn();
			lRow.addColumn("lastname", rLastname);
			//名
			String rFirstname = lRow.getColumn("firstname").getStringColumn();
			lRow.addColumn("firstname", rFirstname);
			//人员类型(ADT成人 CHD儿童 INF婴儿)
			String rPaxtype = lRow.getColumn("paxtype").getStringColumn();
			//国籍
			String nationality = lRow.getColumn("nationality").getStringColumn();
			lRow.addColumn("nationality", nationality);
			//出生日期(格式yyyyMMdd)
			Date rBirth = lRow.getColumn("birth").getDateColumn();
			//性别(M男 F女)
			String rPaxsex = lRow.getColumn("paxsex").getStringColumn();
			//陪护人 婴儿必填
			String rGuardian = lRow.getColumn("guardian").getStringColumn();
			//证件类型(NI身份证 PP护照 OT其他)
			String rPasstype = lRow.getColumn("passtype").getStringColumn();
			//证件号码
			String rPassno = lRow.getColumn("passno").getStringColumn();
			lRow.addColumn("passno", rPassno);
			//电话号码
			String rTelephone = lRow.getColumn("telephone").getStringColumn();
//			if(StringUtils.hasLength(rTelephone)){
//				if(!Pattern.matches(REG_MOBILE, rTelephone)){
//					errmsg = ErrCodeConstants.API_PAX_TELEPHONE_ERROR;
//					return errmsg;
//				}
//			}
			lRow.addColumn("telephone", rTelephone);
			lRow.addColumn("issuecountry", lRow.getColumn("issuecountry").getStringColumn());
			lRow.addColumn("contactprefix", lRow.getColumn("contactprefix").getStringColumn());
			lRow.addColumn("areacode", lRow.getColumn("areacode").getStringColumn());
			lRow.addColumn("docexpiry", lRow.getColumn("docexpiry").getStringColumn());
//			if(rId.isEmpty()){
//				errmsg = ErrCodeConstants.API_NULL_PAXS_ID;
//				return errmsg;
//			}
//			if(rLastname.isEmpty()){
//				errmsg = ErrCodeConstants.API_NULL_PAX_LASTNAME;
//				return errmsg;
//			}
//			if(rFirstname.isEmpty()){
//				errmsg = ErrCodeConstants.API_NULL_PAX_FIRSTNAME;
//				return errmsg;
//			}
			if(!"".equals(rLastname)){
				if(!Pattern.matches(REG_CHINESE, rLastname)){
					if(!Pattern.matches(REG_ENGLISH, rLastname)){
						errmsg=ErrCodeConstants.API_PAX_LASTNAME_ERROR;
						return errmsg;
					}
				}
			}
			if(!"".equals(rFirstname)){
				if(!Pattern.matches(REG_CHINESE, rFirstname)){
					if(!Pattern.matches(REG_ENGLISH, rFirstname)){
						errmsg=ErrCodeConstants.API_PAX_FIRSTNAME_ERROR;
						return errmsg;
					}
				}
			}
			if(!"".equals(rLastname)&&!"".equals(rFirstname)){
				if(Pattern.matches(REG_CHINESE, rLastname)&&!Pattern.matches(REG_CHINESE, rFirstname)){
					return ErrCodeConstants.API_PAX_LASTFIRST_CHINESE_ERROR;
				}
				if(Pattern.matches(REG_ENGLISH, rLastname)&&!Pattern.matches(REG_ENGLISH, rFirstname)){
					return ErrCodeConstants.API_PAX_LASTFIRST_ENGLISH_ERROR;
				}
				if(Pattern.matches(REG_CHINESE, rFirstname)&&!Pattern.matches(REG_CHINESE, rLastname)){
					return ErrCodeConstants.API_PAX_FIRSTLAST_CHINESE_ERROR;
				}
				if(Pattern.matches(REG_ENGLISH, rFirstname)&&!Pattern.matches(REG_ENGLISH, rLastname)){
					return ErrCodeConstants.API_PAX_FIRSTLAST_ENGLISH_ERROR;
				}
			}


			if(!"ADT".equals(rPaxtype) && !"CHD".equals(rPaxtype) && !"INF".equals(rPaxtype)){
				errmsg = ErrCodeConstants.API_PAX_TYPE_ERROR;
				return errmsg;
			}
//			if(rBirth == null){
//				errmsg = ErrCodeConstants.API_NULL_BIRTHDAY;
//				return errmsg;
//			}
			if("NI".equals(rPasstype)){
				if(StringUtils.hasLength(rPassno)){
					if(!getValidIdCard(rPassno)){
						return ErrCodeConstants.API_PAX_PASSNO_ERROR;
					}
//				if(!"".equals(rPaxsex)){
//					int gen = Integer.valueOf(rPassno.substring(16, 17)) % 2;
//					if (("M".equalsIgnoreCase(rPaxsex) && gen == 0) || ("F".equalsIgnoreCase(rPaxsex) && gen == 1)) {
//						return ErrCodeConstants.API_PAX_SEXPASSNO_ERROR;
//					}
//				}
					//根据身份证号获取的生日
					Date rPassnoBirth = Unit.getDate(rPassno.substring(6,14));
					if(!rBirth.equals(rPassnoBirth)){
						errmsg = ErrCodeConstants.API_PAX_PASSNOBIRTH_ERROR;
						return errmsg;
					}
				}
			}
			if(!"".equals(rPaxsex)){
				if(!"M".equals(rPaxsex) && !"F".equals(rPaxsex)){
					errmsg = ErrCodeConstants.API_PAX_SEX_ERROR;
					return errmsg;
				}
			}
			/*if(!"INF".equals(rPaxtype) && ("".equals(rTelephone) || !Pattern.matches(REG_MOBILE, rTelephone))){
				errmsg = ErrCodeConstants.API_PAX_TELEPHONE_ERROR;
				return errmsg;
			}*/
//			if(!"NI".equals(rPasstype) && !"PP".equals(rPasstype) && !"OT".equals(rPasstype)){
//				errmsg = ErrCodeConstants.API_PAX_PASSTYPE_ERROR;
//				return errmsg;
//			}
//			if(rPassno.isEmpty()){
//				return ErrCodeConstants.API_NULL_IDNO;
//			}

			int year = getYear(lOridate,rBirth);
			if (year <= 0){
				return ErrCodeConstants.API_PAX_BIRTH_ERROR;
			} else if(getYear(new Date(lOridate.getTime() - 14 * 24 * 3600 *1000),rBirth) <= 0){
				//14天之内的婴儿不可以买票
				return ErrCodeConstants.API_PAX_INF_ERROR;
			}
			if("CHD".equals(rPaxtype)){
				if(year > 12 || year < 2){
					return ErrCodeConstants.API_CHD_AGE_ERROR;
				}
			} else if("INF".equals(rPaxtype)){
				if(year > 2){
					return ErrCodeConstants.API_INF_AGE_ERROR;
				}
			} else if("ADT".equals(rPaxtype)){
				if(year <= 2){
					return ErrCodeConstants.API_ADT_AGE_ERROR;
				}
			}
			if("INF".equals(rPaxtype)){
				if(rGuardian.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_GUARDIAN;
					return errmsg;
				}
			}
		}
		Table lContact = new Table(new String[]{"lastname","firstname","areacode","contactprefix","telephone","email"});
		for(int i = 0 ; i < lContacts.getRowCount() ; i++){
			Row  lCon = lContact.addRow();
			Row lRow = lContacts.getRow(i);
			String lastname = lRow.getColumn("lastname").getStringColumn();
			String firstname = lRow.getColumn("firstname").getStringColumn();
			String areacode = lRow.getColumn("areacode").getStringColumn();
			String contactprefix = lRow.getColumn("contactprefix").getStringColumn();
			String rTelephone = lRow.getColumn("telephone").getStringColumn();
			//邮件地址
			String rEmail = lRow.getColumn("email").getStringColumn();
			lCon.addColumn("lastname", lastname);
			lCon.addColumn("firstname", firstname);
			lCon.addColumn("areacode", areacode);
			lCon.addColumn("contactprefix", contactprefix);
			lCon.addColumn("telephone", rTelephone);
			lCon.addColumn("email", rEmail);
			if(!rEmail.isEmpty() && (rEmail.length() > 50 || !Pattern.matches(REG_EMAIL, rEmail))){
				errmsg = ErrCodeConstants.API_PAX_EMAIL_ERROR;
			}
//			if(lastname.isEmpty()){
//				errmsg = ErrCodeConstants.API_NULL_CONTACT_NAME;
//				return errmsg;
//			}
			if(!Pattern.matches(REG_CHINESE, lastname)){
				if(!Pattern.matches(REG_ENGLISH, lastname)){
					errmsg = ErrCodeConstants.API_CONTACT_NAME_ERROR;
					return errmsg;
				}
			}
			if(StringUtils.hasLength(rTelephone)){
				if(!Pattern.matches(REG_MOBILE, rTelephone)){
					errmsg = ErrCodeConstants.API_CONTACT_TELEPHONE_ERROR;
					return errmsg;
				}
			}
		}

		if("".equals(errmsg)){
			//会员帐号
			lInput.addParm("memberid", lMemberid);
			//出发地机场三字码
			lInput.addParm("oricode", lOricode);
			//目的地机场三字码
			lInput.addParm("destcode", lDestcode);
			//出发日期	格式yyyyMMdd
			lInput.addParm("oridate", input.getParm("oridate").getStringColumn());
			//返程日期 (格式yyyyMMdd)
			lInput.addParm("destdate", input.getParm("destdate").getStringColumn());
			//往返程类型(S单程 R往返程)
			lInput.addParm("routtype", lRouttype);
			//航班信息
			lInput.addParm("flights", lFlights);
			//乘机人信息
			lInput.addParm("paxs", lPaxs);
			//联系人信息
			lInput.addParm("contacts", lContact);
		}

		return errmsg;
	}

	/**
	 * 订单创建信息 第2阶段
	 * @param input
	 * @param lInput
	 * @return
	 */
	public String toOrderCreate(CommandData input,CommandData lInput){

		String errmsg = "";
		//会员帐号
//		String lMemberid = input.getParm("memberid").getStringColumn();
		//出发地机场三字码
		String lOricode = input.getParm("oricode").getStringColumn();
		//目的地机场三字码
		String lDestcode = input.getParm("destcode").getStringColumn();
		//出发日期	格式yyyyMMdd
		Date lOridate = input.getParm("oridate").getDateColumn();
		//返程日期 (格式yyyyMMdd)
		Date lDestdate = input.getParm("destdate").getDateColumn();
		//往返程类型(S单程 R往返程)
		String lRouttype = input.getParm("routtype").getStringColumn();
		//航班信息
		Table lFlights = input.getParm("flights").getTableColumn();
		//乘机人信息
		Table lPaxs = input.getParm("paxs").getTableColumn();
		//联系人信息
		Table lContacts = input.getParm("contacts").getTableColumn();
		
		//航班信息
		if (null !=lFlights && lFlights.getRowCount()>0) {
			for(int i = 0 ; i < lFlights.getRowCount() ; i++){
				Row lRow = lFlights.getRow(i);
				String rAirlinecd = lRow.getColumn("airlinecd").getStringColumn();	//销售航空公司
				String rFlightno = lRow.getColumn("flightno").getStringColumn();		//销售航班号
				Date rOridate = lRow.getColumn("oridate").getDateColumn();			//起飞时间(格式yyyyMMdd HH:mm:ss)
				String rRouttype = lRow.getColumn("routtype").getStringColumn();		//来回程类型(G去程 R回程)
	//			String rFamilycode = lRow.getColumn("familycode").getStringColumn();	//品牌代码
	
				if(rAirlinecd.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_FLIGHT_AIRLINECD;
					return errmsg;
				}
				if(rFlightno.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_FLIGHT_NO;
					return errmsg;
				}
				if(rOridate == null || rOridate.before(new Date())){
					errmsg = ErrCodeConstants.API_ORIDATE_ERROR;
					return errmsg;
				}
				if(!"G".equals(rRouttype) && !"R".equals(rRouttype)){
					errmsg = ErrCodeConstants.API_ROUTTYPE_ERROR;
					return errmsg;
				}
			}
		}
		
		//乘机人信息
		if (null !=lPaxs && lPaxs.getRowCount()>0) {
			for(int i = 0 ; i < lPaxs.getRowCount() ; i++){
				Row lRow = lPaxs.getRow(i);
				//编号
				String rId = lRow.getColumn("id").getStringColumn();
				//姓
				String rLastname = lRow.getColumn("lastname").getStringColumn();
				lRow.addColumn("lastname", rLastname);
				//名
				String rFirstname = lRow.getColumn("firstname").getStringColumn();
				lRow.addColumn("firstname", rFirstname);
				//人员类型(ADT成人 CHD儿童 INF婴儿)
				String rPaxtype = lRow.getColumn("paxtype").getStringColumn();
				//出生日期(格式yyyyMMdd)
				Date rBirth = lRow.getColumn("birth").getDateColumn();
				//性别(M男 F女)
				String rPaxsex = lRow.getColumn("paxsex").getStringColumn();
				//陪护人 婴儿必填
				String rGuardian = lRow.getColumn("guardian").getStringColumn();
				//证件类型(NI身份证 PP护照 OT其他)
				String rPasstype = lRow.getColumn("passtype").getStringColumn();
				//证件号码
				String rPassno = lRow.getColumn("passno").getStringColumn();
				lRow.addColumn("passno", rPassno);
				//电话号码
				String rTelephone = lRow.getColumn("telephone").getStringColumn();
				lRow.addColumn("telephone", rTelephone);
				lRow.addColumn("issuecountry", lRow.getColumn("issuecountry").getStringColumn());
				lRow.addColumn("contactprefix", lRow.getColumn("contactprefix").getStringColumn());
				lRow.addColumn("areacode", lRow.getColumn("areacode").getStringColumn());
				lRow.addColumn("docexpiry", lRow.getColumn("docexpiry").getStringColumn());
				if (StringUtils.hasLength(rPasstype)) {
					if(!"NI".equals(rPasstype) && !"PP".equals(rPasstype) && !"OT".equals(rPasstype)){
						errmsg = ErrCodeConstants.API_PAX_PASSTYPE_ERROR;
						return errmsg;
					}
				}
			}
		}
		
		//联系人信息
		Table lContact = new Table(new String[]{"lastname","firstname","areacode","contactprefix","telephone","email"});
		if (null !=lContacts && lContacts.getRowCount()>0) {
			for(int i = 0 ; i < lContacts.getRowCount() ; i++){
				Row  lCon = lContact.addRow();
				Row lRow = lContacts.getRow(i);
				String lastname = lRow.getColumn("lastname").getStringColumn();
				String firstname = lRow.getColumn("firstname").getStringColumn();
				String areacode = lRow.getColumn("areacode").getStringColumn();
				String contactprefix = lRow.getColumn("contactprefix").getStringColumn();
				String rTelephone = lRow.getColumn("telephone").getStringColumn();
				//邮件地址
				String rEmail = lRow.getColumn("email").getStringColumn();
				lCon.addColumn("lastname", lastname);
				lCon.addColumn("firstname", firstname);
				lCon.addColumn("areacode", areacode);
				lCon.addColumn("contactprefix", contactprefix);
				lCon.addColumn("telephone", rTelephone);
				lCon.addColumn("email", rEmail);
			}
		}

		if("".equals(errmsg)){
			//会员帐号
	//		lInput.addParm("memberid", lMemberid);
			//出发地机场三字码
			lInput.addParm("oricode", lOricode);
			//目的地机场三字码
			lInput.addParm("destcode", lDestcode);
			//出发日期	格式yyyyMMdd
			lInput.addParm("oridate", input.getParm("oridate").getStringColumn());
			//返程日期 (格式yyyyMMdd)
			lInput.addParm("destdate", input.getParm("destdate").getStringColumn());
			//往返程类型(S单程 R往返程)
			lInput.addParm("routtype", lRouttype);
			//航班信息
			lInput.addParm("flights", lFlights);
			//乘机人信息
			lInput.addParm("paxs", lPaxs);
			//联系人信息
			lInput.addParm("contacts", lContact);
		}

		return errmsg;
	}
	
	
	/**
	 * 验证辅营
	 * @param input input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkSetsubmarket(CommandData input,CommandData lInput){
		String errmsg = "";
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		String lSubmarkettype = input.getParm("submarkettype").getStringColumn();
		Table lSubmarkets = input.getParm("submarkets").getTableColumn();
		if(lSubmarkets != null && lSubmarkets.getRowCount() >0){
			for(int i = 0 ; i < lSubmarkets.getRowCount() ; i++){
				Row lRow = lSubmarkets.getRow(i);
				String rPaxid = lRow.getColumn("paxfltid").getStringColumn();
	//			String rFlightid = lRow.getColumn("flightid").getStringColumn();
				String rSubmarketcode = lRow.getColumn("submarketcode").getStringColumn();
				String rSubmarkettype = lRow.getColumn("submarkettype").getStringColumn();
				String rBuynum = lRow.getColumn("buynum").getStringColumn();
				if(rPaxid.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_PAXS_ID;
					return errmsg;
				}
	//			if(rFlightid.isEmpty()){
	//				errmsg = ErrCodeConstants.API_SUBMARKET_FLIGHTID_ERROR;
	//				return errmsg;
	//			}
				if(rSubmarketcode.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_SUBMARKET_CODE;
					return errmsg;
				}
				if(rSubmarkettype.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_SUBMARKET_TYPE;
					return errmsg;
				}
				if(rSubmarkettype.isEmpty()){
					errmsg = ErrCodeConstants.API_SUBMARKET_TYPE_ERROR;
					return errmsg;
				}
				if(rBuynum.isEmpty()){
					errmsg = ErrCodeConstants.API_NULL_SUBMARKET_BUYNUM;
					return errmsg;
				}
			}
		}

		if("".equals(errmsg)){
			//订单号
			lInput.addParm("orderno", lOrderno);
			//会员帐号
			lInput.addParm("memberid", lMemberid);
			//辅营类型(bag行李 ins保险 meal餐食)
			lInput.addParm("submarkettype", lSubmarkettype);
			//辅营信息
			lInput.addParm("submarkets", lSubmarkets);
		}

		return errmsg;
	}

	/**
	 * 验证座位
	 * @param input input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkSetseat(CommandData input,CommandData lInput){
		String errmsg = "";
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		Table lSeats = input.getParm("seats").getTableColumn();
		if ("".equals(lOrderno)) {
			errmsg = ErrCodeConstants.API_NULL_ORDER_NO;
			return errmsg;
		}
		if ("".equals(lMemberid)) {
			errmsg = ErrCodeConstants.API_NULL_MEMBER_ID;
			return errmsg;
		}
		if(lSeats == null || lSeats.getRowCount() < 1){
			errmsg = ErrCodeConstants.API_NULL_SEATS;
			return errmsg;
		}
		Table seats = new Table(new String[]{"paxfltid","seatno"});
		for(int i = 0 ; i < lSeats.getRowCount() ; i++){
			Row lRow = lSeats.getRow(i);
			String id = lRow.getColumn("paxfltid").getStringColumn();
			String rSeat = lRow.getColumn("seat").getStringColumn();
			if(id.isEmpty()){
				errmsg = ErrCodeConstants.API_SUBMARKET_FLIGHTID_ERROR;
				return errmsg;
			}
			/*if(rSeat.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_SEATS_NO;
				return errmsg;
			}*/
			Row row = seats.addRow();
			row.addColumn("paxfltid", id);
			row.addColumn("seatno", rSeat);
		}

		if("".equals(errmsg)){
			//订单号
			lInput.addParm("orderno", lOrderno);
			//会员帐号
			lInput.addParm("memberid", lMemberid);
			//辅营信息
			lInput.addParm("seats", seats);
		}

		return errmsg;
	}

	/**
	 * 验证订单提交
	 * @param input input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkSubmit(CommandData input,CommandData lInput){
		String errmsg = "";
		String lOrderno = input.getParm("orderno").getStringColumn();
		String lMemberid = input.getParm("memberid").getStringColumn();
		if ("".equals(lOrderno)) {
			errmsg = ErrCodeConstants.API_NULL_ORDER_NO;
			return errmsg;
		}
		if ("".equals(lMemberid)) {
			errmsg = ErrCodeConstants.API_NULL_MEMBER_ID;
			return errmsg;
		}
		if("".equals(errmsg)){
			//订单号
			lInput.addParm("orderno", lOrderno);
			//会员帐号
			lInput.addParm("memberid", lMemberid);
		}

		return errmsg;
	}

	/**
	 * 验证订单明细查询
	 * @param input input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkDetail(CommandData input,CommandData lInput){
		String errmsg = "";
		String pnr = input.getParm("PNR").getStringColumn();
		String lOrderno = input.getParm("orderno").getStringColumn();
//		String lMemberid = input.getParm("memberid").getStringColumn();
		if ("".equals(lOrderno) && "".equals(pnr)) {
			errmsg = ErrCodeConstants.API_NULL_ORDER_NO;
			return errmsg;
		}
		/*if ("".equals(lMemberid)) {
			errmsg = ErrCodeConstants.API_NULL_MEMBER_ID;
			return errmsg;
		}*/
		if("".equals(errmsg)){
			//订单号
			lInput.addParm("orderno", lOrderno);
			//会员帐号
		//	lInput.addParm("memberid", lMemberid);
		}
		return errmsg;
	}

	/**
	 * 检查退票预算
	 * @param input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkRefundtest(CommandData input,CommandData lInput){
		String errmsg = "";
		String orderno = input.getParm("orderno").getStringColumn();
		String memberid = input.getParm("memberid").getStringColumn();
		//退票类型(0自愿退票1非自愿退票)
		String refundtype = input.getParm("refundtype").getStringColumn();
		Table paxs = input.getParm("paxs").getTableColumn();
		if (orderno.isEmpty()) {
			errmsg = ErrCodeConstants.API_NULL_ORDER_NO;
			return errmsg;
		}
		if (memberid.isEmpty()) {
			errmsg = ErrCodeConstants.API_NULL_MEMBER_ID;
			return errmsg;
		}
		if (!"0".equals(refundtype) && !"1".equals(refundtype)) {
			errmsg = ErrCodeConstants.API_REFUNDTYPE_ERROR;
			return errmsg;
		}
		if (paxs == null || paxs.getRowCount() < 1) {
			errmsg = ErrCodeConstants.API_NULL_PAXS;
			return errmsg;
		}
		if(paxs.getRowCount()>0){
			for (Row paxsrow : paxs) {
				Table offers = paxsrow.getColumn("offers").getTableColumn();
				if(offers.getRowCount()>0){
					for (Row offersrow : offers) {
						String serviceid = offersrow.getColumn("serviceid").getStringColumn();
						String servicetype = offersrow.getColumn("servicetype").getStringColumn();
						if(serviceid.isEmpty()){
							errmsg = ErrCodeConstants.API_PAXFLIGHTID_ISNULL;
							return errmsg;
						}
						if(servicetype.isEmpty()){
							errmsg = ErrCodeConstants.API_NULL_REFUND_TYPE;
							return errmsg;
						}
					}
				}
			}
		}
		if("".equals(errmsg)){
			//订单号
			lInput.addParm("orderno", orderno);
			//会员帐号
			lInput.addParm("memberid", memberid);
			//退票类型(0自愿退票1非自愿退票)
			lInput.addParm("cancelflag", refundtype);
			//乘机人信息
			lInput.addParm("paxs", paxs);
		}

		return errmsg;
	}

	/**
	 * 验证退票申请
	 * @param input input
	 * @param lInput lInput
	 * @return String
	 */
	public String checkRefund(CommandData input,CommandData lInput){
		String errmsg = "";
		String orderno = input.getParm("orderno").getStringColumn();
		String memberid = input.getParm("memberid").getStringColumn();
		//0 自愿  1非自愿
		String refundtype = input.getParm("refundtype").getStringColumn();
		//退票原因类型(1航班取消2航班延误3航班改期4因病取消5其它原因)
		String refundreasontype = input.getParm("refundreasontype").getStringColumn();
		//退票原因说明(非自愿退票时输入)
		String refundreason = input.getParm("refundreason").getStringColumn();
		//退票申请人
		String applicant = input.getParm("applicant").getStringColumn();
		//退票申请人联系电话
		String telephone = input.getParm("telephone").getStringColumn();
		Table paxs = input.getParm("paxs").getTableColumn();
		Table attachments = input.getParm("attachments").getTableColumn();
		if (orderno.isEmpty()) {
			errmsg = ErrCodeConstants.API_NULL_ORDER_NO;
			return errmsg;
		}
		if (memberid.isEmpty()) {
			errmsg = ErrCodeConstants.API_NULL_MEMBER_ID;
			return errmsg;
		}
		if (!"0".equals(refundtype) && !"1".equals(refundtype)) {
			errmsg = ErrCodeConstants.API_REFUNDTYPE_ERROR;
			return errmsg;
		}
		if("1".equals(refundtype)){
			if(refundreasontype.isEmpty() || refundreasontype.compareTo("1") < 0 || refundreasontype.compareTo("5") > 0){
				errmsg = ErrCodeConstants.API_REFUND_REASONTYPE_ERROR;
				return errmsg;
			}
			if(refundreason.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_REFUND_REASON;
				return errmsg;
			}
		}
		if (applicant.isEmpty()) {
			errmsg = ErrCodeConstants.API_NULL_REFUND_APPLICANT;
			return errmsg;
		}
		/*if (telephone.isEmpty() || !Pattern.matches(REG_MOBILE, telephone)) {
			errmsg = ErrCodeConstants.API_REFUND_APPLICANT_TELEPHONE_ERROR;
			return errmsg;
		}*/
		if (paxs == null || paxs.getRowCount() < 1) {
			errmsg = ErrCodeConstants.API_NULL_PAXS;
			return errmsg;
		}
		if(paxs.getRowCount()>0){
			for (Row paxsrow : paxs) {
				Table offers = paxsrow.getColumn("offers").getTableColumn();
				if(offers.getRowCount()>0){
					for (Row offersrow : offers) {
						String serviceid = offersrow.getColumn("serviceid").getStringColumn();
						String servicetype = offersrow.getColumn("servicetype").getStringColumn();
						if(serviceid.isEmpty()){
							errmsg = ErrCodeConstants.API_PAXFLIGHTID_ISNULL;
							return errmsg;
						}
						if(servicetype.isEmpty()){
							errmsg = ErrCodeConstants.API_NULL_REFUND_TYPE;
							return errmsg;
						}
					}
				}
			}
		}
		if("".equals(errmsg)){
			//订单号
			lInput.addParm("orderno", orderno);
			//会员帐号
			lInput.addParm("memberid", memberid);
			//退票类型(0自愿退票1非自愿退票)
			lInput.addParm("cancelflag", refundtype);
			// 1 航班取消,2 航班延误,3 航班改期,4 因病取消,5 其它原因
			lInput.addParm("reasontype", refundreasontype);
			lInput.addParm("reason", refundreason);
			lInput.addParm("person", applicant);
			lInput.addParm("phone", telephone);
			lInput.addParm("paxs", paxs);
			//乘机人信息
			lInput.addParm("attachments", attachments);
		}

		return errmsg;
	}

	/**
	 * 验证身份证
	 * @param cardid cardid
	 * @return boolean
	 */
	protected boolean getValidIdCard(String cardid){
		String ls_id = cardid;
		if(ls_id.length() != 18)
		{
			return false;
		}
		char[] l_id = ls_id.toCharArray();
		int l_jyw = 0;
		int[] wi = new int[]{7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2,1};
		char[] ai= new char[]{'1','0','X','9','8','7','6','5','4','3','2'};
		for(int i =0 ; i < 17; i++)
		{
			if(l_id[i] < '0' || l_id[i] > '9')
			{
				return false;
			}
			l_jyw += (l_id[i] -'0')*wi[i];
		}
		l_jyw = l_jyw % 11;
		if(ai[l_jyw] != l_id[17])
		{
			return false;
		}
		return true;
	}

	/**
	 * 验证年龄
	 * @param date1 date1
	 * @param date2 date2
	 * @return int
	 */
	protected int getYear(Date date1,Date date2){
		int li_year =0;
		if(date1 == null || date2 == null){
			return 0;
		}
		if(date2.getTime() >= date1.getTime()){
			return li_year;
		}
		String l_str1 = Unit.getString(date1);
		String l_str2 = Unit.getString(date2);
		li_year = Unit.getInteger(l_str1.substring(0, 4)) - Unit.getInteger(l_str2.substring(0, 4));
		l_str1 = l_str1.substring(4);
		l_str2 = l_str2.substring(4);
		if(l_str1.compareTo(l_str2) > 0){
			li_year ++;
		}
		return li_year;
	}
}