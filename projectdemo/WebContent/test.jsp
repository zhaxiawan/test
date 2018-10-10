<%@page import="org.omg.CORBA.PUBLIC_MEMBER"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@  page import="java.io.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>接口用例测试</title>
<script type="text/javascript" src="js/jquery-2.1.4.min.js"></script>
<style type="text/css">

/*中间部分面板样式*/
#main {
	width: 100%;
	height: 100%;
	border: 1px #F00 solid;
}

.left {
	float: left;
	width: 20%;
	height: 100%;
	margin-: 0px;
}

.right {
	float: right;
	width: 80%;
	height: 500px;
	margin-right: 0px;
}

.righttop {
	width: 100%;
	height: 100%;
}


.nonetd{
width: 0px;
height: 0px;
}
#selectin {
	width: 80%;
	height: 10%;
	font-size: 25px;
}

#righttoptable {
	width: 100%;
	height: 90%;
	font-size: 15px;
}

.tr {
	width: 100%;
	height: 10%;
}

#select {
	margin-top: 0px;
}

.xmltr {
	width: 100%;
	height: 300px;
}

#xmlrqtext {
	width: 100%;
	height: 100%;
}
#xmlrq {
	width: 500px;
	height: 300px;
}
#xmlrs {
	width: 482px;
	height: 302px;
}

#xmlrstext {
	width: 100%;
	height: 100%;
}

#sendbtn {
	width: 5%;
	height: 15px;
}
</style>
<script type="text/javascript">
	$(document).ready(function() {

	});
	
	
	function changeform(v) {
		if (v == 'shopping') {
			$.ajax({
				url : "/projectdemo/getdata",
				cache : false,
				type : "POST",
				dataType : "text",
				data : "p=shopping",
				success : function(data) {
				}
			});

		} else if (v == 'ordercreate') {
			$.ajax({
				url : "/projectdemo/getdata",
				cache : false,
				type : "POST",
				dataType : "text",
				data : "p=ordercreate",
				success : function(data) {
				}
			});
		}
	}
	function changestate() {
		$("#rqxmlbox").attr("checked", "checked");
		$("#rqxmlbox").attr("disabled", "disabled");
		var path = $("#selectin").val() + $("#select").val();

	}
	function getdataxml() {
		var params = serializeForm('dataform'); 
		var reqxml=$("#xmlrqtext").val();
		params=params+"&ReqXML="+reqxml;
		$.ajax({
			url : "/projectdemo/ndc",
			cache : false,
			type : "POST",
			dataType : "text",
			data : params,
			success : function(data) {
				$("#xmlrstext").val(data);
			}
		});
		
	
	  
	}
	//获取单个input中的【name,value】数组 
	function inputSelector(element) {  
	 if (element.checked)  
	   return [element.name, element.value];  
	}  
	    
	function input(element) {  
	  switch (element.type.toLowerCase()) {  
	   case 'submit':  
	   case 'hidden':  
	   case 'password':  
	   case 'text':  
	    return [element.name, element.value];  
	   case 'checkbox':  
	   case 'radio':  
	    return inputSelector(element);  
	  }  
	  return false;  
	}  
	  
	//组合URL 
	function serializeElement(element) {  
	  var method = element.tagName.toLowerCase();  
	  var parameter = input(element);  
	   
	  if (parameter) {  
	   var key = encodeURIComponent(parameter[0]);  
	   if (key.length == 0) return;  
	   
	   if (parameter[1].constructor != Array)  
	    parameter[1] = [parameter[1]];  
	      
	   var values = parameter[1];  
	   var results = [];  
	   for (var i=0; i<values.length; i++) {  
	    results.push(key + '=' + encodeURIComponent(values[i]));  
	   }  
	   return results.join('&');  
	  }  
	}
	  
	function getElements(formId) {    
	    var form = document.getElementById(formId);    
	    var elements = new Array();    
	    var tagElements = form.getElementsByTagName('input');    
	    for (var j = 0; j < tagElements.length; j++){  
	         elements.push(tagElements[j]);  
	  
	    }  
	    return elements;    
	}
	//调用方法     
	function serializeForm(formId) {    
	    var elements = getElements(formId);    
	    var queryComponents = new Array();    
	    
	    for (var i = 0; i < elements.length; i++) {    
	      var queryComponent = serializeElement(elements[i]);    
	      if (queryComponent)    
	        queryComponents.push(queryComponent);    
	    }    
	    
	    return queryComponents.join('&');  
	}  
