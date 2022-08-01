package com.github.common.util;

import com.github.common.collection.MapValueList;
import com.github.common.collection.MapValueSet;
import com.github.common.collection.MultiUtil;
import org.junit.Test;

public class MultiTest {

    @Test
    public void test() {
        MapValueList<Integer, String> mapList1 = MultiUtil.createMapList();
        mapList1.put(123, "aaa");
        mapList1.put(123, "bbb");
        mapList1.put(123, "aaa");
        mapList1.put(1234, "bbb");
        mapList1.put(1234, "aaa");
        mapList1.put(1234, "aaa");
        // {1234=[bbb, aaa, aaa], 123=[aaa, bbb, aaa]}
        System.out.println(mapList1);

        MapValueList<Integer, String> mapList2 = MultiUtil.createLinkedMapList();
        mapList2.put(123, "aaa");
        mapList2.put(123, "bbb");
        mapList2.put(123, "aaa");
        mapList2.put(1234, "bbb");
        mapList2.put(1234, "aaa");
        mapList2.put(1234, "aaa");
        // {123=[aaa, bbb, aaa], 1234=[bbb, aaa, aaa]}
        System.out.println(mapList2);

        MapValueSet<Integer, String> mapSet1 = MultiUtil.createMapSet();
        mapSet1.put(123, "aaa");
        mapSet1.put(123, "bbb");
        mapSet1.put(123, "aaa");
        mapSet1.put(1234, "bbb");
        mapSet1.put(1234, "aaa");
        mapSet1.put(1234, "aaa");
        // {1234=[aaa, bbb], 123=[aaa, bbb]}
        System.out.println(mapSet1);

        MapValueSet<Integer, String> mapSet2 = MultiUtil.createLinkedMapSet();
        mapSet2.put(123, "aaa");
        mapSet2.put(123, "bbb");
        mapSet2.put(123, "aaa");
        mapSet2.put(1234, "bbb");
        mapSet2.put(1234, "aaa");
        mapSet2.put(1234, "aaa");
        // {123=[aaa, bbb], 1234=[aaa, bbb]}
        System.out.println(mapSet2);

        MapValueSet<Integer, String> mapSet3 = MultiUtil.createMapLinkedSet();
        mapSet3.put(123, "aaa");
        mapSet3.put(123, "bbb");
        mapSet3.put(123, "aaa");
        mapSet3.put(1234, "bbb");
        mapSet3.put(1234, "aaa");
        mapSet3.put(1234, "aaa");
        // {1234=[bbb, aaa], 123=[aaa, bbb]}
        System.out.println(mapSet3);

        MapValueSet<Integer, String> mapSet4 = MultiUtil.createLinkedMapLinkedSet();
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
