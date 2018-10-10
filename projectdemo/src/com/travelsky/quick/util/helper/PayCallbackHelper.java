package com.travelsky.quick.util.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.Base64;

public class PayCallbackHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayCallbackHelper.class);

    /**
     * 线上支付回调
     *
     * @param context
     * @param payLogId
     * @return
     */
    public CommandRet olPay(SelvetContext <ApiContext> context, String payLogId) {
        CommandData input = context.getInput();
        // 只获取底层需要的参数
        String orderno = input.getParm("orderno").getStringColumn();
        long money = input.getParm("payamount").getLongColumn();
        String paytype = input.getParm("paytype").getStringColumn();
        String paysucno = input.getParm("msg").getStringColumn();
        String bankid = input.getParm("bankid").getStringColumn();
        String billno = input.getParm("billno").getStringColumn();
        String orgid = input.getParm("orgid").getStringColumn();
        String apptype = input.getParm("AppType").getStringColumn();
        if ("".equals(apptype)) {
            apptype = input.getParm("apptype").getStringColumn();
        }
        String logid = StringUtils.hasLength(payLogId) ? payLogId : "";

        // 调底层获取结果. 如果失败，尝试不间断调用5次，只要有一次成功，则返回，否则认为失败
        CommandRet ret = new CommandRet("");
        for (int i = 0; i < 5; i++) {
            CommandInput input0 = new CommandInput("com.cares.sh.order.order.pay");

            input0.addParm("orderno", orderno);
            input0.addParm("money", money);
            input0.addParm("paytype", paytype);
            input0.addParm("paysucno", paysucno);
            input0.addParm("bankid", bankid);
            input0.addParm("billno", billno);
            input0.addParm("orgid", orgid);
            input0.addParm("apptype", apptype);
            input0.addParm("logid", logid);
            input0.addParm("nomember", "Y");

            ret = context.doOther(input0, false);
            if (!ret.isError()) {
                LOGGER.info("Pay log success.");
                return ret;
            } else {
                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                    LOGGER.error(ret.getErrorCode() + ":" + ret.getErrorDesc());
                }
            }
        }

        LOGGER.info(String.format("Call pay failed. orderno:%s,money:%s,paysucno:%s,error:%s",
                orderno,
                input.getParm("payamount").getStringColumn(),
                input.getParm("msg").getStringColumn(),
                ret.getErrorDesc()));

        return ret;
    }


    /**
     * adyenOnlinePayCallBack
     *
     * @param context
     * @return
     */
    public CommandRet adyenOnlinePay(SelvetContext <ApiContext> context) {
        CommandRet ret = new CommandRet("");
        CommandInput input0 = new CommandInput("com.travelsky.quick.pay.adyen.receiveNotification");
        CommandData input = context.getInput();
        input.copyTo(input0);
        ret = context.doOther(input0, false);
        if (!ret.isError()) {
            LOGGER.info("Pay log success.");
            return ret;
        } else {
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                LOGGER.error(ret.getErrorCode() + ":" + ret.getErrorDesc());
            }
        }
        return ret;
    }

    /**
     * adyenOnlinePayResult
     *
     * @param context
     * @return
     */
    public CommandRet adyenOnlinePayResult(SelvetContext <ApiContext> context) {
        CommandRet ret = new CommandRet("");
        CommandInput input0 = new CommandInput("com.travelsky.quick.pay.adyen.receiveResult");
        CommandData input = context.getInput();
        input.copyTo(input0);
        ret = context.doOther(input0, false);
        return ret;
    }

    /**
     * 记录线上支付日志，并调用底层改变票状态
     *
     * @param context
     * @return
     */
    public CommandRet olPayLog(SelvetContext <ApiContext> context) {
        CommandData input = context.getInput();
        // 只获取底层需要的参数
        String orderno = input.getParm("orderno").getStringColumn();
        long money = input.getParm("payamount").getLongColumn();
        String paytype = input.getParm("paytype").getStringColumn();
        String paysucno = input.getParm("msg").getStringColumn();
        String bankid = input.getParm("bankid").getStringColumn();
        String billno = input.getParm("billno").getStringColumn();
        String orgid = input.getParm("orgid").getStringColumn();
        String apptype = input.getParm("AppType").getStringColumn();
        if ("".equals(apptype)) {
            apptype = input.getParm("apptype").getStringColumn();
        }

        // 调底层获取结果. 如果失败，尝试不间断调用5次，只要有一次成功，则返回，否则认为失败
        CommandRet ret = new CommandRet("");
        for (int i = 0; i < 5; i++) {
            CommandInput input0 = new CommandInput("com.cares.sh.order.order.paylog");
            input0.addParm("orderno", orderno);
            input0.addParm("money", money);
            input0.addParm("paytype", paytype);
            input0.addParm("paysucno", paysucno);
            input0.addParm("bankid", bankid);
            input0.addParm("billno", billno);
            input0.addParm("orgid", orgid);
            input0.addParm("apptype", apptype);
            input0.addParm("nomember", "Y");
            ret = context.doOther(input0, false);
            if (!ret.isError()) {
                LOGGER.info("Pay log success.");
                return ret;
            }
        }

        LOGGER.info(String.format("Call pay log failed. orderno:%s,money:%s,paysucno:%s,error:%s",
                orderno,
                input.getParm("payamount").getStringColumn(),
                input.getParm("msg").getStringColumn(),
                ret.getErrorDesc()));

        return ret;
    }

    /**
     * easypay验签
     *
     * @param context
     * @return
     */
    public boolean verfiySign(SelvetContext <ApiContext> context) {
        CommandData input = context.getInput();


        String apptype = input.getParm("AppType").getStringColumn();
        if ("".equals(apptype)) {
            apptype = input.getParm("apptype").getStringColumn();
        }

        String data = input.getParm("payamount").getStringColumn() + "&" +
                apptype + "&" +
                input.getParm("bankid").getStringColumn() + "&" +
                input.getParm("billno").getStringColumn() + "&" +
                input.getParm("ext1").getStringColumn() + "&" +
                input.getParm("ext2").getStringColumn() + "&" +
                input.getParm("lan").getStringColumn() + "&" +
                input.getParm("msg").getStringColumn() + "&" +
                input.getParm("ordercurtype").getStringColumn() + "&" +
                input.getParm("ordertype").getStringColumn() + "&" +
                input.getParm("orgid").getStringColumn() + "&" +
                input.getParm("paydate").getStringColumn() + "&" +
                input.getParm("paytime").getStringColumn() + "&" +
                input.getParm("orderno").getStringColumn() + "&" +
                input.getParm("paystatus").getStringColumn() + "&";
        String sign = context.getInput().getParm("signature").getStringColumn();

        boolean ret = false;
        Path path = null;
        InputStream in = null;
        try {
            path = Paths.get(CommonConstants.CONF_PATH + "CAcert.pem");
            in = Files.newInputStream(path);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cacClient = cf.generateCertificate(in);
            PublicKey pubClient = cacClient.getPublicKey();
            Signature rsa = Signature.getInstance("MD5withRSA");
            rsa.initVerify(pubClient);
            rsa.update(data.getBytes());
            ret = rsa.verify(Base64.decode(sign.toCharArray()));

            if (!ret) {
                LOGGER.error("Easypay pay callback {}. JSON is:{}",
                        TipMessager.getInfoMessage(ErrCodeConstants.API_DATA_SIGN, context.getContext().getLanguage()),
                        JsonUnit.toJson(context.getInput()));
            }
        } catch (Exception ex) {
            Unit.process(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
        return ret;
    }

    /**
     * 通知回调接口（order notification）
     *
     * @param context
     * @return
     */
    public CommandRet worldPay(SelvetContext <ApiContext> context) {
        CommandInput input = new CommandInput("com.travelsky.quick.pay.worldpay.receiveNotification");
        HttpServletRequest request = context.getRequest();
        String reqXML = getXML(request);
        LOGGER.info("worldPay notification(By Request) xml:" + reqXML);
        CommandData paymentServiceData = null;

        if (StringUtils.hasLength(reqXML)) {
        	String lastEvent = getLastEvent(reqXML);
            if ("AUTHORISED".equals(lastEvent)) {
                //授权通知
                paymentServiceData = parseXMLForAUTHORISED(reqXML);
            } else if ("REFUSED".equals(lastEvent)) {
                //支付拒绝
                paymentServiceData = parseXMLForREFUSED(reqXML);
            } else if ("CAPTURED".equals(lastEvent)) {
                //付款
                paymentServiceData = parseXMLForCAPTURED(reqXML);
            } else if ("CANCELLED".equals(lastEvent) || "EXPIRED".equals(lastEvent)) {
                //取消付款
                paymentServiceData = parseXMLForCANCELLED(reqXML);
            } else if ("SENT_FOR_REFUND".equals(lastEvent) || "REFUNDED".equals(lastEvent)) {
                //退款
                paymentServiceData = parseXMLForSENT_FOR_REFUND(reqXML);
            } else if ("ERROR".equals(lastEvent) || "REFUND_FAILED".equals(lastEvent)) {
                paymentServiceData = parseXMLForAUTHORISED(reqXML);
            }
        }
        input.addParm("notification", reqXML);
        input.addParm("paymentService", paymentServiceData);
        LOGGER.info("api parse Notification:" + paymentServiceData);
        CommandRet ret = context.doOther(input, false);
        if (!ret.isError()) {
            LOGGER.info("WorldPay notification success.");
        }
        return ret;
    }
    
    /**
     * 获取lastEvent
     * @param xml
     * @return
     */
    private String getLastEvent(String xml){
    	String result = "";
    	 //解析xml
        try {
            Document document = DocumentHelper.parseText(xml);
            Element paymentSerElement = document.getRootElement();
            if(paymentSerElement != null){
            	Element notify = paymentSerElement.element("notify");
            	if(notify != null){
            		Element orderStatusEvent = notify.element("orderStatusEvent");
            		if(orderStatusEvent != null){
            			Element payment = orderStatusEvent.element("payment");
            			result = getElementText(payment, "lastEvent");
            		}
            	}
            }
        } catch (DocumentException e) {
            LOGGER.info("parse xml error:[{}]", e);
        }
    	return result;
    }
    
    /**
     * 获取请求中的xml
     *
     * @param request
     * @return
     */
    private String getXML(HttpServletRequest request) {
        SAXReader saxReader = null;
        Document doc = null;
        String xmlStr = "";
        try {
            saxReader = new SAXReader();
            doc = saxReader.read(request.getInputStream());
            xmlStr = doc.asXML();
        } catch (Exception e) {
            LOGGER.info("获取请求中xml数据失败");
        }
        return xmlStr;
    }

    /**
     * 授权通知(AUTHORISED)
     *
     * @return
     */
    private CommandData parseXMLForAUTHORISED(String reqXML) {
        CommandData paymentServiceData = new CommandData();
        CommandData paymentData = new CommandData();
        CommandData amountData = new CommandData();
        CommandData journalData = new CommandData();
        //解析xml
        try {
            Document document = DocumentHelper.parseText(reqXML);
            Element paymentSerElement = document.getRootElement();
            paymentServiceData.addParm("merchantCode", getAttributeValue(paymentSerElement, "merchantCode"));

            Element notify = paymentSerElement.element("notify");
            Element orderStatusEvent = notify.element("orderStatusEvent");
            paymentServiceData.addParm("orderCode", getAttributeValue(orderStatusEvent, "orderCode"));

            Element payment = orderStatusEvent.element("payment");
            if (payment == null) {
                return null;
            }
            paymentData.addParm("paymentMethod", getElementText(payment, "paymentMethod"));

            parsePaymentDetail(payment, paymentData);

            Element amount1 = payment.element("amount");
            amountData.addParm("value", getAttributeValue(amount1, "value"));
            amountData.addParm("currencyCode", getAttributeValue(amount1, "currencyCode"));
            amountData.addParm("exponent", getAttributeValue(amount1, "exponent"));
            paymentData.addParm("amount", amountData);

            paymentData.addParm("lastEvent", getElementText(payment, "lastEvent"));

            Element AuthorisationId = payment.element("AuthorisationId");
            paymentData.addParm("AuthorisationId", getAttributeValue(AuthorisationId, "id"));

            Element CVCResultCode = payment.element("CVCResultCode");
            paymentData.addParm("CVCResultCode", getAttributeValue(CVCResultCode, "description"));

            Element AVSResultCode = payment.element("AVSResultCode");
            paymentData.addParm("AVSResultCode", getAttributeValue(AVSResultCode, "description"));

            Element AAVAddressResultCode = payment.element("AAVAddressResultCode");
            paymentData.addParm("AAVAddressResultCode", getAttributeValue(AAVAddressResultCode, "description"));

            Element AAVPostcodeResultCode = payment.element("AAVPostcodeResultCode");
            paymentData.addParm("AAVPostcodeResultCode", getAttributeValue(AAVPostcodeResultCode, "description"));

            Element AAVCardholderNameResultCode = payment.element("AAVCardholderNameResultCode");
            paymentData.addParm("AAVCardholderNameResultCode", getAttributeValue(AAVCardholderNameResultCode, "description"));

            Element AAVTelephoneResultCode = payment.element("AAVTelephoneResultCode");
            paymentData.addParm("AAVTelephoneResultCode", getAttributeValue(AAVTelephoneResultCode, "description"));

            Element AAVEmailResultCode = payment.element("AAVEmailResultCode");
            paymentData.addParm("AAVEmailResultCode", getAttributeValue(AAVEmailResultCode, "description"));

            Element ThreeDSecureResult = payment.element("ThreeDSecureResult");
            paymentData.addParm("ThreeDSecureResult", getAttributeValue(ThreeDSecureResult, "description"));

            paymentData.addParm("cardHolderName", getElementText(payment, "cardHolderName"));
            paymentData.addParm("issuerCountryCode", getElementText(payment, "issuerCountryCode"));
            paymentData.addParm("cardNumber", getElementText(payment, "cardNumber"));

            Element riskScore = payment.element("riskScore");
            paymentData.addParm("riskScore", getAttributeValue(riskScore, "value"));
            paymentServiceData.addParm("payment", paymentData);

            Element journal = orderStatusEvent.element("journal");
            journalData.addParm("journalType", getAttributeValue(journal, "journalType"));
            journalData.addParm("sent", getAttributeValue(journal, "sent"));

            Element bookingDateEle = journal.element("bookingDate");
            if (bookingDateEle != null) {
                Element bookingDate = bookingDateEle.element("date");
                journalData.addParm("bookingDate", getAttributeValue(bookingDate, "year") + "-"
                        + getAttributeValue(bookingDate, "month") + "-" + getAttributeValue(bookingDate, "dayOfMonth"));
            }
            paymentServiceData.addParm("journal", journalData);
        } catch (DocumentException e) {
            LOGGER.info("parse xml error:[{}]", e);
        }
        return paymentServiceData;
    }

    /**
     * 支付拒绝(REFUSED)
     *
     * @return
     */
    private CommandData parseXMLForREFUSED(String reqXML) {
        CommandData paymentServiceData = new CommandData();
        CommandData paymentData = new CommandData();
        CommandData amountData = new CommandData();
        CommandData ISO8583ReturnCodeData = new CommandData();
        CommandData journalData = new CommandData();
        //解析xml
        try {
            Document document = DocumentHelper.parseText(reqXML);
            Element paymentSerElement = document.getRootElement();
            paymentServiceData.addParm("merchantCode", getAttributeValue(paymentSerElement, "merchantCode"));

            Element notify = paymentSerElement.element("notify");
            Element orderStatusEvent = notify.element("orderStatusEvent");
            paymentServiceData.addParm("orderCode", getAttributeValue(orderStatusEvent, "orderCode"));

            Element payment = orderStatusEvent.element("payment");
            if (payment == null) {
                return null;
            }
            paymentData.addParm("paymentMethod", getElementText(payment, "paymentMethod"));

            parsePaymentDetail(payment, paymentData);

            Element amount1 = payment.element("amount");
            amountData.addParm("value", getAttributeValue(amount1, "value"));
            amountData.addParm("currencyCode", getAttributeValue(amount1, "currencyCode"));
            amountData.addParm("exponent", getAttributeValue(amount1, "exponent"));

            paymentData.addParm("amount", amountData);

            Element ISO8583ReturnCode = payment.element("ISO8583ReturnCode");
            ISO8583ReturnCodeData.addParm("code", getAttributeValue(ISO8583ReturnCode, "code"));
            ISO8583ReturnCodeData.addParm("description", getAttributeValue(ISO8583ReturnCode, "description"));
            paymentData.addParm("ISO8583ReturnCode", ISO8583ReturnCodeData);
            paymentData.addParm("lastEvent", getElementText(payment, "lastEvent"));


            Element CVCResultCode = payment.element("CVCResultCode");
            paymentData.addParm("CVCResultCode", getAttributeValue(CVCResultCode, "description"));

            Element AVSResultCode = payment.element("AVSResultCode");
            paymentData.addParm("AVSResultCode", getAttributeValue(AVSResultCode, "description"));

            Element AAVAddressResultCode = payment.element("AAVAddressResultCode");
            paymentData.addParm("AAVAddressResultCode", getAttributeValue(AAVAddressResultCode, "description"));

            Element AAVPostcodeResultCode = payment.element("AAVPostcodeResultCode");
            paymentData.addParm("AAVPostcodeResultCode", getAttributeValue(AAVPostcodeResultCode, "description"));

            Element AAVCardholderNameResultCode = payment.element("AAVCardholderNameResultCode");
            paymentData.addParm("AAVCardholderNameResultCode", getAttributeValue(AAVCardholderNameResultCode, "description"));

            Element AAVTelephoneResultCode = payment.element("AAVTelephoneResultCode");
            paymentData.addParm("AAVTelephoneResultCode", getAttributeValue(AAVTelephoneResultCode, "description"));

            Element AAVEmailResultCode = payment.element("AAVEmailResultCode");
            paymentData.addParm("AAVEmailResultCode", getAttributeValue(AAVEmailResultCode, "description"));

            Element ThreeDSecureResult = payment.element("ThreeDSecureResult");
            paymentData.addParm("ThreeDSecureResult", getAttributeValue(ThreeDSecureResult, "description"));

            Element riskScore = payment.element("riskScore");
            if (riskScore != null) {
                paymentData.addParm("riskScore", getAttributeValue(riskScore, "value"));
            }
            paymentServiceData.addParm("payment", paymentData);

            Element journal = orderStatusEvent.element("journal");
            journalData.addParm("journalType", getAttributeValue(journal, "journalType"));
            journalData.addParm("sent", getAttributeValue(journal, "sent"));

            Element bookingDateEle = journal.element("bookingDate");

            if (bookingDateEle != null) {
                Element bookingDate = bookingDateEle.element("date");
                journalData.addParm("bookingDate", getAttributeValue(bookingDate, "year") + "-"
                        + getAttributeValue(bookingDate, "month") + "-" + getAttributeValue(bookingDate, "dayOfMonth"));
            }
            paymentServiceData.addParm("journal", journalData);
        } catch (DocumentException e) {
            LOGGER.info("parse xml error:[{}]", e);
        }
        return paymentServiceData;
    }

    /**
     * 付款(CAPTURED)
     *
     * @return
     */
    private CommandData parseXMLForCAPTURED(String reqXML) {
        CommandData paymentService = new CommandData();
        CommandData paymentData = new CommandData();
        CommandData amountData = new CommandData();
        CommandData balanceData = new CommandData();
        CommandData balAmountData = new CommandData();
        CommandData journalData = new CommandData();
        CommandData journalReferenceData = new CommandData();
        //解析xml
        try {
            Document document = DocumentHelper.parseText(reqXML);
            Element paymentSerElement = document.getRootElement();
            paymentService.addParm("merchantCode", getAttributeValue(paymentSerElement, "merchantCode"));

            Element notify = paymentSerElement.element("notify");
            Element orderStatusEvent = notify.element("orderStatusEvent");
            paymentService.addParm("orderCode", getAttributeValue(orderStatusEvent, "orderCode"));

            Element payment = orderStatusEvent.element("payment");
            if (payment == null) {
                return null;
            }
            paymentData.addParm("paymentMethod", getElementText(payment, "paymentMethod"));

            Element amount1 = payment.element("amount");
            amountData.addParm("value", getAttributeValue(amount1, "value"));
            amountData.addParm("currencyCode", getAttributeValue(amount1, "currencyCode"));
            amountData.addParm("exponent", getAttributeValue(amount1, "exponent"));
            paymentData.addParm("amount", amountData);
            paymentData.addParm("lastEvent", getElementText(payment, "lastEvent"));
            paymentData.addParm("reference", getElementText(payment, "reference"));

            Element balance = payment.element("balance");
            balanceData.addParm("accountType", getAttributeValue(balance, "accountType"));
            if (balance != null) {
                Element amount2 = balance.element("amount");
                balAmountData.addParm("value", getAttributeValue(amount2, "value"));
                balAmountData.addParm("currencyCode", getAttributeValue(amount2, "currencyCode"));
                balAmountData.addParm("exponent", getAttributeValue(amount2, "exponent"));
            }
            balanceData.addParm("amount", balAmountData);
            paymentData.addParm("balance", balanceData);

            paymentData.addParm("cardNumber", getElementText(payment, "cardNumber"));

            Element riskScore = payment.element("riskScore");
            paymentData.addParm("riskScore", getAttributeValue(riskScore, "value"));

            Element journal = orderStatusEvent.element("journal");
            journalData.addParm("journalType", getAttributeValue(journal, "journalType"));
            journalData.addParm("sent", getAttributeValue(journal, "sent"));

            Element bookingDateEle = journal.element("bookingDate");
            if (bookingDateEle != null) {
                Element date = bookingDateEle.element("date");
                String bookingDate = getAttributeValue(date, "year") + "-"
                        + getAttributeValue(date, "month") + "-" + getAttributeValue(date, "dayOfMonth");
                journalData.addParm("bookingDate", bookingDate);
            }

            Element journalReference = journal.element("journalReference");
            journalReferenceData.addParm("type", getAttributeValue(journalReference, "type"));
            journalReferenceData.addParm("reference", getAttributeValue(journalReference, "reference"));

            journalData.addParm("journalReference", journalReferenceData);
            paymentService.addParm("payment", paymentData);
            paymentService.addParm("journal", journalData);
            return paymentService;
        } catch (DocumentException e) {
            LOGGER.info("parse xml error:[{}]", e);
        }
        return null;
    }

    /**
     * 取消付款(CANCELLED)
     *
     * @return
     */
    private CommandData parseXMLForCANCELLED(String reqXML) {
        CommandData paymentServiceData = new CommandData();
        CommandData paymentData = new CommandData();
        CommandData amountData = new CommandData();
        CommandData journalData = new CommandData();
        //解析xml
        try {
            Document document = DocumentHelper.parseText(reqXML);
            Element paymentSerElement = document.getRootElement();
            paymentServiceData.addParm("merchantCode", getAttributeValue(paymentSerElement, "merchantCode"));

            Element notify = paymentSerElement.element("notify");
            Element orderStatusEvent = notify.element("orderStatusEvent");
            paymentServiceData.addParm("orderCode", getAttributeValue(orderStatusEvent, "orderCode"));

            Element payment = orderStatusEvent.element("payment");
            if (payment == null) {
                return null;
            }
            paymentData.addParm("paymentMethod", getElementText(payment, "paymentMethod"));
            Element amount1 = payment.element("amount");
            amountData.addParm("value", getAttributeValue(amount1, "value"));
            amountData.addParm("currencyCode", getAttributeValue(amount1, "currencyCode"));
            amountData.addParm("exponent", getAttributeValue(amount1, "exponent"));
            paymentData.addParm("amount", amountData);
            paymentData.addParm("lastEvent", getElementText(payment, "lastEvent"));

            Element CVCResultCode = payment.element("CVCResultCode");
            paymentData.addParm("CVCResultCode", getAttributeValue(CVCResultCode, "description"));

            Element AVSResultCode = payment.element("AVSResultCode");
            paymentData.addParm("AVSResultCode", getAttributeValue(AVSResultCode, "description"));

            Element AAVAddressResultCode = payment.element("AAVAddressResultCode");
            paymentData.addParm("AAVAddressResultCode", getAttributeValue(AAVAddressResultCode, "description"));

            Element AAVPostcodeResultCode = payment.element("AAVPostcodeResultCode");
            paymentData.addParm("AAVPostcodeResultCode", getAttributeValue(AAVPostcodeResultCode, "description"));

            Element AAVCardholderNameResultCode = payment.element("AAVCardholderNameResultCode");
            paymentData.addParm("AAVCardholderNameResultCode", getAttributeValue(AAVCardholderNameResultCode, "description"));

            Element AAVTelephoneResultCode = payment.element("AAVTelephoneResultCode");
            paymentData.addParm("AAVTelephoneResultCode", getAttributeValue(AAVTelephoneResultCode, "description"));

            Element AAVEmailResultCode = payment.element("AAVEmailResultCode");
            paymentData.addParm("AAVEmailResultCode", getAttributeValue(AAVEmailResultCode, "description"));

            Element ThreeDSecureResult = payment.element("ThreeDSecureResult");
            paymentData.addParm("ThreeDSecureResult", getAttributeValue(ThreeDSecureResult, "description"));

            paymentData.addParm("cardNumber", getElementText(payment, "cardNumber"));

            Element riskScore = payment.element("riskScore");
            paymentData.addParm("riskScore", getAttributeValue(riskScore, "description"));
            paymentServiceData.addParm("payment", paymentData);


            Element journal = orderStatusEvent.element("journal");
            journalData.addParm("journalType", getAttributeValue(journal, "journalType"));
            journalData.addParm("sent", getAttributeValue(journal, "sent"));

            Element bookingDateEle = journal.element("bookingDate");
            if (bookingDateEle != null) {
                Element bookingDate = bookingDateEle.element("date");
                journalData.addParm("bookingDate", getAttributeValue(bookingDate, "year") + "-"
                        + getAttributeValue(bookingDate, "month") + "-" + getAttributeValue(bookingDate, "dayOfMonth"));
            }
            paymentServiceData.addParm("journal", journalData);

        } catch (DocumentException e) {
            LOGGER.info("parse xml error:[{}]", e);
        }
        return paymentServiceData;
    }

    /**
     * 退款(SENT_FOR_REFUND)
     *
     * @return
     */
    private CommandData parseXMLForSENT_FOR_REFUND(String reqXML) {
        CommandData paymentServiceData = new CommandData();
        CommandData paymentData = new CommandData();
        CommandData amountData = new CommandData();
        CommandData balanceData = new CommandData();
        CommandData balanceAmountData = new CommandData();
        CommandData journalData = new CommandData();
        CommandData journalReferenceData = new CommandData();
        //解析xml
        try {
            Document document = DocumentHelper.parseText(reqXML);
            Element paymentSerElement = document.getRootElement();
            paymentServiceData.addParm("merchantCode", getAttributeValue(paymentSerElement, "merchantCode"));
            Element notify = paymentSerElement.element("notify");
            Element orderStatusEvent = notify.element("orderStatusEvent");
            String orderCode = getAttributeValue(orderStatusEvent, "orderCode");
            paymentServiceData.addParm("orderCode", orderCode);

            Element payment = orderStatusEvent.element("payment");
            if (payment == null) {
                return null;
            }

            paymentData.addParm("paymentMethod", getElementText(payment, "paymentMethod"));

            Element amount = payment.element("amount");
            amountData.addParm("value", getAttributeValue(amount, "value"));
            amountData.addParm("currencyCode", getAttributeValue(amount, "currencyCode"));
            amountData.addParm("exponent", getAttributeValue(amount, "exponent"));
            paymentData.addParm("amount", amountData);

            String lastEvent = getElementText(payment, "lastEvent");
            paymentData.addParm("lastEvent", lastEvent);
            String reference = getElementText(payment, "reference");
            paymentData.addParm("reference", reference);

            LOGGER.info("orderCode:{}, lastEvent:{}, reference:{}", orderCode, lastEvent, reference);

            Element balance = payment.element("balance");
            if (balance != null) {
                balanceData.addParm("accountType", getAttributeValue(balance, "accountType"));
                Element accAmount = balance.element("amount");
                balanceAmountData.addParm("value", getAttributeValue(accAmount, "value"));
                balanceAmountData.addParm("currencyCode", getAttributeValue(accAmount, "currencyCode"));
                balanceAmountData.addParm("exponent", getAttributeValue(accAmount, "exponent"));
                balanceData.addParm("amount", balanceAmountData);
            }

            paymentData.addParm("cardNumber", getElementText(payment, "cardNumber"));

            Element riskScore = payment.element("riskScore");
            paymentData.addParm("riskScore", getAttributeValue(riskScore, "value"));
            paymentServiceData.addParm("payment", paymentData);

            Element journal = orderStatusEvent.element("journal");
            journalData.addParm("journalType", getAttributeValue(journal, "journalType"));
            journalData.addParm("sent", getAttributeValue(journal, "sent"));

            Element bookingDateEle = journal.element("bookingDate");
            if (bookingDateEle != null) {
                Element bookingDate = bookingDateEle.element("date");
                journalData.addParm("bookingDate", getAttributeValue(bookingDate, "year") + "-"
                        + getAttributeValue(bookingDate, "month") + "-" + getAttributeValue(bookingDate, "dayOfMonth"));
            }

            Element journalReference = journal.element("journalReference");
            journalReferenceData.addParm("type", getAttributeValue(journalReference, "type"));
            journalReferenceData.addParm("reference", getAttributeValue(journalReference, "reference"));
            journalData.addParm("journalReference", journalReferenceData);

            paymentServiceData.addParm("journal", journalData);
        } catch (DocumentException e) {
            LOGGER.info("parse xml error:[{}]", e);
        }
        return paymentServiceData;
    }

    /**
     * 获取当前节点下的属性值
     *
     * @param element
     * @param name
     * @return
     */
    private String getAttributeValue(Element element, String name) {
        if (element == null) {
            return "";
        }
        return element.attributeValue(name);
    }

    /**
     * 获取当前节点下的值
     *
     * @param element
     * @param name
     * @return
     */
    private String getElementText(Element element, String name) {
        if (element == null) {
            return "";
        }
        return element.elementTextTrim(name);
    }

    private void parsePaymentDetail(Element payment, CommandData paymentData) {
        Element paymentMethodDetail = payment.element("paymentMethodDetail");
        if (paymentMethodDetail == null) {
            return;
        }
        Element card = paymentMethodDetail.element("card");
        String number = card.attributeValue("number");
        String type = card.attributeValue("type");
        paymentData.addParm("cardnumber", number);
        paymentData.addParm("type", type);

        Element expiryDate = card.element("expiryDate");
        Element date = expiryDate.element("date");
        String expiryYear = date.attributeValue("year");
        String expiryMonth = date.attributeValue("month");
        paymentData.addParm("expiryDate", expiryYear + "-" + expiryMonth);
    }

}
