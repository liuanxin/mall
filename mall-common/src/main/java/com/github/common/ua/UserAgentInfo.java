package com.github.common.ua;

import com.github.common.util.U;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAgentInfo {

    private UserAgentBrowser browser;
    private UserAgentCpu cpu;
    private UserAgentDevice device;
    private UserAgentModel engine;
    private UserAgentModel os;

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
