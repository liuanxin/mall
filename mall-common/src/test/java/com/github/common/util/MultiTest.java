package com.github.common.util;

import com.github.common.collection.MapMultiUtil;
import com.github.common.collection.MapMultiValue;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class MultiTest {

    @Test
    public void test() {
        MapMultiValue<Integer, String, List<String>> mapList1 = MapMultiUtil.createMapList();
        mapList1.put(123, "aaa");
        mapList1.put(123, "bbb");
        mapList1.put(123, "aaa");
        mapList1.put(1234, "bbb");
        mapList1.put(1234, "aaa");
        mapList1.put(1234, "aaa");
        // {1234=[bbb, aaa, aaa], 123=[aaa, bbb, aaa]}
        System.out.println(mapList1);

        MapMultiValue<Integer, String, List<String>> mapList2 = MapMultiUtil.createLinkedMapList();
        mapList2.put(123, "aaa");
        mapList2.put(123, "bbb");
        mapList2.put(123, "aaa");
        mapList2.put(1234, "bbb");
        mapList2.put(1234, "aaa");
        mapList2.put(1234, "aaa");
        // {123=[aaa, bbb, aaa], 1234=[bbb, aaa, aaa]}
        System.out.println(mapList2);

        MapMultiValue<Integer, String, Set<String>> mapSet1 = MapMultiUtil.createMapSet();
        mapSet1.put(123, "aaa");
        mapSet1.put(123, "bbb");
        mapSet1.put(123, "aaa");
        mapSet1.put(1234, "bbb");
        mapSet1.put(1234, "aaa");
        mapSet1.put(1234, "aaa");
        // {1234=[aaa, bbb], 123=[aaa, bbb]}
        System.out.println(mapSet1);

        MapMultiValue<Integer, String, Set<String>> mapSet2 = MapMultiUtil.createLinkedMapSet();
        mapSet2.put(123, "aaa");
        mapSet2.put(123, "bbb");
        mapSet2.put(123, "aaa");
        mapSet2.put(1234, "bbb");
        mapSet2.put(1234, "aaa");
        mapSet2.put(1234, "aaa");
        // {123=[aaa, bbb], 1234=[aaa, bbb]}
        System.out.println(mapSet2);

        MapMultiValue<Integer, String, Set<String>> mapSet3 = MapMultiUtil.createMapLinkedSet();
        mapSet3.put(123, "aaa");
        mapSet3.put(123, "bbb");
        mapSet3.put(123, "aaa");
        mapSet3.put(1234, "bbb");
        mapSet3.put(1234, "aaa");
        mapSet3.put(1234, "aaa");
        // {1234=[bbb, aaa], 123=[aaa, bbb]}
        System.out.println(mapSet3);

        MapMultiValue<Integer, String, Set<String>> mapSet4 = MapMultiUtil.createLinkedMapLinkedSet();
        mapSet4.put(123, "aaa");
        mapSet4.put(123, "bbb");
        mapSet4.put(123, "aaa");
        mapSet4.put(1234, "bbb");
        mapSet4.put(1234, "aaa");
        mapSet4.put(1234, "aaa");
        // {123=[aaa, bbb], 1234=[bbb, aaa]}
        System.out.println(mapSet4);
    }
}
