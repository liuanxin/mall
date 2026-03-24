package com.github.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class UtilTest {

    @Test
    public void calcExecutorCount() {
        List<List<List<Object>>> list = List.of(
                List.of(
                        List.of(0.7, 10, 190),
                        List.of(0.7, 20, 180),
                        List.of(0.7, 100, 1),
                        List.of(0.7, 1000, 1)
                ),
                List.of(
                        List.of(0.75, 10, 190),
                        List.of(0.75, 20, 180),
                        List.of(0.75, 100, 1),
                        List.of(0.75, 1000, 1)
                ),
                List.of(
                        List.of(0.8, 10, 190),
                        List.of(0.8, 20, 180),
                        List.of(0.8, 100, 1),
                        List.of(0.8, 1000, 1)
                ),
                List.of(
                        List.of(0.85, 10, 190),
                        List.of(0.85, 20, 180),
                        List.of(0.85, 100, 1),
                        List.of(0.85, 1000, 1)
                )
        );
        for (List<List<Object>> arr : list) {
            for (List<Object> lt : arr) {
                double rate = Obj.toDouble(lt.get(0));
                int cpu = Obj.toInt(lt.get(1));
                int io = Obj.toInt(lt.get(2));
                int num = Obj.calcPoolSize(rate, cpu, io);
                System.out.printf("有 %s 个 CPU 核心, CPU 利用率 %4s, CPU 时间: %4s, IO 时间: %4s ==> %s\n",
                        Obj.CPU_SIZE, rate, cpu, io, num);
            }
            System.out.println();
        }
    }

    @Test
    public void encode() {
        Assert.assertEquals("", Obj.urlEncode(null));
        Assert.assertEquals("", Obj.urlEncode(""));

        String source = "name=中文&id=123&n=1+2   &n=2";
        String encode = Obj.urlEncode(source);
        Assert.assertNotEquals(source, encode);
        Assert.assertEquals(source, Obj.urlDecode(encode));
    }

    @Test
    public void chinese() {
        Assert.assertFalse(Obj.containsChinese(null));
        Assert.assertFalse(Obj.containsChinese(""));
        Assert.assertFalse(Obj.containsChinese("wqiroewfds123$%^&*("));

        Assert.assertTrue(Obj.containsChinese("wqiroewfds中123$%^&*("));
    }

    @Test
    public void phone() {
        Assert.assertFalse(Obj.hasPhone(null));
        Assert.assertFalse(Obj.hasPhone(""));
        Assert.assertFalse(Obj.hasPhone("131-1234-5678"));
        Assert.assertFalse(Obj.hasPhone("131 1234 5678"));
        Assert.assertFalse(Obj.hasPhone("131-1234 5678"));
        Assert.assertFalse(Obj.hasPhone("1311234678"));

        Assert.assertTrue(Obj.hasPhone("13112345678"));
        Assert.assertTrue(Obj.hasPhone("12112345678"));
    }

    @Test
    public void image() {
        Assert.assertFalse(Obj.hasImage(null));
        Assert.assertFalse(Obj.hasImage(""));
        Assert.assertFalse(Obj.hasImage("/tmp/image/fdwqrewqiofds.giff"));
        Assert.assertFalse(Obj.hasImage("afdwruewqrewq.abc"));

        Assert.assertTrue(Obj.hasImage("/tmp/ufio1u8231/abc.png"));
        Assert.assertTrue(Obj.hasImage("http://abc.xyz.com/uire4ui231.jpg"));
        Assert.assertTrue(Obj.hasImage("D:\\image\\中.bmp"));
        Assert.assertTrue(Obj.hasImage(".bmp"));
    }

    @Test
    public void email() {
        Assert.assertFalse(Obj.hasEmail(null));
        Assert.assertFalse(Obj.hasEmail(""));
        Assert.assertFalse(Obj.hasEmail("1$%^&*23-iurew@xyz.13s-rew.com"));
        Assert.assertFalse(Obj.hasEmail("-123@xyz.com"));

        Assert.assertTrue(Obj.hasEmail("abc-xyz@126.com"));
        Assert.assertTrue(Obj.hasEmail("abc@126.com"));
        Assert.assertTrue(Obj.hasEmail("10010@qq.com"));
        Assert.assertTrue(Obj.hasEmail("_abc-def@123-hij.uvw_xyz.com"));
        Assert.assertTrue(Obj.hasEmail("123-iurew@xyz.13s-rew.com"));
    }

    @Test
    public void ipNum() {
        Map<String, Long> ipNumMap = Arr.maps(
                "127.0.0.1", 2130706433L,
                "192.168.1.5", 3232235781L,
                "106.12.99.55", 1779196727L
        );
        for (Map.Entry<String, Long> entry : ipNumMap.entrySet()) {
            Assert.assertEquals(Obj.ip2num(entry.getKey()), entry.getValue().longValue());
            Assert.assertEquals(Obj.num2ip(entry.getValue()), entry.getKey());
        }
        System.out.println(Obj.ip2num("::1"));
        System.out.println(Obj.ip2num("ff02::2"));
        System.out.println();
        System.out.println(Obj.num2ip(1L));
        System.out.println(Obj.num2ip(2L));
    }

    @Test
    public void sign() {
        Map<String, String[]> paramMap = Arr.linkedMaps(
                "id", new String[] { "123" },
                "type", new String[] { "2" },
                "phone", new String[] { "13012345678" },
                "name", new String[] { "xx" },
                "desc", new String[] { "中文" }
        );
        System.out.println("准备发送的数据: " + Obj.formatPrintParam(paramMap) + "\n");

        paramMap = SignUtil.handleSign(paramMap);
        System.out.println("使用默认 key 真正发送的数据: " + Obj.formatPrintParam(paramMap));
        SignUtil.checkSign(paramMap);
        System.out.println("使用默认 key 检查通过");

        System.out.println();

        String key = "123";
        paramMap = SignUtil.handleSign(paramMap, key);
        System.out.println("使用指定 key 真正发送的数据: " + Obj.formatPrintParam(paramMap));
        SignUtil.checkSign(paramMap, key);
        System.out.println("使用指定 key 检查通过");
    }
}
