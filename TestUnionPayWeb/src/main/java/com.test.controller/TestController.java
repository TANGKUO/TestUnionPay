package com.test.controller;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import unionpay.UnionPay;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import unionpay.sdk.AcpService;
import unionpay.sdk.SDKConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/test")
public class TestController {

    @RequestMapping(value = "/tran", method = RequestMethod.GET, produces = "text/html;charset=UTF-8")
    @ResponseBody()
    public void tran(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=utf-8");
        String merId = request.getParameter("merId");
        String txnAmt = request.getParameter("txnAmt");
        String orderId = request.getParameter("orderId");

        String txnTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String reanderHtml = UnionPay.wapPay(merId, txnAmt, orderId, txnTime);
        System.out.println(reanderHtml);

        // return  UnionPay.wapPay(merId, txnAmt, orderId, txnTime);
        response.getWriter().print(reanderHtml);
    }

    @RequestMapping(value = "/notice", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public void notice(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("text/html; charset=utf-8");
        String encoding = req.getParameter(SDKConstants.param_encoding);
        String pageResult = "";
        Map<String, String> respParam = getAllRequestParam(req);


        //TODO 以下代码没有用直接从respParam取参数记录数据库，如果存在需要更新,签名需要验证 AcpService.validate(respParam, encoding)

        Map<String, String> valideData = null;
        StringBuffer page = new StringBuffer();
        page.append("<table>");
        if (null != respParam && !respParam.isEmpty()) {
            Iterator<Map.Entry<String, String>> it = respParam.entrySet()
                    .iterator();
            valideData = new HashMap<String, String>(respParam.size());
            while (it.hasNext()) {
                Map.Entry<String, String> e = it.next();
                String key = (String) e.getKey();
                String value = (String) e.getValue();
                value = new String(value.getBytes(encoding), encoding);
                page.append("<tr><td width=\"30%\" align=\"right\">" + key
                        + "(" + key + ")</td><td>" + value + "</td></tr>");
                valideData.put(key, value);
            }
        }
        if (!AcpService.validate(valideData, encoding)) {
            page.append("<tr><td width=\"30%\" align=\"right\">验证签名结果</td><td>失败</td></tr>");
        } else {
            page.append("<tr><td width=\"30%\" align=\"right\">验证签名结果</td><td>成功</td></tr>");
            System.out.println(valideData.get("orderId")); //其他字段也可用类似方式获取

            String respCode = valideData.get("respCode");
        }
        page.append("</table>");
        resp.getWriter().print(page);
    }

    @RequestMapping(value = "/afterNotice", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public void afterNotice(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String encoding = req.getParameter(SDKConstants.param_encoding);
        // 获取银联通知服务器发送的后台通知参数
        Map<String, String> reqParam = getAllRequestParamStream(req);

        //TODO:取参数插入数据库reqParam.get

        //重要！验证签名前不要修改reqParam中的键值对的内容，否则会验签不过
        if (!AcpService.validate(reqParam, encoding)) {
            //验签失败，需解决验签问题

        } else {
            //【注：为了安全验签成功才应该写商户的成功处理逻辑】交易成功，更新商户订单状态

            String orderId = reqParam.get("orderId"); //获取后台通知的数据，其他字段也可用类似方式获取
            String respCode = reqParam.get("respCode");
            //判断respCode=00、A6后，对涉及资金类的交易，请再发起查询接口查询，确定交易成功后更新数据库。

        }
        //返回给银联服务器http 200  状态码
        resp.getWriter().print("ok");
    }

    /**
     * 获取请求参数中所有的信息
     * 当商户上送frontUrl或backUrl地址中带有参数信息的时候，
     * 这种方式会将url地址中的参数读到map中，会导多出来这些信息从而致验签失败，这个时候可以自行修改过滤掉url中的参数或者使用getAllRequestParamStream方法。
     *
     * @param request
     * @return
     */
    public static Map<String, String> getAllRequestParam(
            final HttpServletRequest request) {
        Map<String, String> res = new HashMap<String, String>();
        Enumeration<?> temp = request.getParameterNames();
        if (null != temp) {
            while (temp.hasMoreElements()) {
                String en = (String) temp.nextElement();
                String value = request.getParameter(en);
                res.put(en, value);
                // 在报文上送时，如果字段的值为空，则不上送<下面的处理为在获取所有参数数据时，判断若值为空，则删除这个字段>
                if (res.get(en) == null || "".equals(res.get(en))) {
                    // System.out.println("======为空的字段名===="+en);
                    res.remove(en);
                }
            }
        }
        return res;
    }

    /**
     * 获取请求参数中所有的信息。
     * 非struts可以改用此方法获取，好处是可以过滤掉request.getParameter方法过滤不掉的url中的参数。
     * struts可能对某些content-type会提前读取参数导致从inputstream读不到信息，所以可能用不了这个方法。理论应该可以调整struts配置使不影响，但请自己去研究。
     * 调用本方法之前不能调用req.getParameter("key");这种方法，否则会导致request取不到输入流。
     *
     * @param request
     * @return
     */
    public static Map<String, String> getAllRequestParamStream(
            final HttpServletRequest request) {
        Map<String, String> res = new HashMap<String, String>();
        try {
            String notifyStr = new String(IOUtils.toByteArray(request.getInputStream()), "utf-8");
            //LogUtil.writeLog("收到通知报文：" + notifyStr);
            String[] kvs = notifyStr.split("&");
            for (String kv : kvs) {
                String[] tmp = kv.split("=");
                if (tmp.length >= 2) {
                    String key = tmp[0];
                    String value = URLDecoder.decode(tmp[1], "utf-8");
                    res.put(key, value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            // LogUtil.writeLog("getAllRequestParamStream.UnsupportedEncodingException error: " + e.getClass() + ":" + e.getMessage());
        } catch (IOException e) {
            //    LogUtil.writeLog("getAllRequestParamStream.IOException error: " + e.getClass() + ":" + e.getMessage());
        }
        return res;
    }
}
