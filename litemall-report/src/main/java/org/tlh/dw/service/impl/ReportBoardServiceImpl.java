package org.tlh.dw.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.tlh.dw.entity.AdsProductSaleTopn;
import org.tlh.dw.entity.AdsRegionDayCount;
import org.tlh.dw.entity.AdsUserActionConvertDay;
import org.tlh.dw.mapper.AdsRegionDayCountMapper;
import org.tlh.dw.service.IAdsProductSaleTopnService;
import org.tlh.dw.service.IAdsUserActionConvertDayService;
import org.tlh.dw.service.ReportBoardService;
import org.tlh.dw.vo.EchartBarVo;
import org.tlh.dw.vo.RegionOrderVo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 离歌笑
 * @desc
 * @date 2021-01-04
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ReportBoardServiceImpl implements ReportBoardService {

    @Autowired
    private IAdsUserActionConvertDayService adsUserActionConvertDayService;

    @Autowired
    private IAdsProductSaleTopnService productSaleTopnService;

    @Autowired
    private AdsRegionDayCountMapper regionDayCountMapper;

    @Override
    public List<Map<String, Object>> uaConvert(String date) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (StringUtils.isEmpty(date)) {
            date = DateFormatUtils.format(DateUtils.addDays(new Date(),-1), "yyyy-MM-dd");
        }
        QueryWrapper wrapper = new QueryWrapper<AdsUserActionConvertDay>().eq("dt", date);
        AdsUserActionConvertDay convertDay = this.adsUserActionConvertDayService.getOne(wrapper);
        if (convertDay != null) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "访问");
            item.put("value", 100);
            result.add(item);

            item = new HashMap<>();
            item.put("name", "加购");
            item.put("value", convertDay.getCart2orderConvertRatio().doubleValue() * 100);
            result.add(item);

            item = new HashMap<>();
            item.put("name", "订单");
            item.put("value", convertDay.getCart2orderConvertRatio().doubleValue() * 100);
            result.add(item);

            item = new HashMap<>();
            item.put("name", "支付");
            item.put("value", convertDay.getOrder2paymentConvertRatio().doubleValue() * 100);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<EchartBarVo> saleTopN(String date) {
        if (StringUtils.isEmpty(date)) {
            date = DateFormatUtils.format(DateUtils.addDays(new Date(),-1), "yyyy-MM-dd");
        }
        Wrapper<AdsProductSaleTopn> wrapper=new QueryWrapper<AdsProductSaleTopn>().eq("dt",date);
        List<AdsProductSaleTopn> data = this.productSaleTopnService.list(wrapper);
        if (!ObjectUtils.isEmpty(data)){
            List<EchartBarVo> result = data.stream()
                    .map(item -> new EchartBarVo(item.getSkuId() + "", item.getPaymentCount()))
                    .collect(Collectors.toList());
            return result;
        }
        return null;
    }

    private static final List<String> SPECIAL_REGIONS=Arrays.asList(
            "内蒙古","广西","西藏","宁夏","新疆",
            "北京","天津","天津","重庆");

    @Override
    public List<RegionOrderVo> regionOrder(String date, int type,String name) {
        if (StringUtils.isEmpty(date)) {
            date = DateFormatUtils.format(DateUtils.addDays(new Date(),-1), "yyyy-MM-dd");
        }
        List<AdsRegionDayCount> temp=null;
        switch (type){
            case 0:
                temp=this.regionDayCountMapper.provinceSummary(date);
                break;
            case 1:
                if (SPECIAL_REGIONS.contains(name)){
                    temp=this.regionDayCountMapper.countrySummary(date,name);
                }else {
                    temp = this.regionDayCountMapper.citySummary(date, name);
                }
                break;
            case 2:
                temp=this.regionDayCountMapper.countrySummary(date,name);
                break;
        }
        if (temp==null){
            return null;
        }
        List<RegionOrderVo> result = temp.stream()
                .map(item -> new RegionOrderVo(
                        item.getProvinceName(),
                        item.getOrderDayCount(),
                        item.getOrderDayAmount().doubleValue())
                ).collect(Collectors.toList());
        return result;
    }
}