</script>

</head>
<body>


	<div id="main">
		<div class="left">
			<select id="selectin" onchange="changeform(this.value)">
				<option value="shopping">查询品牌</option>
				<option value="ordercreate">订单创建</option>
			</select>

		</div>
		<div class="right">
			<div id="righttop" class="righttop">
			<input type="text" value="192.168.82.88/api/ndc"></input>
					<button id="sendbtn" onclick="getdataxml()"></button>
				<form  id="dataform" action="">
					<table id="righttoptable">
						<tr class="tr">
							<td><input disabled="disabled" checked="checked"
								class="checkbox" type="checkbox" /></td>
							<td>ServiceName</td>
							<td><input name="ServiceName" type="text"
								value="LCC_AIRSHOPPING_SERVICE" /></td>
							<td><input class="checkbox" checked="checked"
								type="checkbox" disabled="disabled" /></td>
							<td>AuthUserID</td>
							<td><input name="AuthUserID" type="text" value="ANONYMOUS" />
							</td>
						</tr>

						<tr class="tr">
							<td><input disabled="disabled" checked="checked"
								class="checkbox" type="checkbox" /></td>
							<td>AuthAppID</td>
							<td><input name="AuthAppID" type="text" value="WEB" /></td>
							<td><input class="checkbox" checked="checked"
								type="checkbox" disabled="disabled" /></td>
							<td>AuthTktdeptid</td>
							<td><input name="AuthTktdeptid" type="text" value="SYS_SELF_DEPT" />
							</td>
						</tr>
						<tr class="tr">
							<td><input disabled="disabled" checked="checked"
								class="checkbox" type="checkbox" /></td>
							<td>Language</td>
							<td><input name="Language" type="text" value="en_US" /></td>
							<td><input class="checkbox" checked="checked"
								type="checkbox" disabled="disabled" /></td>
							<td>Version</td>
							<td><input name="Version" type="text" value="1.0" /></td>
						</tr>
						<tr class="tr">
							<td><input disabled="disabled" checked="checked"
								class="checkbox" type="checkbox" /></td>
							<td>Timestamp</td>
							<td><input name="Timestamp" type="text"
								value="20180812070706" /></td>
							<td><input class="checkbox" checked="checked"
								type="checkbox" disabled="disabled" /></td>
							<td>Sign</td>
							<td><input name="Sign" type="text"
								value="FAR0yHJXlcprFnA7Qy06mZGEsvUPzwnriBzV" /></td>
						</tr>
						<tr class="tr">
							<td><input disabled="disabled" checked="checked"
								class="checkbox" type="checkbox" /></td>
							<td>TimeZone</td>
							<td><input name="TimeZone" type="text" value="+8:00" /></td>
							<td><input class="checkbox" checked="checked"
								type="checkbox" disabled="disabled" /></td>
							<td>ClientIP</td>
							<td><input name="ClientIP" type="text" value="127.0.0.1" />
							</td>
						</tr>
						<tr class="xmltr">
							<td id="xmlrq">xmlrq: <textarea id="xmlrqtext" name="ReqXML"></textarea>
							</td>
							<td id="xmlrs">xmlrs: <textarea id="xmlrstext"></textarea>
							</td>
							<td calss="nonetd"></td>
							<td calss="nonetd"></td>
						</tr>
					</table>
				</form>

			</div>
		</div>

	</div>


</body>
</html>