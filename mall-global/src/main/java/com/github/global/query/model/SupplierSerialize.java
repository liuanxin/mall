package com.github.global.query.model;

import java.io.Serializable;
import java.util.function.Supplier;

public interface SupplierSerialize<T> extends Supplier<T>, Serializable {
}
