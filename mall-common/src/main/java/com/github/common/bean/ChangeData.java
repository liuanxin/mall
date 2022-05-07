package com.github.common.bean;

record ChangeData(int order, String value) implements Comparable<ChangeData> {

    @Override
    public int compareTo(ChangeData cd) {
        return order - cd.order;
    }
}
