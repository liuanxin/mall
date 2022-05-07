package com.github.common.bean;

class ChangeData implements Comparable<ChangeData> {

    private final int order;
    private final String value;

    ChangeData(int order, String value) {
        this.order = order;
        this.value = value;
    }

    public int getOrder() {
        return order;
    }
    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(ChangeData cd) {
        return order - cd.order;
    }
}
