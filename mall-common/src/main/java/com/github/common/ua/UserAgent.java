package com.github.common.ua;

import com.github.common.util.U;

import java.util.ArrayList;
import java.util.List;

public class UserAgent {

    private UserAgentBrowser browser;
    private UserAgentCpu cpu;
    private UserAgentDevice device;
    private UserAgentModel engine;
    private UserAgentModel os;

    public UserAgentBrowser getBrowser() {
        return browser;
    }
    public void setBrowser(UserAgentBrowser browser) {
        this.browser = browser;
    }

    public UserAgentCpu getCpu() {
        return cpu;
    }
    public void setCpu(UserAgentCpu cpu) {
        this.cpu = cpu;
    }

    public UserAgentDevice getDevice() {
        return device;
    }
    public void setDevice(UserAgentDevice device) {
        this.device = device;
    }

    public UserAgentModel getEngine() {
        return engine;
    }
    public void setEngine(UserAgentModel engine) {
        this.engine = engine;
    }

    public UserAgentModel getOs() {
        return os;
    }
    public void setOs(UserAgentModel os) {
        this.os = os;
    }

    @Override
    public String toString() {
        List<String> list = new ArrayList<>();
        if (U.isNotNull(browser)) {
            String br = browser.toString();
            if (U.isNotBlank(br)) {
                list.add("browser=" + br);
            }
        }
        if (U.isNotNull(cpu)) {
            String cp = cpu.toString();
            if (U.isNotBlank(cp)) {
                list.add("cpu=" + cp);
            }
        }
        if (U.isNotNull(device)) {
            String de = device.toString();
            if (U.isNotBlank(de)) {
                list.add("device=" + de);
            }
        }
        if (U.isNotNull(engine)) {
            String en = engine.toString();
            if (U.isNotBlank(en)) {
                list.add("engine=" + en);
            }
        }
        if (U.isNotNull(os)) {
            String o = os.toString();
            if (U.isNotBlank(o)) {
                list.add("os=" + o);
            }
        }
        return "{" + String.join(",", list) + "}";
    }

    public static class UserAgentModel {
        private String name;
        private String version;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }
        public void setVersion(String version) {
            this.version = version;
        }

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
    public static class UserAgentBrowser extends UserAgentModel {
        private String major;

        public String getMajor() {
            return major;
        }
        public void setMajor(String major) {
            this.major = major;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
    public static class UserAgentCpu {
        private String architecture;

        public String getArchitecture() {
            return architecture;
        }
        public void setArchitecture(String architecture) {
            this.architecture = architecture;
        }

        @Override
        public String toString() {
            return U.toStr(architecture);
        }
    }
    public static class UserAgentDevice {
        private String type;
        private String model;
        private String vendor;

        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }

        public String getModel() {
            return model;
        }
        public void setModel(String model) {
            this.model = model;
        }

        public String getVendor() {
            return vendor;
        }
        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        @Override
        public String toString() {
            List<String> list = new ArrayList<>();
            if (U.isNotBlank(type)) {
                list.add(type.toLowerCase());
            }
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
