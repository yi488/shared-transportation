package com.example.shared_transportation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnRequest {

    @NotNull(message = "借车记录编号不能为空")
    private Long borrowId;

    /** 还车时所在站点 */
    private String stationId;
}
