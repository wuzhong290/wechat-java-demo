package com.wechat.login;

import me.chanjar.weixin.common.util.StringUtils;
import me.chanjar.weixin.mp.api.WxMpConfigStorage;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wuzhong on 2015/7/20.
 */
@RestController
public class CheckController {

    @Autowired
    private WxMpConfigStorage wxMpConfigStorage;
    @Autowired
    private WxMpService wxMpService;
    @Autowired
    private WxMpMessageRouter wxMpMessageRouter;

    @RequestMapping("coreJoin")
    public void coreJoin(HttpServletRequest request,HttpServletResponse response){
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        String signature = request.getParameter("signature");
        String nonce = request.getParameter("nonce");
        String timestamp = request.getParameter("timestamp");

        System.out.println(signature);

        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            // 消息签名不正确，说明不是公众平台发过来的消息
            try {
                response.getWriter().println("非法请求");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String echostr = request.getParameter("echostr");
        if (StringUtils.isNotBlank(echostr)) {
            // 说明是一个仅仅用来验证的请求，回显echostr
            try {
                response.getWriter().println(echostr);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String encryptType = StringUtils.isBlank(request.getParameter("encrypt_type")) ?
                "raw" :
                request.getParameter("encrypt_type");

        if ("raw".equals(encryptType)) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = null;
            try {
                inMessage = WxMpXmlMessage.fromXml(request.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
            if (outMessage != null) {
                try {
                    response.getWriter().write(outMessage.toXml());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        if ("aes".equals(encryptType)) {
            // 是aes加密的消息
            String msgSignature = request.getParameter("msg_signature");
            WxMpXmlMessage inMessage = null;
            try {
                inMessage = WxMpXmlMessage.fromEncryptedXml(request.getInputStream(), wxMpConfigStorage, timestamp, nonce, msgSignature);
            } catch (IOException e) {
                e.printStackTrace();
            }
            WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
            try {
                response.getWriter().write(outMessage.toEncryptedXml(wxMpConfigStorage));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            response.getWriter().println("不可识别的加密类型");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;

    }
}
