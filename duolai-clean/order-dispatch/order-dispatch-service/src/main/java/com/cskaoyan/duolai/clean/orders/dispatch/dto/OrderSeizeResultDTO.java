package com.cskaoyan.duolai.clean.orders.dispatch.dto;

import lombok.Data;

@Data
public class OrderSeizeResultDTO {

    Long serveProviderId;

    Long ordersId;

    Boolean isMachineSeize;
}
