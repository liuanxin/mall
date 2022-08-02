package com.github.common.collection;

/** 键值对的 value 是集合时, 数据重复时的做法 */
enum MapValueDuplicateType {

    /** 抛异常 */
    THROW,

    /** 覆盖 */
    COVER,

    /** 忽略 */
    IGNORE
}
