package com.usthe.collector.collect.http.promethus;

import com.google.gson.JsonElement;
import com.usthe.collector.dispatch.DispatchConstants;
import com.usthe.collector.util.CollectUtil;
import com.usthe.common.entity.dto.PromVectorOrMatrix;
import com.usthe.common.entity.job.protocol.HttpProtocol;
import com.usthe.common.entity.message.CollectRep;
import com.usthe.common.util.CommonConstants;
import com.usthe.common.util.GsonUtil;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 *
 * 处理prometheus返回类型为“vector”的响应格式
 */
@NoArgsConstructor
public class PrometheusVectorParser extends AbstractPrometheusParse {
    @Override
    public Boolean checkType(String responseStr) {
        try {
            PromVectorOrMatrix promVectorOrMatrix = GsonUtil.fromJson(responseStr, PromVectorOrMatrix.class);
            if (DispatchConstants.PARSE_PROMETHEUS_VECTOR.equals(promVectorOrMatrix.getData().getResultType())) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void parse(String resp, List<String> aliasFields, HttpProtocol http, CollectRep.MetricsData.Builder builder) {
        boolean setTimeFlag = false;
        boolean setValueFlag = false;
        PromVectorOrMatrix promVectorOrMatrix = GsonUtil.fromJson(resp, PromVectorOrMatrix.class);
        List<PromVectorOrMatrix.Result> result = promVectorOrMatrix.getData().getResult();
        for (PromVectorOrMatrix.Result r : result) {
            CollectRep.ValueRow.Builder valueRowBuilder = CollectRep.ValueRow.newBuilder();
            for (String aliasField : aliasFields) {
                if (!CollectUtil.assertPromRequireField(aliasField)) {
                    JsonElement jsonElement = r.getMetric().get(aliasField);
                    if (jsonElement != null) {
                        valueRowBuilder.addColumns(jsonElement.getAsString());
                    } else {
                        valueRowBuilder.addColumns(CommonConstants.NULL_VALUE);
                    }
                } else {
                    if (CommonConstants.PROM_TIME.equals(aliasField)) {
                        for (Object o : r.getValue()) {
                            if (o instanceof Double) {
                                valueRowBuilder.addColumns(String.valueOf(new BigDecimal((Double) o * 1000)));
                                setTimeFlag = true;
                            }
                        }
                        if (!setTimeFlag) {
                            valueRowBuilder.addColumns(CommonConstants.NULL_VALUE);
                        }
                    } else {
                        for (Object o : r.getValue()) {
                            if (o instanceof String) {
                                valueRowBuilder.addColumns((String) o);
                                setValueFlag = true;
                            }
                        }
                        if (!setValueFlag) {
                            valueRowBuilder.addColumns(CommonConstants.NULL_VALUE);
                        }
                    }
                }
            }
            builder.addValues(valueRowBuilder);
        }
    }
}