package com.travelsky.quick.util.helper;

import java.util.Date;
import java.util.regex.Pattern;

import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ErrCodeConstants;

/**
 *
 * @author ZHANGJIABIN
 *
 */
public class QOrderValidate extends OrderValidate{
	@Override
	public String checkOrderCreate(CommandData input, CommandData lInput) {
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
		if("R".equals(lRouttype)){
			if(lDestdate == null || lDestdate.before(lOridate)){
				errmsg = ErrCodeConstants.API_DESTDATE_ERROR;
				return errmsg;
			}
		}
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
			if(rFamilycode.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_BRAND_CODE;
				return errmsg;
			}
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

			if(rId.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_PAXS_ID;
				return errmsg;
			}
			if(rLastname.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_PAX_LASTNAME;
				return errmsg;
			}
			if(rFirstname.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_PAX_FIRSTNAME;
				return errmsg;
			}
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
			if(rBirth == null){
				errmsg = ErrCodeConstants.API_NULL_BIRTHDAY;
				return errmsg;
			}
			if("NI".equals(rPasstype)){
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
			if(!"".equals(rPaxsex)){
				if(!"M".equals(rPaxsex) && !"F".equals(rPaxsex)){
					errmsg = ErrCodeConstants.API_PAX_SEX_ERROR;
					return errmsg;
				}
			}
			if(!"INF".equals(rPaxtype) && ("".equals(rTelephone) || !Pattern.matches(REG_MOBILE, rTelephone))){
				errmsg = ErrCodeConstants.API_PAX_TELEPHONE_ERROR;
				return errmsg;
			}
			if(!"NI".equals(rPasstype) && !"PP".equals(rPasstype) && !"OT".equals(rPasstype)){
				errmsg = ErrCodeConstants.API_PAX_PASSTYPE_ERROR;
				return errmsg;
			}
			if(rPassno.isEmpty()){
				return ErrCodeConstants.API_NULL_IDNO;
			}

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
		Table lContact = new Table(new String[]{"name","lastname","firstname","areacode","contactprefix","telephone","email"});
		for(int i = 0 ; i < lContacts.getRowCount() ; i++){
			Row  lCon = lContact.addRow();
			Row lRow = lContacts.getRow(i);
			String rName = lRow.getColumn("name").getStringColumn();
			String rTelephone = lRow.getColumn("telephone").getStringColumn();
			//邮件地址
			String rEmail = lRow.getColumn("email").getStringColumn();
			lCon.addColumn("lastname", rName);
			lCon.addColumn("name", rName);
			lCon.addColumn("firstname", rName);
			lCon.addColumn("areacode", "86[中国]");
			lCon.addColumn("contactprefix", "86");
			lCon.addColumn("telephone", rTelephone);
			lCon.addColumn("email", rEmail);
			if(!rEmail.isEmpty() && (rEmail.length() > 50 || !Pattern.matches(REG_EMAIL, rEmail))){
				errmsg = ErrCodeConstants.API_PAX_EMAIL_ERROR;
			}
			if(rName.isEmpty()){
				errmsg = ErrCodeConstants.API_NULL_CONTACT_NAME;
				return errmsg;
			}
			if(!Pattern.matches(REG_CHINESE, rName)){
				if(!Pattern.matches(REG_ENGLISH, rName)){
					errmsg = ErrCodeConstants.API_CONTACT_NAME_ERROR;
					return errmsg;
				}
			}
			if(rTelephone.isEmpty() || !Pattern.matches(REG_MOBILE, rTelephone)){
				errmsg = ErrCodeConstants.API_CONTACT_TELEPHONE_ERROR;
				return errmsg;
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
}