package com.test.bean.bench;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CollectionBenchMapper {

    CollectionBenchMapper INSTANCE = Mappers.getMapper(CollectionBenchMapper.class);

    @Mapping(target = "setShortToListLong", ignore = true)
    @Mapping(target = "longsToDoubles", ignore = true)
    CollectionBenchTarget to(CollectionBenchSource source);

    @Mapping(target = "setShortToListLong", ignore = true)
    @Mapping(target = "longsToDoubles", ignore = true)
    void update(CollectionBenchSource source, @MappingTarget CollectionBenchTarget target);
}
