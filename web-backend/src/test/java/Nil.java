import com.github.common.util.A;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nil {

    private static final Object OBJ = new Object();

    public static void main(String[] args) {
        System.out.println(OBJ);

        List<Object> list = new ArrayList<>();
        list.add(OBJ);
        System.out.println(list.get(0).equals(OBJ));

        Map<String, Object> map = new HashMap<>();
        String key = "abc";
        map.put(key, OBJ);
        System.out.println(map.get(key).equals(OBJ));

        System.out.println(A.toString("abc".getBytes()));
    }
}
