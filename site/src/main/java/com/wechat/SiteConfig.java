package com.wechat; /**
 * Copyright (C) 2006-2015 Tuniu All rights reserved
 */

import com.wechat.demo.*;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.mp.api.*;
import org.apache.log4j.helpers.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

@SpringBootApplication
public class SiteConfig implements CommandLineRunner {

    private static Logger logger = LoggerFactory.getLogger(SiteConfig.class);

    @Value("${spring.profiles.active}")
    private String profiles;

    @Override
    public void run(String... args) throws Exception {
        logger.info("The spring.profiles.active is: {}", profiles);

    }
    @Bean
    public WxMpConfigStorage wxMpConfigStorage(){
        InputStream is1 = null;
        URL url = Loader.getResource("test-config.xml");
        try {
            is1 = new FileInputStream(ResourceUtils.getFile(url));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return WxMpDemoInMemoryConfigStorage.fromXml(is1);
    }

    @Bean
    public WxMpService wxMpService(){
        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(wxMpConfigStorage());
        return wxMpService;
    }

    @Bean
    public WxMpMessageRouter wxMpMessageRouter(){
        WxMpMessageHandler logHandler = new DemoLogHandler();
        WxMpMessageHandler textHandler = new DemoTextHandler();
        WxMpMessageHandler imageHandler = new DemoImageHandler();
        WxMpMessageHandler oauth2handler = new DemoOAuth2Handler();
        DemoGuessNumberHandler guessNumberHandler = new DemoGuessNumberHandler();

        WxMpMessageRouter wxMpMessageRouter = new WxMpMessageRouter(wxMpService());
        wxMpMessageRouter
                .rule().handler(logHandler).next()
                .rule().msgType(WxConsts.XML_MSG_TEXT).matcher(guessNumberHandler).handler(guessNumberHandler).end()
                .rule().async(false).content("哈哈").handler(textHandler).end()
                .rule().async(false).content("图片").handler(imageHandler).end()
                .rule().async(false).content("oauth").handler(oauth2handler).end();
        return wxMpMessageRouter;
    }
}
