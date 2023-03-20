package com.github.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UtilTest {

    @Test
    public void calcExecutorCount() {
        List<List<Object>> list = Arrays.asList(
                Arrays.asList(0.7, 10, 190),
                Arrays.asList(0.7, 20, 180),
                Arrays.asList(0.7, 100, 1),
                Arrays.asList(0.7, 1000, 1),

                Arrays.asList(0.75, 10, 190),
                Arrays.asList(0.75, 20, 180),
                Arrays.asList(0.75, 100, 1),
                Arrays.asList(0.75, 1000, 1),

                Arrays.asList(0.8, 10, 190),
                Arrays.asList(0.8, 20, 180),
                Arrays.asList(0.8, 100, 1),
                Arrays.asList(0.8, 1000, 1),

                Arrays.asList(0.85, 10, 190),
                Arrays.asList(0.85, 20, 180),
                Arrays.asList(0.85, 100, 1),
                Arrays.asList(0.85, 1000, 1)
        );
        for (List<Object> lt : list) {
            double rate = U.toDouble(lt.get(0));
            int cpu = U.toInt(lt.get(1));
            int io = U.toInt(lt.get(2));
            int num = U.calcPoolSize(rate, cpu, io);
            System.out.printf("CPU 利用率 %4s, CPU 时间: %4s, IO 时间: %4s ==> %s\n", rate, cpu, io, num);
        }
    }

    @Test
    public void encode() {
        Assert.assertEquals("", U.urlEncode(null));
        Assert.assertEquals("", U.urlEncode(""));

        String source = "name=中文&id=123&n=1+2   &n=2";
        String encode = U.urlEncode(source);
        Assert.assertNotEquals(source, encode);
        Assert.assertEquals(source, U.urlDecode(encode));
    }

    @Test
    public void chinese() {
        Assert.assertFalse(U.containsChinese(null));
        Assert.assertFalse(U.containsChinese(""));
        Assert.assertFalse(U.containsChinese("wqiroewfds123$%^&*("));

        Assert.assertTrue(U.containsChinese("wqiroewfds中123$%^&*("));
    }

    @Test
    public void phone() {
        Assert.assertFalse(U.hasPhone(null));
        Assert.assertFalse(U.hasPhone(""));
        Assert.assertFalse(U.hasPhone("131-1234-5678"));
        Assert.assertFalse(U.hasPhone("131 1234 5678"));
        Assert.assertFalse(U.hasPhone("131-1234 5678"));
        Assert.assertFalse(U.hasPhone("1311234678"));

        Assert.assertTrue(U.hasPhone("13112345678"));
        Assert.assertTrue(U.hasPhone("12112345678"));
    }

    @Test
    public void image() {
        Assert.assertFalse(U.hasImage(null));
        Assert.assertFalse(U.hasImage(""));
        Assert.assertFalse(U.hasImage("/tmp/image/fdwqrewqiofds.giff"));
        Assert.assertFalse(U.hasImage("afdwruewqrewq.abc"));

        Assert.assertTrue(U.hasImage("/tmp/ufio1u8231/abc.png"));
        Assert.assertTrue(U.hasImage("http://abc.xyz.com/uire4ui231.jpg"));
        Assert.assertTrue(U.hasImage("D:\\image\\中.bmp"));
        Assert.assertTrue(U.hasImage(".bmp"));
    }

    @Test
    public void email() {
        Assert.assertFalse(U.hasEmail(null));
        Assert.assertFalse(U.hasEmail(""));
        Assert.assertFalse(U.hasEmail("1$%^&*23-iurew@xyz.13s-rew.com"));
        Assert.assertFalse(U.hasEmail("-123@xyz.com"));

        Assert.assertTrue(U.hasEmail("abc-xyz@126.com"));
        Assert.assertTrue(U.hasEmail("abc@126.com"));
        Assert.assertTrue(U.hasEmail("10010@qq.com"));
        Assert.assertTrue(U.hasEmail("_abc-def@123-hij.uvw_xyz.com"));
        Assert.assertTrue(U.hasEmail("123-iurew@xyz.13s-rew.com"));
    }

    @Test
    public void ipNum() {
        Map<String, Long> ipNumMap = A.maps(
                "127.0.0.1", 2130706433L,
                "192.168.1.5", 3232235781L,
                "106.12.99.55", 1779196727L
        );
        for (Map.Entry<String, Long> entry : ipNumMap.entrySet()) {
            Assert.assertEquals(U.ip2num(entry.getKey()), entry.getValue().longValue());
            Assert.assertEquals(U.num2ip(entry.getValue()), entry.getKey());
        }
        System.out.println(U.ip2num("::1"));
        System.out.println(U.ip2num("ff02::2"));
        System.out.println();
        System.out.println(U.num2ip(1L));
        System.out.println(U.num2ip(2L));
    }

    @Test
    public void sign() {
        Map<String, String[]> paramMap = A.linkedMaps(
                "id", new String[] { "123" },
                "type", new String[] { "2" },
                "phone", new String[] { "13012345678" },
                "name", new String[] { "xx" },
                "desc", new String[] { "中文" }
        );
        System.out.println("准备发送的数据: " + U.formatParam(false, false, paramMap) + "\n");

        paramMap = SignUtil.handleSign(paramMap);
        System.out.println("使用默认 key 真正发送的数据: " + U.formatParam(false, false, paramMap));
        SignUtil.checkSign(paramMap);
        System.out.println("使用默认 key 检查通过");

        System.out.println();

        String key = "123";
        paramMap = SignUtil.handleSign(paramMap, key);
        System.out.println("使用指定 key 真正发送的数据: " + U.formatParam(false, false, paramMap));
        SignUtil.checkSign(paramMap, key);
        System.out.println("使用指定 key 检查通过");
    }
}
