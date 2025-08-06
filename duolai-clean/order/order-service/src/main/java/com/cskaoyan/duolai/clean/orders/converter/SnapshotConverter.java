package com.cskaoyan.duolai.clean.orders.converter;

import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SnapshotConverter {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateOrderSnapshot(OrderSnapshotDTO oldOrderSnapshot, @MappingTarget OrderSnapshotDTO newOrderSnapshot);

}
