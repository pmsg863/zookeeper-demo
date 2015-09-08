package com.xmgps.yun.config;

import com.xmgps.yun.communication.statistics.ServerStatistics;
import com.xmgps.yun.config.zookeeper.ServiceDiscovery;
import com.xmgps.yun.config.zookeeper.ServiceJmxRegister;
import com.xmgps.yun.config.zookeeper.ServiceRegister;
import com.xmgps.yun.configuration.config.BindingAddressConfig;
import com.xmgps.yun.configuration.config.ClientConnectionConfig;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() throws Exception {

        ServiceRegister service = new ServiceRegister();
        service.init() ;

        BindingAddressConfig tempConfit = null;
        try {
            tempConfit = new BindingAddressConfig("GW");
        } catch (Exception ex) {
        }
        tempConfit.setLocalHostID(10010001);
        tempConfit.setVersion(0);
        tempConfit.setPort(7767);
        tempConfit.setHost("0.0.0.0");

        service.registerConfig(tempConfit);
        tempConfit.setLocalHostID(10010002);
        tempConfit.setPort(7766);
        service.registerConfig(tempConfit);
        tempConfit.setLocalHostID(10010003);
        tempConfit.setPort(7768);
        service.registerConfig(tempConfit);
        service.updateConfig(tempConfit);

        ServiceJmxRegister jmx = new ServiceJmxRegister();
        jmx.init() ;

        ServerStatistics statistics = ServerStatistics.valueOf("<ServerStatistics LID=\"10010001\" IP=\"127.0.0.1\" PT=\"0\" STA=\"0\" RP=\"0\" RB=\"0\" SP=\"0\" SB=\"0\" LSC=\"0\" CSC=\"0\" MSC=\"0\" RPT=\"0.0\" RBT=\"100.0\" SPT=\"0.0\" SBT=\"100.0\"/>");
        jmx.registerConfig(statistics);
        statistics.setLocalHostID(10010002);
        statistics.setRecvBytesThroughput(200);
        statistics.setSendBytesThroughput(200);
        jmx.registerConfig(statistics);
        statistics.setLocalHostID(10010003);
        statistics.setRecvBytesThroughput(300);
        statistics.setSendBytesThroughput(300);
        jmx.registerConfig(statistics);

        while (true) {
            ServiceDiscovery discovery = new ServiceDiscovery();
            discovery.init() ;
            ClientConnectionConfig clientConnectionConfig = discovery.discoveryConfig();
            TimeUnit.SECONDS.sleep(5);
        }
    }
}
