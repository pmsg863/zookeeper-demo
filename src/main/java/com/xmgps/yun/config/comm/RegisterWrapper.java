package com.xmgps.yun.config.comm;

import com.xmgps.yun.communication.statistics.ServerStatistics;
import com.xmgps.yun.communication.ypt.server.PlatformServer;
import com.xmgps.yun.config.zookeeper.ServiceJmxRegister;
import com.xmgps.yun.config.zookeeper.ServiceRegister;
import com.xmgps.yun.configuration.config.BindingAddressConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangwb on 2015/5/12.
 */
public class RegisterWrapper implements Runnable{
    static PlatformServer socket;

    static ServiceRegister service = new ServiceRegister();
    static ServiceJmxRegister jmx = new ServiceJmxRegister();

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
            service.init();
            jmx.init();
            threadPool.scheduleAtFixedRate(new RegisterWrapper(), 30,
                    30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public static void setConfig(PlatformServer server,BindingAddressConfig config) throws Exception {
        socket = server;

        service.registerConfig(config);
        if(socket.getServerStatistics()!=null && ServerStatistics.valueOf(socket.getServerStatistics())!=null)
            jmx.registerConfig(ServerStatistics.valueOf(socket.getServerStatistics()));
        else{
            ServerStatistics statistics = new ServerStatistics();
            statistics.setLocalHostID(socket.getLocalHostID());
            statistics.setIP(config.getHost());
            statistics.setPort(config.getPort());
            statistics.setStatus(0);
            jmx.registerConfig( statistics );
        }

    }

    /**
     * 定时器执行接口
     */
    public void run() {
        String serverStatistics = socket.getServerStatistics();
        try {
            jmx.updateConfig(ServerStatistics.valueOf(serverStatistics) );
        } catch (Exception e) {
            e.printStackTrace();
        }
    };
}
