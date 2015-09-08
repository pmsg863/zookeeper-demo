package com.xmgps.yun.config;

import com.xmgps.yun.communication.core.handler.IoMessageHandler;
import com.xmgps.yun.communication.core.session.SessionWrapper;
import com.xmgps.yun.communication.message.IoMessage;
import com.xmgps.yun.communication.ypt.client.GatewayClient;
import com.xmgps.yun.communication.ypt.message.YPTPackageMsg;
import com.xmgps.yun.configuration.core.ConfigService;
import com.xmgps.yun.configuration.source.XmlConfigFile;
import org.apache.log4j.PropertyConfigurator;

import java.util.concurrent.TimeUnit;

/**
 * Created by huangwb on 2015/5/14.
 */
public class ClientDemo implements IoMessageHandler {

    static {
        PropertyConfigurator.configureAndWatch("log4j.properties", 1000 * 60); //动态加载log4j配置
    }
    /**
     * 网关连接客户端
     */
    private GatewayClient client;

    /**
     * 构造网关连接客户端
     * @param name 网关连接客户端实例名，其配置、日志均使用该实例名
     */
    public ClientDemo(String name) {
        client = new GatewayClient(name);
        client.setMsgHandler(this);
    }

    /**
     * 启动客户端
     */
    public boolean start() {
        return client.connect();
    }

    public boolean sendMessage(IoMessage message) {
        return client.sendMessage(message);
    }

    public void messageReceived(SessionWrapper session, IoMessage message) throws Exception {
        if (message instanceof YPTPackageMsg) {
            System.out.println("RECEIVED");
        }
    }

    public void sessionClosed(SessionWrapper session) throws Exception {
        // Do nothing
    }

    public void sessionOpened(SessionWrapper session) throws Exception {
        // Do nothing
    }

    public void sessionAuthorized(SessionWrapper session) throws Exception {
        // Do nothing
    }


    public static void main(String[] args) throws InterruptedException {
        XmlConfigFile configSource = new XmlConfigFile();
        configSource.setFileName("System.config");
        ConfigService configService = ConfigService.getInstance();
        configService.setSource(configSource);
        configService.initialize();

        ClientDemo client = new ClientDemo("GatewayClient");
        client.start();

        while (true) {
            TimeUnit.SECONDS.sleep(5);
        }
    }
}
