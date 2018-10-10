package com.travelsky.quick.util.helper;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
/**
 * 类说明:打印行程单对ibe接口
 * @author qichunyu
 * @date 2016.07.04
 */
public class PrintReceiptManager {
	
	
	/*
	 * @desc 查询行程单
	 * @param CommandData  
	 * @param SelvetContext<ApiContext>  
	 * @return CommandRet 
	 */
	public CommandRet printReceipt(SelvetContext<ApiContext> context){
		CommandInput l_input = new CommandInput("");
		CommandData input =  context.getInput();
		input.copyTo(l_input);
		l_input.setName("com.cares.sh.ibe.t4");
		l_input.setCmdId("");
		l_input.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		CommandRet l_ret = context.doOther(l_input,false);
		l_ret.copyTo(context.getRet());
		if("9999".equals(l_ret.getErrorCode())){
			context.getRet().setError("9999", l_ret.getErrorDesc()+"(系统间调用出错，api系统到interface系统)");
		}else if("9990".equals(l_ret.getErrorCode())){
			context.getRet().setError("9990", l_ret.getErrorDesc()+"(api系统找不到interface系统,系统间调用失败)");
		}else{
			context.getRet().setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
		}
		return l_ret;
	}

}
