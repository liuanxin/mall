package com.github.common.ua;

import com.github.common.util.U;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAgent {

    private UserAgentBrowser browser;
    private UserAgentCpu cpu;
    private UserAgentDevice device;
    private UserAgentModel engine;
    private UserAgentModel os;

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        String br = browser.toString();
        if (U.isNotBlank(br)) {
            list.add("browser=" + br);
        }
        String cp = cpu.toString();
        if (U.isNotBlank(cp)) {
            list.add("cpu=" + cp);
        }
        String de = device.toString();
        if (U.isNotBlank(de)) {
            list.add("device=" + de);
        }
        String en = engine.toString();
        if (U.isNotBlank(en)) {
            list.add("engine=" + en);
        }
        String o = os.toString();
        if (U.isNotBlank(o)) {
            list.add("os=" + o);
        }
        return "UserAgentInfo(" + String.join(", ", list) + ")";
    }

    @Data
    public static class UserAgentModel {
        private String name;
        private String version;

        @Override
        public String toString() {
            List<String> list = new ArrayList<>();
            if (U.isNotBlank(name)) {
                list.add(name.toLowerCase());
            }
            if (U.isNotBlank(version)) {
                list.add(version);
            }
            return String.join(":", list);
        }
    }
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserAgentBrowser extends UserAgentModel {
        private String major;

        @Override
        public String toString() {
            return super.toString();
        }
    }
    @Data
    public static class UserAgentCpu {
        private String architecture;

        @Override
        public String toString() {
            return U.toStr(architecture);
        }
    }
    @Data
    public static class UserAgentDevice {
        private String type;
        private String model;
        private String vendor;

        @Override
        public String toString() {
            List<String> list = new ArrayList<>();
            if (U.isNotBlank(vendor)) {
                list.add(vendor.toLowerCase());
            }
            if (U.isNotBlank(model)) {
                list.add(model.toLowerCase());
            }
            return String.join(":", list);
        }
    }
}
