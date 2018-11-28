package com.leyou.search.client;

import com.leyou.item.api.BrandApi;
import com.leyou.item.pojo.Brand;
import org.springframework.cloud.openfeign.FeignClient;

import java.util.ArrayList;
import java.util.List;

@FeignClient("item-service")
public interface BrandClient extends BrandApi {
}
