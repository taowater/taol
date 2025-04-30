package com.test.bean;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BeanMapper {

    BeanMapper INSTANCE = Mappers.getMapper(BeanMapper.class);

    @Mapping(target = "list2", ignore = true)
    Bean2 to(Bean1 bean);
}