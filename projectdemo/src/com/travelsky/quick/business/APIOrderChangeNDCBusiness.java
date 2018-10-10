package com.travelsky.quick.business;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.BookingReferencesDocument.BookingReferences;
import org.iata.iata.edist.CodesetType;
import org.iata.iata.edist.ContactsDocument.Contacts;
import org.iata.iata.edist.ContactsDocument.Contacts.Contact;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightList;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.OriginDestinationList;
import org.iata.iata.edist.DataListType.SeatList;
import org.iata.iata.edist.DescriptionType.Text;
import org.iata.iata.edist.EmailType;
import org.iata.iata.edist.EmailType.Address;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfSeatType;
import org.iata.iata.edist.ListOfServiceBundleType;
import org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.OrderChangeRQDocument;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query.Passengers;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query.Passengers.Passenger;
import org.iata.iata.edist.OrderItemAssociationType;
import org.iata.iata.edist.OrderItemAssociationType.Flight;
import org.iata.iata.edist.OrderItemRepriceType;
import org.iata.iata.edist.OrderItemRepriceType.OrderItem;
import org.iata.iata.edist.OrderItemRepriceType.OrderItem.InventoryGuarantee;
import org.iata.iata.edist.OriginDestinationDocument.OriginDestination;
import org.iata.iata.edist.PassengerSummaryType.Gender;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo.FOID;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo.PassengerDocument;
import org.iata.iata.edist.PhoneContactType;
import org.iata.iata.edist.PhoneType.Number;
import org.iata.iata.edist.SeatLocationType;
import org.iata.iata.edist.SegmentReferencesDocument.SegmentReferences;
import org.iata.iata.edist.ServiceDescriptionType;
import org.iata.iata.edist.ServiceDescriptionType.Description;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
import org.iata.iata.edist.TravelerCoreType.Age;
import org.iata.iata.edist.TravelerCoreType.Age.BirthDate;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelerSummaryType.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.APIOrderCreateNDCONEE;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

