package com.github.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum FileMagicNumber {

    ICO("000001"),
    JPG("FFD8FF"),
    PNG("89504E"),
    GIF("474946"),
    BMP("424D36"),
    SVG("3C3F78"),

    /** doc xls ppt 都是一样的 */
    OFFICE03("D0CF11"),
    /** docx xlsx pptx 都是一新的 */
    OFFICE07("504B03"),

    PDF("255044"),

    NIL("");

    private final String magicNumber;
    FileMagicNumber(String magicNumber) {
        this.magicNumber = magicNumber;
    }
    public String getMagicNumber() {
        return magicNumber;
    }

    private static boolean check(String magicNumber, List<FileMagicNumber> magicNumberList) {
        if (U.isNotBlank(magicNumber)) {
            String upperCase = magicNumber.toUpperCase();
            for (FileMagicNumber value : magicNumberList) {
                if (upperCase.startsWith(value.magicNumber)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean hasImage(String magicNumber) {
        return check(magicNumber, Arrays.asList(ICO, JPG, PNG, GIF, BMP, SVG));
    }
    public static boolean hasPdf(String magicNumber) {
        return check(magicNumber, Collections.singletonList(PDF));
    }
    public static boolean hasOffice(String magicNumber) {
        return check(magicNumber, Arrays.asList(OFFICE03, OFFICE07));
    }
    public static boolean hasOffice03(String magicNumber) {
        return check(magicNumber, Collections.singletonList(OFFICE03));
    }
    public static boolean hasOffice07(String magicNumber) {
        return check(magicNumber, Collections.singletonList(OFFICE07));
    }
}
