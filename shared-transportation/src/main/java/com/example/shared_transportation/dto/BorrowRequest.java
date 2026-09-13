package com.example.shared_transportation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BorrowRequest {

    @NotBlank(message = "车辆编号不能为空")
    private String vehicleId;

    /** 借车时所在站点 */
    private String stationId;
}
