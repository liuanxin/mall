package com.github.common.export.poi;

import com.github.common.export.FileExport;
import com.github.common.export.WebExport;
import com.github.common.util.U;

/** 如果想要将数据导成文件保持, 使用 {@link FileExport} 类, 如果要导出文件在 web 端下载, 使用 {@link WebExport} 类 */
public enum ExportType {

    Xls03, Xls07, Csv;

    public boolean is03() {
        return this == Xls03;
    }
    public boolean is07() {
        return this == Xls07;
    }
    public boolean isExcel() {
        return is03() || is07();
    }

    public boolean isCsv() {
        return this == Csv;
    }

    public static ExportType to(String type) {
        if (U.isNotBlank(type)) {
            for (ExportType exportType : values()) {
                if (type.equalsIgnoreCase(exportType.name())) {
                    return exportType;
                }
            }
        }
        return Xls07;
    }
}
