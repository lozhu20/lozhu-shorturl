package day.happy365.shorturlservice.constant;

import lombok.Getter;

@Getter
public enum ResultEnum {
    OK(0),
    FAIL(-1);

    private final int code;

    ResultEnum(int code) {
        this.code = code;
    }
}
