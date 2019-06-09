package yore;

import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

/**
 * 用于输出窗口的结果
 *
 * Created by yore on 2018/12/11 14:51
 */
public class WindowResultFunction implements WindowFunction<Long, HotItems.ItemViewCount, Tuple, TimeWindow> {

    /**
     *
     * @param key 窗口的主键，即itemId
     * @param window 窗口
     * @param aggregateResult 聚合函数的结果，即count值
     * @param collector 输出类型为ItemViewCount
     */
    @Override
    public void apply(Tuple key, TimeWindow window, Iterable<Long> aggregateResult, Collector<HotItems.ItemViewCount> collector) throws Exception {
        Long itemId = ((Tuple1<Long>) key).f0;
        Long count = aggregateResult.iterator().next();
        collector.collect(HotItems.ItemViewCount.of(itemId, window.getEnd(), count));
    }

}
