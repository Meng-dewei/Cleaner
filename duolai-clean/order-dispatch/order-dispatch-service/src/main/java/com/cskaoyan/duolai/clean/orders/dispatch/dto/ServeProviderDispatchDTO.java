package com.cskaoyan.duolai.clean.orders.dispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServeProviderDispatchDTO {
    /**
     * 服务人员或机构id
     */
    private Long id;
    /**
     * 评分
     */
    private Integer evaluationScore;
    /**
     * 当前接单数量
     */
    private Integer acceptanceNum;
    /**
     * 接单距离
     */
    private Integer acceptanceDistance;
}
