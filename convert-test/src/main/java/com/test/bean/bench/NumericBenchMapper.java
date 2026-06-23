package com.test.bean.bench;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface NumericBenchMapper {

    NumericBenchMapper INSTANCE = Mappers.getMapper(NumericBenchMapper.class);

    NumericBenchTarget to(NumericBenchSource source);

    void update(NumericBenchSource source, @MappingTarget NumericBenchTarget target);
}
