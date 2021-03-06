package com.jx.blackface.paycenter.controllers;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.alipay.util.AlipayNotify;
import com.jx.argo.ActionResult;
import com.jx.argo.BeatContext;
import com.jx.argo.annotations.Path;
import com.jx.blackface.gaea.usercenter.entity.OldOrderBFGEntity;
import com.jx.blackface.gaea.usercenter.entity.PayProcessBFGEntity;
import com.jx.blackface.orderplug.buzs.PayBuz;
import com.jx.blackface.orderplug.common.MD5;
import com.jx.blackface.orderplug.frame.RSBLL;
import com.jx.blackface.paycenter.annotaion.CheckLogin;
import com.jx.blackface.paycenter.frame.PSF;
import com.jx.blackface.paycenter.utools.Constants;
import com.jx.blackface.servicecoreclient.entity.PayOrderBFGEntity;
import com.jx.blackface.tools.blackTrack.entity.WebLogs;


public class ZFBPayController extends PayBaseController{

	
	@Path("/paycheckzfbmobile/{orderid:\\d+}")
	public ActionResult zfbmobcheckpay(long orderid){
		WebLogs logs = WebLogs.getIntanse(ZFBPayController.class, "zfbmobcheckpay");
		String trade_ret = beat().getRequest().getParameter("trade_status");
		String issuccess = beat().getRequest().getParameter("is_success");
		
		logs.putParam("payorderid", orderid);
		logs.putParam("trade_ret", trade_ret);
		logs.putParam("issuccess", issuccess);
		
		PayOrderBFGEntity order = null;
		try {
			order = RSBLL.rb.getPayOrderService().getPayOrderByid(orderid);//getPayRecordBFGService().loadPayRecordByid(orderid);
		} catch (Exception e) {
				// TODO Auto-generated catch block
			logs.putParam("exception", e.getStackTrace());
				e.printStackTrace();
		}finally{
			logs.printInfoLog();
		}
		
		if( null != trade_ret && "TRADE_SUCCESS".equalsIgnoreCase(trade_ret) && checkZfb()){
			if(order != null){//新系统
				try {
					PayBuz.pb.updatePay(orderid,2);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logs.putParam("exceptionupdate", e.getStackTrace());
				}finally{
					logs.printInfoLog();
				}
			}
		}
		return redirect("/m/payresult/"+orderid);
	}
	@Path("/zfbmoble/{orderid:\\d+}")
	public ActionResult zfbpaymoblie(long orderid){
		//orderid = new Date().getTime();
		WebLogs logs = WebLogs.getIntanse(ZFBPayController.class, "zfbpaymoblie");
		String notify_url = "http://pay.lvzheng.com/zfbnotifycheck/mobile/"+orderid;
		String return_url = "http://pay.lvzheng.com/paycheckzfbmobile/"+orderid;
		logs.putParam("payorderid", orderid);
		
		float needpay = 0;
		String subject = "小微律政";
		PayOrderBFGEntity orderE = null;
		if(orderid > 0){
			try {
				orderE  = PSF.getPayOrderbfgService().getPayOrderByid(orderid);//RSBLL.getstance().getISorderService().getSorderEntityByid(orderid);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(orderE != null){
			needpay = orderE.getPaycount();
			if(orderE.getPaystate() == 1){
				return redirect("/payresult/"+orderid);
			}
			
		}
		
		String pars = "_input_charset=utf-8&enable_paymethod=directPay^bankPay^cartoon^cash&notify_url="+notify_url+"&out_trade_no="+orderid+"&partner="+Constants.ali_seller_id+"&payment_type=1&return_url="+return_url+"&"
				+ "seller_id="+Constants.ali_seller_id+"&service=alipay.wap.create.direct.pay.by.user&show_url=http://pay.lvzheng.com/m/payresult/"+orderid+"&"
				+ "subject="+subject+"&total_fee="+needpay;
		
		System.out.println("zfb zfb pars"+pars);
		logs.putParam("beforesignpars", pars);
		String sign = MD5.sign(pars, Constants.pubkey, "utf-8");
		beat().getModel().add("orderid", orderid);
		beat().getModel().add("partner", Constants.ali_seller_id);
		beat().getModel().add("subject", subject);
		beat().getModel().add("total_fee", needpay);
		beat().getModel().add("paymethod", "creditPay");
		beat().getModel().add("return_url", return_url);
		beat().getModel().add("notify_url", notify_url);
		beat().getModel().add("sign", sign);
		logs.printInfoLog();
		return view("zfb/mobilezfb");
	}
	@Path("/zfbpay/{orderid:\\d+}")
	@CheckLogin
	public ActionResult zfbpay(long orderid){
		WebLogs logs = WebLogs.getIntanse(ZFBPayController.class, "zfbpay");
		String tt = beat().getRequest().getParameter("ttt");
		float needpay = 0;
		String returl = beat().getRequest().getParameter("path");
		
		String notify_url = "http://pay.lvzheng.com/zfbnotifycheck/"+orderid;
		String return_url = "http://pay.lvzheng.com/paycheckzfb/"+orderid;
		if(null != returl && !"".equals(returl)){
			return_url = returl;
		}
		String subject = "小微律政";
		PayOrderBFGEntity orderE = null;
		logs.putParam("payorderid", orderid);
		if(orderid > 0){
			try {
				orderE  = PSF.getPayOrderbfgService().getPayOrderByid(orderid);//RSBLL.getstance().getISorderService().getSorderEntityByid(orderid);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(orderE != null){
			needpay = orderE.getPaycount();
			if(orderE.getPaystate() == 1){
				return redirect("/payresult/"+orderid);
			}
		}else{
			OldOrderBFGEntity obf = null;
			try {
				obf = RSBLL.rb.getOldOrderBFGService().loadOrderbyid(orderid);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(obf != null){
				if(obf.getPaystate() == 2){
					return redirect("/payresult/"+orderid);
				}
				needpay = obf.getPaymoneycount(); 
			}
		}
		if(null != tt && !"".equals(tt)){
			needpay = 0.01f;
			beat().getModel().add("ttt", "99999999");
		}
		String pars = "_input_charset=utf-8&enable_paymethod=directPay^bankPay^cartoon^cash^creditCardExpress^debitCardExpress&notify_url="+notify_url+
				"&out_trade_no="+orderid+"&partner="+Constants.ali_seller_id+
				"&payment_type=1&return_url="+return_url+"&"
				+ "seller_id="+Constants.ali_seller_id+"&service=create_direct_pay_by_user&"
				+ "subject="+subject+"&total_fee="+needpay;
		System.out.println("zfb zfb pars"+pars);
		logs.putParam("beforsignpars", pars);
		String sign = MD5.sign(pars, Constants.pubkey, "utf-8");
		beat().getModel().add("orderid", orderid);
		beat().getModel().add("partner", Constants.ali_seller_id);
		beat().getModel().add("subject", subject);
		beat().getModel().add("total_fee", needpay);
		beat().getModel().add("return_url", return_url);
		beat().getModel().add("paymethod", "creditPay");
		beat().getModel().add("notify_url", notify_url);
		beat().getModel().add("sign", sign);
		return view("zfb/zfb");
	}
	@Path("/zfbnotifycheck/{orderid:\\d+}")
public ActionResult zfbnofiycheck(final long orderid){
		
		return new ActionResult(){

			@Override
			public void render(BeatContext beatContext) {
				// TODO Auto-generated method stub
				WebLogs logs = WebLogs.getIntanse(ZFBPayController.class, "zfbnofiycheck");
				String parsstr = beat().getRequest().getQueryString();
				//String ret = beat().getRequest().getParameter("is_success");
				String trade_ret = beat().getRequest().getParameter("trade_status");
				String pmoney = beat().getRequest().getParameter("total_fee");
				String ooid = beat().getRequest().getParameter("out_trade_no");
				logs.putParam("payid", orderid);
				logs.putParam("trade_ret", trade_ret);
				logs.putParam("out_trade_no", ooid);
				PayOrderBFGEntity order = null;
				try {
					order = RSBLL.rb.getPayOrderService().getPayOrderByid(orderid);//getPayRecordBFGService().loadPayRecordByid(orderid);
				} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logs.putParam("exception", e.getStackTrace());
				}finally{
					logs.printInfoLog();
				}
				
				if( null != trade_ret  && "TRADE_SUCCESS".equalsIgnoreCase(trade_ret) && checkZfb()){
					if(order != null){//新系统
						try {
							PayBuz.pb.updatePay(orderid,2);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							logs.printErrorLog(e);
						}
					}else{//老系统
						
		    			OldOrderBFGEntity ole = null;
		    			try {
							ole = RSBLL.rb.getOldOrderBFGService().loadOrderbyid(orderid);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    			if(ole != null && ole.getPaystate() != 2){
		    				ole.setPaystate(2);
		    				ole.setUpdatetime(new Date().getTime());
		    				try {
								RSBLL.rb.getOldOrderBFGService().updateOrder(ole);
								PayProcessBFGEntity ppe = new PayProcessBFGEntity();
								
								ppe.setContents("支付宝支付");
								ppe.setOpempid(0);
								ppe.setOptime(new Date().getTime());
								ppe.setOptype(1);
								ppe.setPaychannel(5);
								ppe.setPayfee(0);
								ppe.setPaynumber(Float.parseFloat(pmoney));
								ppe.setPaytotal(Float.parseFloat(pmoney));
								ppe.setOrderid(orderid);
								
								ppe.setPaystate(2);
								
								try {
									RSBLL.rb.getPayProcessBFGService().addNewPayProcess(ppe);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									logs.printErrorLog(e);
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								logs.printErrorLog(e);
							}
		    			}
					}
				}
				
				
				try {
					beat().getResponse().getWriter().print("success");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logs.printErrorLog(e);
				}
				
			}
			
		};
	}
	@Path("/paycheckzfb/{orderid:\\d+}")
	public ActionResult checkZfbpay(long orderid){
		System.out.println("come into zfb checkZfbpay order id is "+orderid);
		String ret = beat().getRequest().getParameter("is_success");
		String trade_ret = beat().getRequest().getParameter("trade_status");
		String pmoney = beat().getRequest().getParameter("total_fee");
		PayOrderBFGEntity order = null;
		try {
			order = RSBLL.rb.getPayOrderService().getPayOrderByid(orderid);//getPayRecordBFGService().loadPayRecordByid(orderid);
		} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		if( null != trade_ret && "TRADE_SUCCESS".equalsIgnoreCase(trade_ret) && checkZfb()){
			if(order != null){//新系统
				try {
					PayBuz.pb.updatePay(orderid,2);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{//老系统
    			OldOrderBFGEntity ole = null;
    			try {
					ole = RSBLL.rb.getOldOrderBFGService().loadOrderbyid(orderid);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			if(ole != null && ole.getPaystate() != 2){
    				ole.setPaystate(2);
    				ole.setUpdatetime(new Date().getTime());
    				try {
						RSBLL.rb.getOldOrderBFGService().updateOrder(ole);
						PayProcessBFGEntity ppe = new PayProcessBFGEntity();
						
						ppe.setContents("支付宝支付");
						ppe.setOpempid(0);
						ppe.setOptime(new Date().getTime());
						ppe.setOptype(1);
						ppe.setPaychannel(5);
						ppe.setPayfee(0);
						ppe.setPaynumber(Float.parseFloat(pmoney));
						ppe.setPaytotal(Float.parseFloat(pmoney));
						ppe.setOrderid(orderid);
						
						ppe.setPaystate(2);
						
						try {
							RSBLL.rb.getPayProcessBFGService().addNewPayProcess(ppe);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
			}
			
		}
		return redirect("/payresult/"+orderid);
	}
	private boolean checkZfb(){
		boolean f = false;
		Map<String,String> params = new HashMap<String,String>();
		Map requestParams = beat().getRequest().getParameterMap();
		
		for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			String[] values = (String[]) requestParams.get(name);
			String valueStr = "";
			for (int i = 0; i < values.length; i++) {
				valueStr = (i == values.length - 1) ? valueStr + values[i]
						: valueStr + values[i] + ",";
			}
			//乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
			//valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
			params.put(name, valueStr);
		}
		f = AlipayNotify.verify(params);
		System.out.println("check result is ======="+f);
		return f;
	}
}
