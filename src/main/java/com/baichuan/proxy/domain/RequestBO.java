package com.baichuan.proxy.domain;

import lombok.Data;

/**
 * @author kun
 * @date 2020-06-10 10:04
 */
@Data
public class RequestBO {
    private Boolean supportSsl;

    private String host;

    private Integer port;
}
