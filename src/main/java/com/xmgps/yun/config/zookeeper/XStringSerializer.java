package com.xmgps.yun.config.zookeeper;

import com.xmgps.yun.communication.statistics.ClientStatistics;
import com.xmgps.yun.communication.statistics.ServerStatistics;
import com.xmgps.yun.communication.statistics.SessionStatistics;
import com.xmgps.yun.communication.statistics.SessionStatisticsList;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangwb on 2015/5/12.
 */
public class XStringSerializer implements InstanceSerializer {
    static Map<String, Class> classMap = new HashMap<>();

    static {
        classMap.put("SessionStatisticsList", SessionStatisticsList.class);
        classMap.put("SessionStatistics", SessionStatistics.class);
        classMap.put("ServerStatistics", ServerStatistics.class);
        classMap.put("ClientStatistics", ClientStatistics.class);
    }

    @Override
    public byte[] serialize(ServiceInstance instance) throws Exception {
        String simpleName = instance.getPayload().getClass().getSimpleName();
        Class aClass = classMap.get(simpleName);
        Method toXmlString = aClass.getMethod("toXmlString");
        Object invoke = toXmlString.invoke(instance.getPayload());
        return ((String) invoke).getBytes();
    }

    @Override
    public ServiceInstance deserialize(byte[] bytes) throws Exception {
        String xml = new String(bytes);
        String simpleName = xml.substring(1, 25).split(" ")[0];
        Class aClass = classMap.get(simpleName);
        Object result = aClass.newInstance();
        Method valueOf = aClass.getMethod("valueOf", String.class);
        Object invoke = valueOf.invoke(result, xml);

        return ServiceInstance.builder()
                .name(simpleName)
                .payload(invoke)
                .build();
    }

    public static class PriorityRank implements Comparable {
        long localHostID;
        double recvBytesThroughput;
        int priority;

        public PriorityRank(long localHostID, double recvBytesThroughput) {
            this.localHostID = localHostID;
            this.recvBytesThroughput = recvBytesThroughput;
        }

        public long getLocalHostID() {
            return localHostID;
        }

        public double getRecvBytesThroughput() {
            return recvBytesThroughput;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(Object o) {
            if (this == o) {
                return 0;
            } else if (o != null && o instanceof PriorityRank) {
                PriorityRank u = (PriorityRank) o;
                if (recvBytesThroughput <= u.recvBytesThroughput) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return -1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PriorityRank)) return false;

            PriorityRank that = (PriorityRank) o;

            if (localHostID != that.localHostID) return false;
            if (Double.compare(that.recvBytesThroughput, recvBytesThroughput) != 0) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = (int) (localHostID ^ (localHostID >>> 32));
            temp = Double.doubleToLongBits(recvBytesThroughput);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        public int getPriority() {
            return priority;
        }
    }

  /*  public static void main(String[] args) throws Exception {
        ServerStatistics testItem = new ServerStatistics();
        testItem.setIP("127.0.0.1");
        testItem.setStatus(0);
        XStringSerializer xstringSerial = new XStringSerializer();
        ServiceInstance deserialize = xstringSerial.deserialize(testItem.toXmlString().getBytes());
        byte[] serialize = xstringSerial.serialize(deserialize);

        System.out.println(new String(serialize));

    }*/
}
