package com.xmgps.yun.config.comm;

import com.xmgps.yun.communication.ypt.client.GatewayClient;
import com.xmgps.yun.config.zookeeper.ServiceDiscovery;
import com.xmgps.yun.config.zookeeper.XStreamSerializer;
import com.xmgps.yun.configuration.config.ClientConnectionConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangwb on 2015/5/12.
 */
public class DiscoveryWrapper implements Runnable {
    static GatewayClient socket;

    final static ServiceDiscovery discovery = new ServiceDiscovery();

    /**
     * 定时访问数据库线程池
     */
    static ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "TasksExecuter");
            return t;
        }
    });

    public static boolean isZookeeper(){
        try {
            discovery.init();
            threadPool.scheduleAtFixedRate(new DiscoveryWrapper(), 30,
                    30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static ClientConnectionConfig getConfig(GatewayClient client) throws Exception {
        socket = client;

        ClientConnectionConfig clientConnectionConfig = discovery.discoveryConfig();
        return clientConnectionConfig;
    }

    /**
     * 定时器执行接口
     */
    @Override
    public void run() {
        ClientConnectionConfig clientConnectionConfig = null;
        try {
            clientConnectionConfig = discovery.discoveryConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean result = socket.setGatewayConnections(clientConnectionConfig.getVersion(), XStreamSerializer.getXml(clientConnectionConfig.getConnections()));
        if(result)
            System.out.println( XStreamSerializer.getXml(clientConnectionConfig.getConnections()) );
    };

    public static void main(String[] args) throws Exception {

        DiscoveryWrapper.isZookeeper();
        ClientConnectionConfig config = DiscoveryWrapper.getConfig(null);
        System.out.println(XStreamSerializer.getXml(config));


        while (true) {
            TimeUnit.SECONDS.sleep(5);
        }
    }

}