@Service("LCC_ORDERCHANGE_SERVICE")
@Lazy
public class APIOrderChangeNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIOrderChangeNDCBusiness.class);
	private static final String FLT = "FLT";

	
	
	@Override
	protected void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 转换 xml-->Reqbean
		try {
			transInputXmlToRequestBean();
			CommandRet ret;
			//change时，失败的情况需要依次返回
			Table errorTable;
			// 调用后台修改数据
			CommandData input = context.getInput();
			// 获取订单号
			String orderNo = "";
			String pnr="";
			errorTable=new Table(new String[]{"errorcode","errordesc"});
			// 调用优先级
			Table paxflts = input.getParm("paxflts").getTableColumn();
			
			if (paxflts != null && paxflts.getRowCount() > 0) {
				// 增删人航段
				context.setRet(getePaxflt());
				//有错误时，需要记录错误信息，集中返回
				ret = context.getRet();
				if (ret.isError()) {
					setError(ret, errorTable);
				}
				if ("".equals(orderNo)) {
					orderNo = context.getRet().getParm("orderno").getStringColumn();
				}
			}
			// 选座
			Table seats = input.getParm("seats").getTableColumn();
			if (seats != null && seats.getRowCount() > 0) {
				// 选座和释放座位
				context.setRet(getSeat());
				//有错误时，需要记录错误信息，集中返回
				ret = context.getRet();
				if (ret.isError()) {
					setError(ret, errorTable);
				}
				if ("".equals(orderNo)) {
					orderNo = context.getRet().getParm("orderno").getStringColumn();
				}
			}
			// 追加特殊服务+餐食
			Table spaxsubmarkets = input.getParm("spaxsubmarkets").getTableColumn();
			if (spaxsubmarkets != null && spaxsubmarkets.getRowCount() > 0) {
				Row row = spaxsubmarkets.getRow(0);
				//特服TKNE直接那pnr查订单详情
				String code = row.getColumn("submarketcode").getStringColumn();
				if ("TKNE".equals(code)) {
					pnr=input.getParm("pnr").getStringColumn();
				}else {
					context.setRet(getResponseBean());
					//有错误时，需要记录错误信息，集中返回
					ret = context.getRet();
					if (ret.isError()) {
						RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("boolean"), "false", 60);
						setError(ret, errorTable);
					}else {
						//婴儿添加成功时，详情中需要展示
						RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("boolean"), "true", 60);
					}
					if ("".equals(orderNo)) {
						orderNo = context.getRet().getParm("orderno").getStringColumn();
					}
				}
			}
			// 修改生日 修改旅客信息
			Table paxs = input.getParm("paxs").getTableColumn();
			if (paxs != null && paxs.getRowCount() > 0) {
				context.setRet(getBrithBean());
				//有错误时，需要记录错误信息，集中返回
				ret = context.getRet();
				if (ret.isError()) {
					setError(ret, errorTable);
				}
				if ("".equals(orderNo)) {
					orderNo = context.getRet().getParm("orderno").getStringColumn();
				}
			}
				// 调用订单详情
				input.addParm("PNR", pnr);
				input.addParm("orderno", orderNo);
				input.addParm("errorTable", errorTable);
				context.setRet(getorderDetail());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_ORDER_CHANGE,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}	
	}
	/**
	 * 
	 * @param ret  底层返回参数
	 * @param errorTable  封装的错误参数
	 */
	private void setError(CommandRet ret, Table errorTable) {
		String errorCode = ret.getErrorCode();
		String errorDesc = ret.getErrorDesc();
		Row row = errorTable.addRow();
		row.addColumn("errorcode", errorCode);
		row.addColumn("errordesc", errorDesc);
	}

	/**
	 * 增删人航段信息
	 */
	public CommandRet getePaxflt() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.ePaxflt(input, context);
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
		return orderOpManager.esubmarket(input, context);
	}

	/**
	 * 修改生日,旅客类型
	 * 
	 */
	public CommandRet getBrithBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.updateie(input, context);
	}

	/**
	 * 选座
	 */
	public CommandRet getSeat() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.eseat(input, context);
	}

	/**
	 * 订单详情查询
	 */
	public CommandRet getorderDetail() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.orderDetail(input, context);
	}

	/**
	 * 
	 * @param travelerMap 
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
		// 辅营信息 String 辅营key CommandData 辅营信息
		Map<String, CommandData> travelerMap = new HashMap<String, CommandData>();
		//航班天信息 String 航班key CommandData 航班信息
		Map<String, CommandData> flightMap = new HashMap<String, CommandData>();
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderChangeRQDocument rootDoc = null;
		rootDoc = OrderChangeRQDocument.Factory.parse(xmlInput);
		
	/*	// 会员id
		String memberid = context.getContext().getUserID();
		input.addParm("memberid", memberid);
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid", deptno);*/
		OrderChangeRQ orderChangeArry = rootDoc.getOrderChangeRQ();
	//	input.addParm("owner", "1E");
		input = APICacheHelper.setDeptInfo(context, orderChangeArry.getParty(),input);
		Query queryArry = orderChangeArry.getQuery();
		// 获取pnr
		BookingReferences references = queryArry.getBookingReferences();
		BookingReferenceType referenceType = references.getBookingReferenceArray(0);
		CodesetType codesetType = referenceType.getType();
		String code = "";
		if (!"".equals(codesetType) && codesetType != null) {
			// 重试标识
			code = codesetType.getCode();
		}
		input.addParm("flag", "RLR".equals(code) ? "1" : "0");
		String pnr = referenceType.getID();
		input.addParm("pnr", pnr);
		// 获取旅客证件
		if (queryArry != null && !"".equals(queryArry)) {
			Passengers passengersArry = queryArry.getPassengers();
			if (passengersArry != null && !"".equals(passengersArry)) {
				Passenger[] passengerTable = passengersArry.getPassengerArray();
				if (passengerTable.length > 0) {
					for (Passenger pArry : passengerTable) {
						// 旅客id
						CommandData paxRet = new CommandData();
						// 存放旅客信息
						
						getPassenger(paxRet, pArry, travelerMap);
					}
				}
			}
		}
		// 获取航节对应的航班信息
		DataListType dataLists = orderChangeArry.getDataLists();
		if (dataLists != null && !"".equals(dataLists)) {
			FlightSegmentList segmentList = dataLists.getFlightSegmentList();
			if (segmentList != null && !"".equals(segmentList)) {
				ListOfFlightSegmentType[] flightSegmentArray = segmentList.getFlightSegmentArray();
				if (flightSegmentArray.length > 0) {
					for (ListOfFlightSegmentType segmentType : flightSegmentArray) {
						CommandData flightRet = new CommandData();
						String key = segmentType.getSegmentKey();
						String oricode = segmentType.getDeparture().getAirportCode().getStringValue();
						Calendar date = segmentType.getDeparture().getDate();
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						String dateStr = sdf.format(date.getTime());
						String destcode = segmentType.getArrival().getAirportCode().getStringValue();
						MarketingCarrierFlightType marketingCarrier = segmentType.getMarketingCarrier();
						String aircode = "";
						String suffix = "";
						String flno = "";
						String cabin = "";
						if (!"".equals(marketingCarrier) && marketingCarrier != null) {
							aircode = marketingCarrier.getAirlineID().getStringValue();
							suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
							flno = marketingCarrier.getFlightNumber().getStringValue();
							flno=DateUtils.getFlightNo(flno);
							cabin = marketingCarrier.getResBookDesigCode();
						}
						flightRet.addParm("seg", key);
						flightRet.addParm("oricode", oricode);
						flightRet.addParm("destcode", destcode);
						flightRet.addParm("date", dateStr);
						flightRet.addParm("aircode", aircode);
						flightRet.addParm("suffix", suffix);
						flightRet.addParm("flno", flno);
						flightRet.addParm("cabin", cabin);
						flightMap.put(key, flightRet);
					}
				}
			}
		}
		org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query.Order paxOrder = queryArry.getOrder();
		/**
		 * 兼容不指定航段添加辅营,针对要被删除掉的航段，把需要删除的seg信息过滤掉。
		 * 当加航段和删除婴儿共存时，要把新加的seg信息也过滤掉
		 */
		List<String> delList=new ArrayList<String>();
		ServiceList segService = dataLists.getServiceList();
		if (segService!=null && !"".equals(segService)) {
			ServiceDetailType[] serviceArray = segService.getServiceArray();
			if (serviceArray.length>0) {
				for (ServiceDetailType serviceDetailType : serviceArray) {
					String	settype = serviceDetailType.getActionType().getStringValue().toString().toUpperCase();
					String value = serviceDetailType.getServiceID().getStringValue().toUpperCase();
					if (("INFT".equals(value) && "CANCEL".equals(settype))) {
						if (paxOrder!=null && !"".equals(paxOrder)) {
							OrderItemRepriceType orderItems = paxOrder.getOrderItems();
							if (orderItems!=null && !"".equals(orderItems)) {
								OrderItem[] orderItemArray = orderItems.getOrderItemArray();
								if (orderItemArray.length>0 && !"".equals(orderItemArray)) {
									for (OrderItem item : orderItemArray) {
										String actionType = item.getActionType().getStringValue().toString().toUpperCase();
										if ( "CREATE".equals(actionType)) {
											OrderItemAssociationType associations = item.getAssociations();
											Flight flight = associations.getFlight();
											if (flight!=null && !"".equals(flight)) {
												String seg = flight.getSegmentReferencesArray(0).getStringValue().toString();
												delList.add(seg);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (paxOrder!=null && !"".equals(paxOrder)) {
				OrderItemRepriceType orderItems = paxOrder.getOrderItems();
				if (orderItems!=null && !"".equals(orderItems)) {
					OrderItem[] orderItemArray = orderItems.getOrderItemArray();
					if (orderItemArray.length>0 && !"".equals(orderItemArray)) {
						for (OrderItem item : orderItemArray) {
							String actionType = item.getActionType().getStringValue().toString().toUpperCase();
							if ("CANCEL".equals(actionType)) {
								OrderItemAssociationType associations = item.getAssociations();
								Flight flight = associations.getFlight();
								if (flight!=null && !"".equals(flight)) {
									String seg = flight.getSegmentReferencesArray(0).getStringValue().toString();
									delList.add(seg);
								}
							}
						}
					}
				}
			}
		}
		
		
		if (paxOrder != null && !"".equals(paxOrder)) {
			OrderItemRepriceType orderItems = paxOrder.getOrderItems();
			if (orderItems != null && !"".equals(orderItems)) {
				OrderItem[] paxOrderArray = orderItems.getOrderItemArray();
				if (paxOrderArray.length > 0) {
					// 获取要修改的旅客信息
					Table pTable = new Table(new String[] { "firstname", "lastname", "paxtype", "birth", "paxsex",
							"telephone", "email", "guardian", "passtype", "passno", "docexpiry", "issuecountry",
							"birthcountry", "oldpassno", "oldlastname", "oldfirstname", "oldpasstype", "oldpaxname",
							"oldpaxtype", "mode" });
					// 增删人航段信息
					Table fligthTable = new Table(
							new String[] { "passno", "passtype", "paxname", "carricd", "flightno", "flightsuffix",
									"flightdate", "oricode", "destcode", "InvGuaranteeID", "mode", "cabin", "flightDay" });
					Table infTable = new Table(new String[]{"id",
							"paxtype","lastname","firstname","birth","paxsex",
				            "telephone","email","guardian","passtype",
				            "passno","issuecountry","docexpiry","birthcountry","areacode"});
					for (OrderItem orderItem : paxOrderArray) {
						// 当有这个节点存在 是修改人信息或增加婴儿的信息
						org.iata.iata.edist.OrderItemAssociationType.Passengers passengers = orderItem.getAssociations()
								.getPassengers();
						Flight flight = orderItem.getAssociations().getFlight();
						org.iata.iata.edist.PassengerDocument.Passenger[] passengerArray = passengers.getPassengerArray();
						if (passengerArray.length > 0 && flight==null ) {
							String mode = orderItem.getActionType().getStringValue().toUpperCase();
							if ("UPDATE".equals(mode)) {
								// 修改旅客信息
								Row pRow = pTable.addRow();
								pRow.addColumn("mode", "1");
								this.forPassenger(pRow, passengerArray, orderItem, 1,travelerMap);
							} else if ("CREATE".equals(mode)) {
								// 增加婴儿
								CommandData paxRet = new CommandData();
								String paxInfid = passengerArray[0].getObjectKey();
								PassengerIDInfo passengerIDInfo = passengerArray[0].getPassengerIDInfo();
								String passtype = "";
								String passno = "";
								String docexpiry = "";
								String issuecountry = "";
								String birthcountry = "";
								if (passengerIDInfo != null && !"".equals(passengerIDInfo)) {
									FOID foid = passengerIDInfo.getFOID();
									if (foid != null && !"".equals(foid)) {
										passtype = foid.getType();
										passno = foid.getID().getStringValue();
									}
									PassengerDocument[] passengerDocumentArray = passengerIDInfo
											.getPassengerDocumentArray();
									int length = passengerDocumentArray.length;
									if (length > 0) {
										PassengerDocument array = passengerIDInfo.getPassengerDocumentArray(0);
										passtype = array.getType();
										passno = array.getID();
										// 证件有效期
										if (!"".equals(array.getDateOfExpiration())
												&& array.getDateOfExpiration() != null) {
											docexpiry = DateUtils.getInstance().formatDate(array.getDateOfExpiration(),
													"yyyyMMdd");
										}
										// 证件签发国
										issuecountry = array.getCountryOfIssuance();
										// 出生国
										birthcountry = array.getBirthCountry();
									}
								}
								// 证件类型
								paxRet.addParm("passtype", passtype);
								paxRet.addParm("docexpiry", docexpiry);
								paxRet.addParm("issuecountry", issuecountry);
								paxRet.addParm("birthcountry", birthcountry);
								// 姓名
								String xing = passengerArray[0].getName().getSurname().getStringValue();
								String ming = passengerArray[0].getName().getGivenArray(0).getStringValue();
								// 生日
								Age age = passengerArray[0].getAge();
								String brithDay = "";
								if (age != null) {
									BirthDate birthDate = age.getBirthDate();
									if (birthDate != null) {
										brithDay = age.getBirthDate().getStringValue();
									}
								}
								Contacts contacts = passengerArray[0].getContacts();
								// 电话，联系信息，性别
								String phone = "";
								String email = "";
								String paxsex = "";
								if (contacts != null && !"".equals(contacts)) {
									Contact contact = contacts.getContactArray(0);
									if (contact != null && !"".equals(contact)) {
										phone = contact.getPhoneContact().getNumberArray(0).getStringValue();
										email = contact.getEmailContact().getAddress().getStringValue();
									}
								}
								Gender gender = passengerArray[0].getGender();
								if (gender != null && !"".equals(gender)) {
									paxsex = gender.getStringValue();
								}
								paxRet.addParm("phone", phone);
								paxRet.addParm("email", email);
								paxRet.addParm("paxsex", paxsex);
								paxRet.addParm("birth", brithDay);
								String name = xing.toUpperCase() + ming.toUpperCase();
								paxRet.addParm("paxname", name);
								// 用于整体判断同时中文或英文
								if (Pattern.matches(
										"^([a-zA-Z-'`~!@#%=_<>;:\"\\+\\{\\}\\(\\^\\?\\.\\*\\|\\)\\$\\[\\]\\,\\& ·0-9]{2,70})$",
										name)) {
									// 说明是英文名
									paxRet.addParm("paxname", xing.toUpperCase() + "/" + ming.toUpperCase());
								}
								paxRet.addParm("xing", xing.toUpperCase());
								paxRet.addParm("ming", ming.toUpperCase());
								// 证件号
								paxRet.addParm("passno", passno);
								// 人的类型 婴儿
								PTC ptc = passengerArray[0].getPTC();
								String paxtype = "";
								if (ptc != null) {
									paxtype = ptc.getStringValue();
								}
								paxRet.addParm("paxtype", paxtype);
								travelerMap.put(paxInfid, paxRet);
								// 得到婴儿信息与监护人进行关联
								CommandData commandData = travelerMap.get(passengerArray[0].getRefs().toString());
							//	CommandData commandData = travelerMap.get(passengers.getPassengerReference().toString());
								commandData.addParm("inftid", paxInfid);
								
								/*兼容的inf添加成功或者失败都在paseenger中显示以及service中显示
										兼容关于inft创建成功，需要在详情中展示service，OK的改造 */
								Row infRow = infTable.addRow();
								infRow.addColumn("id",paxInfid);
								infRow.addColumn("paxtype", paxtype);
								infRow.addColumn("birth", brithDay);
								infRow.addColumn("lastname", xing);
								infRow.addColumn("firstname", ming);
								infRow.addColumn("paxsex", paxsex);
								infRow.addColumn("passtype", passtype);
								infRow.addColumn("passno", passno);
								infRow.addColumn("birthcountry", birthcountry);
								infRow.addColumn("issuecountry", issuecountry);
								infRow.addColumn("docexpiry", docexpiry);
								infRow.addColumn("telephone", phone);
								infRow.addColumn("email", email);
								infRow.addColumn("guardian", passengerArray[0].getRefs().toString());
								//infRow.addColumn("guardian", passengers.getPassengerReference().toString());
								
								
								
								
								
							} else if ("DELETE".equals(mode)) {
								// 删除旅客信息
								Row pRow = pTable.addRow();
								pRow.addColumn("mode", "0");
								this.forPassenger(pRow, passengerArray, orderItem, 0,travelerMap);
							}
						} else {
							// passenger不存在表示修改航段信息
							String paxid = passengers.getPassengerArray()[0].getRefs().get(0).toString();
							String mode = orderItem.getActionType().getStringValue().toUpperCase();
							// 如果是TypeA增加航段的时候，则必须传入InvGuaranteeID
							String InvGuaranteeID = "";
							InventoryGuarantee inventoryGuarantee = orderItem.getInventoryGuarantee();
							if (!"".equals(inventoryGuarantee) && inventoryGuarantee != null) {
								if ("CREATE".equals(mode)) {
									InvGuaranteeID = inventoryGuarantee.getInvGuaranteeID();
								}
							}
							String segId = orderItem.getAssociations().getFlight().getSegmentReferencesArray(0)
									.getStringValue();
							Row flightRow = fligthTable.addRow();
							flightRow.addColumn("passno", travelerMap.get(paxid).getParm("passno"));
							flightRow.addColumn("passtype", travelerMap.get(paxid).getParm("passtype"));
							flightRow.addColumn("paxname", travelerMap.get(paxid).getParm("paxname"));
							flightRow.addColumn("carricd", flightMap.get(segId).getParm("aircode"));
							flightRow.addColumn("flightno", flightMap.get(segId).getParm("flno"));
							flightRow.addColumn("flightsuffix", flightMap.get(segId).getParm("suffix"));
							flightRow.addColumn("flightdate", flightMap.get(segId).getParm("date"));
							flightRow.addColumn("oricode", flightMap.get(segId).getParm("oricode"));
							flightRow.addColumn("destcode", flightMap.get(segId).getParm("destcode"));
							flightRow.addColumn("cabin", flightMap.get(segId).getParm("cabin"));
							flightRow.addColumn("InvGuaranteeID", InvGuaranteeID);
							flightRow.addColumn("flightDay", flightMap.get(segId).getParm("date"));
							flightRow.addColumn("mode", "CREATE".equals(mode) ? "1" : "0");
						}
					}
					if (infTable!=null && infTable.getRowCount()>0) {
						CommandData data=new CommandData();
						data.addParm("infTable", infTable);
						RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("infRedis"),JsonUnit.toJson(data), 60);
					}
					input.addParm("paxflts", fligthTable);
					// 添加修改人信息table
					input.addParm("paxs", pTable);
				}
			}
		}
		// 添加座位信息
		Table seatTable = new Table(new String[] { "passno", "passtype", "paxname", "carricd", "flightno",
				"flightsuffix", "flightdate", "oricode", "destcode", "seatno","pid","fid","mode"});
		SeatList seatList = dataLists.getSeatList();
		if (seatList != null && !"".equals(seatList)) {
			ListOfSeatType[] seatsArray = seatList.getSeatsArray();
			if (seatsArray.length > 0) {
				for (ListOfSeatType listOfSeatType : seatsArray) {
					Row seatRow = seatTable.addRow();
					SeatLocationType location = listOfSeatType.getLocation();
					String column = location.getColumn();
					String number = location.getRow().getNumber().getStringValue();
					OrderItemAssociationType associations = location.getAssociations();
					String paxid = associations.getPassengers().getPassengerArray(0).getRefs().get(0).toString();
					String flightid = associations.getFlight().getSegmentReferencesArray(0).getStringValue();
					seatRow.addColumn("passno", travelerMap.get(paxid).getParm("passno").getStringColumn());
					seatRow.addColumn("passtype", travelerMap.get(paxid).getParm("passtype").getStringColumn());
					seatRow.addColumn("paxname", travelerMap.get(paxid).getParm("paxname").getStringColumn());
					seatRow.addColumn("carricd", flightMap.get(flightid).getParm("aircode"));
					seatRow.addColumn("flightno", flightMap.get(flightid).getParm("flno"));
					seatRow.addColumn("flightsuffix", flightMap.get(flightid).getParm("suffix"));
					seatRow.addColumn("flightdate", flightMap.get(flightid).getParm("date"));
					seatRow.addColumn("oricode", flightMap.get(flightid).getParm("oricode"));
					seatRow.addColumn("destcode", flightMap.get(flightid).getParm("destcode"));
					seatRow.addColumn("fid", flightid);
					seatRow.addColumn("pid", paxid);
					String type = listOfSeatType.getActionType().getStringValue().toUpperCase();
					if ("CREATE".equalsIgnoreCase(type)) {
						seatRow.addColumn("mode", 1);
						seatRow.addColumn("seatno", number + column);
					} else {
						seatRow.addColumn("mode", 0);
						seatRow.addColumn("seatno", "");
					}
				}
				input.addParm("seats", seatTable);
			}
		}
		// 追加特服餐食
		Table submarkTable = new Table(new String[] { "passno", "passtype", "paxname", "carricd", "flightno",
				"flightsuffix", "flightdate", "oricode", "destcode", "buynum", "submarketcode", "mode", "paxinft","fid","pid"});
		//兼容增加婴儿失败或成功时，在orderview中都有显示
		Table redisSubTable = new Table(new String[] {"fid","pid"});
		//区别不同的辅营描述，防止重复添加
		Map<String, String> subMap=new HashMap<String, String>(); 
		ServiceList serviceList = dataLists.getServiceList();
		// 兼容指定修改 增加航段的指定修改
		if (serviceList != null && !"".equals(serviceList)) {
			ServiceDetailType[] serviceArray = serviceList.getServiceArray();
			if (serviceArray.length > 0) {
				for (ServiceDetailType service : serviceArray) {
					String submarketcode = service.getServiceID().getStringValue().toUpperCase();
					/**
					 * 辅营描述 start
					 */
					if (!subMap.containsKey(submarketcode)) {
						String description="";
						String name ="";
						ServiceDescriptionType descriptions = service.getDescriptions();
						if (descriptions!=null && !"".equals(descriptions)) {
							Description destion = descriptions.getDescriptionArray()[0];
							if (destion!=null && !"".equals(destion)) {
								Text text =destion.getText();
								if (text!=null && !"".equals(text)) {
									   description = text.stringValue();
									}
							}
						}
						//name
						 org.iata.iata.edist.ServiceCoreType.Name n = service.getName();
						 if (n!=null && !"".equals(n)) {
							 name = n.getStringValue();
						}
						if (!"".equals(description) || !"".equals(name)) {
							CommandData date=new CommandData();
							date.addParm("name", name);
							date.addParm("description", description);
							RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey(submarketcode), JsonUnit.toJson(date), 60);
							subMap.put(submarketcode, submarketcode);
						}
					}
					/**
					 * 辅营描述   end
					 */
					String type = service.getActionType().getStringValue().toUpperCase();
					org.iata.iata.edist.ServiceCoreType.Associations[] associationsArray = service
							.getAssociationsArray();
					for (org.iata.iata.edist.ServiceCoreType.Associations associations : associationsArray) {

						Object paxId = associations.getTraveler().getTravelerReferences().get(0);
						SegmentReferences[] segmentReferencesArray = associations.getFlight()
								.getSegmentReferencesArray();
						// 兼容不传航段信息时，默认全航段加此辅营信息
						if (segmentReferencesArray.length<=0) {
							for (CommandData flightValue : flightMap.values()) {
								// 增加所有航段，拿到map中所有的对应航段信息
								this.setMarket(submarkTable, flightValue, paxId, submarketcode, type,delList,redisSubTable,travelerMap);
							}
						} else {
							String flightId = associations.getFlight().getSegmentReferencesArray(0).getStringValue();
							CommandData flightValue = flightMap.get(flightId);
							this.setMarket(submarkTable, flightValue, paxId, submarketcode, type,delList,redisSubTable,travelerMap);
						}
					}
				}
				if (redisSubTable.getRowCount()>0) {
					CommandData date=new CommandData();
					date.addParm("infService", redisSubTable);
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("infService"), JsonUnit.toJson(date), 60);
				}
				input.addParm("spaxsubmarkets", submarkTable);
			}
		}

	}

	/**
	 * 
	 * @param submarkTable
	 *            要封装的辅营信息
	 * @param flightValue
	 *            当前航班信息
	 * @param paxId
	 *            旅客id
	 * @param submarketcode
	 *            辅营code
	 * @param submarkType
	 *            操作类型：create为添加，cancel为取消
	 * @param delList 
	 * @param redisSubTable 
	 * @param travelerMap 
	 */
	private void setMarket(Table submarkTable, CommandData flightValue, Object paxId, String submarketcode,
			String submarkType, List<String> delList, Table redisSubTable, Map<String, CommandData> travelerMap) {
	//	if (delList.size()>0) {
			if (!delList.contains(flightValue.getParm("seg").getStringColumn())) {
				Row subRow = submarkTable.addRow();
				// 添加对应的辅营信息
				subRow.addColumn("passno", travelerMap.get(paxId).getParm("passno"));
				subRow.addColumn("passtype", travelerMap.get(paxId).getParm("passtype"));
				subRow.addColumn("paxname", travelerMap.get(paxId).getParm("paxname"));
				subRow.addColumn("carricd", flightValue.getParm("aircode"));
				subRow.addColumn("flightno",flightValue.getParm("flno"));
				subRow.addColumn("flightsuffix", flightValue.getParm("suffix"));
				subRow.addColumn("flightdate", flightValue.getParm("date"));
				subRow.addColumn("oricode", flightValue.getParm("oricode"));
				subRow.addColumn("destcode", flightValue.getParm("destcode"));
				subRow.addColumn("buynum", 1);
				subRow.addColumn("submarketcode", submarketcode);
				subRow.addColumn("mode", "CREATE".equals(submarkType) ? "1" : "0");
				subRow.addColumn("pid",paxId.toString());
				subRow.addColumn("fid",flightValue.getParm("seg").getStringColumn());
				// 增加婴儿
				if ("INFT".equals(submarketcode)) {
					if ("CREATE".equals(submarkType)) {
						// 兼容追加特服婴儿时 详情中显示
						Row infRow = redisSubTable.addRow();
						infRow.addColumn("pid", paxId.toString());
						infRow.addColumn("fid", flightValue.getParm("seg").getStringColumn());
					}
						// 当追加一个婴儿时需要有此信息 可能会有追加两个航段的信息
						CommandData inftData = new CommandData();
						String inftid = travelerMap.get(paxId).getParm("inftid").getStringColumn();
						inftData.addParm("firstname", travelerMap.get(inftid).getParm("ming"));
						inftData.addParm("lastname", travelerMap.get(inftid).getParm("xing"));
						inftData.addParm("paxname", travelerMap.get(inftid).getParm("paxname"));
						inftData.addParm("paxtype", travelerMap.get(inftid).getParm("paxtype"));
						inftData.addParm("birth", travelerMap.get(inftid).getParm("birth"));
						String paxsex = travelerMap.get(inftid).getParm("paxsex").getStringColumn();
						if ("".equals(paxsex)) {
							inftData.addParm("paxsex", "");
						}else {
							inftData.addParm("paxsex", paxsex.equals("Female") ? "F" : "M");
						}
						inftData.addParm("telephone", travelerMap.get(inftid).getParm("phone"));
						inftData.addParm("email", travelerMap.get(inftid).getParm("email"));
						inftData.addParm("passtype", travelerMap.get(inftid).getParm("passtype"));
						inftData.addParm("passno", travelerMap.get(inftid).getParm("passno"));
						inftData.addParm("docexpiry", travelerMap.get(inftid).getParm("docexpiry"));
						inftData.addParm("issuecountry", travelerMap.get(inftid).getParm("issuecountry"));
						inftData.addParm("birthcountry", travelerMap.get(inftid).getParm("birthcountry"));
						subRow.addColumn("paxinft", inftData);
				} else {
					subRow.addColumn("paxinft", "");
				}
			}
		
	}

	/**
	 * 
	 * @param pRow
	 *            需要封装的数据结构
	 * @param passengerArray
	 *            入参
	 * @param orderItem
	 * @param i
	 *            1为修改人信息 0为删除对应的人信息
	 * @param travelerMap 
	 */
	private void forPassenger(Row pRow, org.iata.iata.edist.PassengerDocument.Passenger[] passengerArray,
			OrderItem orderItem, int i, Map<String, CommandData> travelerMap) {
		PTC ptc = passengerArray[0].getPTC();
		String paxType = "";
		if (!"".equals(ptc) && ptc != null) {
			paxType = passengerArray[0].getPTC().getStringValue();
		}
		Age age = passengerArray[0].getAge();
		String brithDay = "";
		if (age != null && !"".equals(age)) {
			BirthDate birthDate = age.getBirthDate();
			if (birthDate != null) {
				brithDay = age.getBirthDate().getStringValue();
			}
		}
		// 人信息
		String paxid = passengerArray[0].getRefs().toString();
		//String paxid = orderItem.getAssociations().getPassengers().getPassengerReference().get(0).toString();
		// 取旅客姓名，修改和删除取值不一样
		Name name = passengerArray[0].getName();
		String fName = "";
		String lName = "";
		String oldFName = "";
		String oldLName = "";
		if (i == 1) {
			if (name != null && !"".equals(name)) {
				lName = name.getSurname().getStringValue();
				fName= name.getGivenArray(0).getStringValue();
			}
			oldLName = travelerMap.get(paxid).getParm("xing").toString();
			oldFName = travelerMap.get(paxid).getParm("ming").toString();
		} else {
			lName = travelerMap.get(paxid).getParm("xing").toString();
			fName = travelerMap.get(paxid).getParm("ming").toString();
			oldFName=fName;
			oldLName=lName;
		}
		Contacts contacts = passengerArray[0].getContacts();
		String phone = "";
		String email = "";
		String paxsex = "";
		if (contacts != null && !"".equals(contacts)) {
			 Contact contactArray = contacts.getContactArray(0);
			if (contactArray != null && !"".equals(contactArray)) {
				 PhoneContactType phoneContact = contactArray.getPhoneContact();
				if (phoneContact !=null && !"".equals(phoneContact)) {
					Number[] numberArray = phoneContact.getNumberArray();
					if (numberArray!=null && !"".equals(numberArray)) {
						phone = numberArray[0].getStringValue();
					}
				}
				EmailType emailContact = contactArray.getEmailContact();
				if (emailContact!=null && !"".equals(emailContact)) {
					Address address = emailContact.getAddress();
					if (address!=null && !"".equals(address)) {
						email = address.getStringValue();
					}
				}
			}
		}
		Gender gender = passengerArray[0].getGender();
		if (gender != null && !"".equals(gender)) {
			paxsex = gender.getStringValue();
		}
		PassengerIDInfo passengerIDInfo = passengerArray[0].getPassengerIDInfo();
		String type = "";
		String number = "";
		String docexpiry = "";
		String issuecountry = "";
		String birthcountry = "";
		if (passengerIDInfo != null && !"".equals(passengerIDInfo)) {
			FOID foid = passengerIDInfo.getFOID();
			if (foid != null && !"".equals(foid)) {
				type = foid.getType();
				number = foid.getID().getStringValue();
			}
			PassengerDocument[] passengerDocumentArray = passengerIDInfo.getPassengerDocumentArray();
			int length = passengerDocumentArray.length;
			if (length > 0) {
				PassengerDocument array = passengerIDInfo.getPassengerDocumentArray(0);
				type = array.getType();
				number = array.getID();
				Calendar dateOfExpiration = array.getDateOfExpiration();
				if (!"".equals(dateOfExpiration) && dateOfExpiration != null) {
					docexpiry = DateUtils.getInstance().formatDate(dateOfExpiration, "yyyyMMdd");
				}
				// 证件签发国
				issuecountry = array.getCountryOfIssuance();
				// 出生国
				birthcountry = array.getBirthCountry();
			}
		}
		pRow.addColumn("paxtype", paxType);
		pRow.addColumn("birth", brithDay);
		pRow.addColumn("firstname", fName);
		pRow.addColumn("lastname", lName);
		pRow.addColumn("telephone", phone);
		pRow.addColumn("email", email);
		pRow.addColumn("docexpiry", docexpiry);
		pRow.addColumn("issuecountry", issuecountry);
		pRow.addColumn("birthcountry", birthcountry);
		if ("".equals(paxsex)) {
			pRow.addColumn("paxsex", "");
		} else if ("Female".equals(paxsex)) {
			pRow.addColumn("paxsex", "F");
		} else {
			pRow.addColumn("paxsex", "M");
		}
		pRow.addColumn("passtype", type);
		pRow.addColumn("passno", number);
		pRow.addColumn("oldlastname", oldLName);
		pRow.addColumn("oldfirstname", oldFName);
		pRow.addColumn("oldpassno", travelerMap.get(paxid).getParm("passno"));
		pRow.addColumn("oldpasstype", travelerMap.get(paxid).getParm("passtype"));
		pRow.addColumn("oldpaxname", travelerMap.get(paxid).getParm("paxname"));
		pRow.addColumn("oldpaxtype", travelerMap.get(paxid).getParm("paxtype"));
	}

	/**
	 * 
	 * @param paxRet
	 *            存放旅客信息的对象
	 * @param pArry
	 *            传入的旅客信息 拼接好的旅客信息
	 */

	private void getPassenger(CommandData paxRet, Passenger pArry, Map<String, CommandData> travelerMap) {
		String paxid = pArry.getObjectKey();
		PassengerIDInfo passengerIDInfo = pArry.getPassengerIDInfo();
		String passtype = "";
		String passno = "";
		String docexpiry = "";
		String issuecountry = "";
		String birthcountry = "";
		if (passengerIDInfo != null && !"".equals(passengerIDInfo)) {
			FOID foid = passengerIDInfo.getFOID();
			if (foid != null && !"".equals(foid)) {
				passtype = foid.getType();
				passno = foid.getID().getStringValue();
			}
			PassengerDocument[] passengerDocumentArray = passengerIDInfo.getPassengerDocumentArray();
			int length = passengerDocumentArray.length;
			if (length > 0) {
				PassengerDocument array = passengerIDInfo.getPassengerDocumentArray(0);
				passtype = array.getType();
				passno = array.getID();
				// 证件有效期
				if (!"".equals(array.getDateOfExpiration()) && array.getDateOfExpiration() != null) {
					docexpiry = DateUtils.getInstance().formatDate(array.getDateOfExpiration(), "yyyyMMdd");
				}
				// 证件签发国
				issuecountry = array.getCountryOfIssuance();
				// 出生国
				birthcountry = array.getBirthCountry();
			}
		}
		// 证件类型
		paxRet.addParm("passtype", passtype);
		// 证件号
		paxRet.addParm("passno", passno);
		paxRet.addParm("docexpiry", docexpiry);
		paxRet.addParm("issuecountry", issuecountry);
		paxRet.addParm("birthcountry", birthcountry);
		// 姓名
		String xing = pArry.getName().getSurname().getStringValue();
		String ming = pArry.getName().getGivenArray(0).getStringValue();
		// 存放对应的婴儿信息
		if (pArry.getPassengerAssociation() != null && !"".equals(pArry.getPassengerAssociation())) {
			paxRet.addParm("inftid", pArry.getPassengerAssociation());
		} else {
			paxRet.addParm("inftid", "");
		}
		// 生日
		Age age = pArry.getAge();
		String brithDay = "";
		if (age != null) {
			BirthDate birthDate = age.getBirthDate();
			if (birthDate != null) {
				brithDay = age.getBirthDate().getStringValue();
			}
		}
		Contacts contacts = pArry.getContacts();
		// 电话，联系信息，性别
		String phone = "";
		String email = "";
		String paxsex = "";
		if (contacts != null && !"".equals(contacts)) {
			Contact contact = contacts.getContactArray(0);
			if (contact != null && !"".equals(contact)) {
				phone = contact.getPhoneContact().getNumberArray(0).getStringValue();
				email = contact.getEmailContact().getAddress().getStringValue();
			}
		}
		Gender gender = pArry.getGender();
		if (gender != null && !"".equals(gender)) {
			paxsex = gender.getStringValue();
		}
		paxRet.addParm("phone", phone);
		paxRet.addParm("email", email);
		paxRet.addParm("paxsex", paxsex);
		paxRet.addParm("birth", brithDay);
		String name = xing.toUpperCase() + ming.toUpperCase();
		paxRet.addParm("paxname", name);
		// 用于整体判断同时中文或英文
		if (Pattern.matches("^([a-zA-Z-'`~!@#%=_<>;:\"\\+\\{\\}\\(\\^\\?\\.\\*\\|\\)\\$\\[\\]\\,\\& ·0-9]{2,70})$",
				name)) {
			// 说明是英文名
			paxRet.addParm("paxname", xing.toUpperCase() + "/" + ming.toUpperCase());
		}
		paxRet.addParm("xing", xing.toUpperCase());
		paxRet.addParm("ming", ming.toUpperCase());
		// 人的类型 儿童,成人
		PTC ptc = pArry.getPTC();
		String paxtype = "";
		if (ptc != null) {
			paxtype = ptc.getStringValue();
		}
		paxRet.addParm("paxtype", paxtype);
		travelerMap.put(paxid, paxRet);
	}

	/***************************************************************************/

	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input ) {
		APIOrderCreateNDCONEE orderCreateONEE = new APIOrderCreateNDCONEE();
		Table errorTable = input.getParm("errorTable").getTableColumn();
		commandRet.addParm("errorTable", errorTable);
		return orderCreateONEE.transResponseBeanToXmlBean(commandRet, input);
	}

	/**
	 * 设置旅客与辅营的关联关系
	 * 
	 * @param paxid
	 * @param submarketid
	 * @param subMap
	 */
	public void serSubmarketsMap(String paxid, Row submarkets, Map<String, List<Row>> subMap) {
		List<Row> submarketList = subMap.get(paxid);
		if (null == submarketList) {
			submarketList = new ArrayList<Row>();
		}
		submarketList.add(submarkets);
		subMap.put(paxid, submarketList);
	}

	/**
	 * 辅营与品牌关联的节点
	 * 
	 * @param listOfServiceBundleArry
	 */
	public void addServiceBundleListArry(Mapping mapping, ListOfServiceBundleType listOfServiceBundleArry) {
		Map<String, String> ServiceBundleMap = mapping.getServiceBundleMap();
		for (Entry<String, String> freeEntry : ServiceBundleMap.entrySet()) {
			// 辅营id
			String submarketid = freeEntry.getValue().trim();
			if (!"".equals(submarketid) && null != submarketid) {
				ServiceBundle serviceBundleArry = listOfServiceBundleArry.addNewServiceBundle();
				String[] submarketidS = submarketid.split(" ");
				// 商品 id
				String brandid = freeEntry.getKey();
				serviceBundleArry.setListKey(brandid);
				serviceBundleArry.setItemCount(BigInteger.valueOf(submarketidS.length));
				org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle.Associations associationsArry = serviceBundleArry
						.addNewAssociations();
				for (String id : submarketidS) {
					associationsArry.addNewServiceReference().setStringValue(id);
				}
			}
		}
	}

	/**
	 * 航班于OD关联
	 * 
	 * @param commandRet
	 * @param flightListArry
	 * @param originArry
	 */
	public void addOriginArryArry(CommandRet commandRet, FlightList flightListArry, OriginDestinationList originArry,
			Mapping mapping) {
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		String oricode = "";
		String destcode = "";
		String routtypeS = "";
		String routtypeR = "";
		int flt = 1;
		for (Row flights : flightsTable) {
			org.iata.iata.edist.DataListType.FlightList.Flight flightArry = flightListArry.addNewFlight();
			// 出发地三字码
			oricode = flights.getColumn("oricode").getStringColumn();
			// 到达地三字码
			destcode = flights.getColumn("destcode").getStringColumn();
			// 航班编号
			String flightno = flights.getColumn("flightno").getStringColumn();
			String routtype = flights.getColumn("routtype").getStringColumn();
			flightArry.setFlightKey(FLT + flt);
			flightArry.addNewSegmentReferences().setStringValue(mapping.getAllMap().get(flightno));
			if ("G".equals(routtype)) {
				routtypeS = FLT + flt + " " + routtypeS;
			} else {
				routtypeR = FLT + flt + " " + routtypeR;
			}
			flt = flt + 1;
		}
		if ("".equals(routtypeR)) {
			OriginDestination originDestinationArry = originArry.addNewOriginDestination();
			originDestinationArry.addNewDepartureCode().setStringValue(oricode);
			originDestinationArry.addNewArrivalCode().setStringValue(destcode);
			originDestinationArry.addNewFlightReferences().setStringValue(routtypeS.trim());
		} else {
			OriginDestination originDestinationArry1 = originArry.addNewOriginDestination();
			originDestinationArry1.addNewDepartureCode().setStringValue(oricode);
			originDestinationArry1.addNewArrivalCode().setStringValue(destcode);
			originDestinationArry1.addNewFlightReferences().setStringValue(routtypeS.trim());
			OriginDestination originDestinationArry2 = originArry.addNewOriginDestination();
			originDestinationArry2.addNewDepartureCode().setStringValue(destcode);
			originDestinationArry2.addNewArrivalCode().setStringValue(oricode);
			originDestinationArry2.addNewFlightReferences().setStringValue(routtypeR.trim());
		}
	}

	public void addSeats(CommandRet commandRet, Mapping mapping) {
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		if (null != seatsTable) {
			for (Row seats : seatsTable) {
				mapping.getSeatsMap().put(seats.getColumn("flightid").getStringColumn(), seats);
			}
		}
	}

	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return list
	 */
	public List<String> getList(String args) {
		List<String> list = new ArrayList<String>();
		list.add(args);
		return list;
	}

	private static final class Mapping {
		private Map<String, String> allMap = new HashMap<String, String>();
		/**
		 * 辅营信息 String 免费服务 id Row 服务内容
		 */
		private Map<String, String> ServiceBundleMap = new HashMap<String, String>();

		private Map<String, Row> SeatsMap = new HashMap<String, Row>();

		public Map<String, String> getServiceBundleMap() {
			return ServiceBundleMap;
		}

		public Map<String, Row> getSeatsMap() {
			return SeatsMap;
		}

		public Map<String, String> getAllMap() {
			return allMap;
		}
	}
}
