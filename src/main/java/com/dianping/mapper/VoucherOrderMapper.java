package com.dianping.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.entity.VoucherOrder;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
}
