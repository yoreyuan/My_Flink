package yore;

import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * COUNT 统计的聚合函数实现，每出现一条记录加一
 *
 * Created by yore on 2018/12/11 14:46
 */
public class CountAgg implements AggregateFunction<HotItems.UserBehavior, Long, Long> {
    @Override
    public Long createAccumulator() {
        return 0L;
    }

    @Override
    public Long add(HotItems.UserBehavior userBehavior, Long acc) {
        return acc + 1;
    }

    @Override
    public Long getResult(Long acc) {
        return acc;
    }

    @Override
    public Long merge(Long acc1, Long acc2) {
        return acc1 + acc2;
    }
}
