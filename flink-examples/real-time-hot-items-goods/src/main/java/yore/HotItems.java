package yore;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.io.PojoCsvInputFormat;
import org.apache.flink.api.java.typeutils.PojoTypeInfo;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.File;
import java.net.URL;

/**
 * <pre>
 *    抽取出业务时间戳，告诉Flink框架基于业务时间做窗口
 *    过滤出点击行为数据
 *    按一小时的窗口大小，每5分钟统计一次，做滑动窗口聚合（Sliding Window）
 *    按每个窗口聚合，输出每个窗口中点击量前N名的商品
 *
 *    <b>Event Time与Watermark:</b>
 *    当我们说"统计过去一小时内的点击量"，这里的一小时是指的什么呢？
 *    在Flink中它可以是指ProcessingTime，也可以是EventTime，由用户决定
 *
 *    ProcessingTime: 时间被处理的时间，也就是有机器的系统时间决定。
 *    EventTime: 时间发生的时间。一般就是数据本身携带的时间。
 *
 *    <b>Watermark</b>
 *    Watermark是用来跟踪事务事件的概念，可以理解成EventTime世界中的时钟，用来指示当期那处理到什么时刻的数据了。
 *    因为我们的测试数据源的数据已经经过了整理，没有乱序，即事件的时间戳是单调递增的，所以可以将每条数据的业务事件当做watermark.
 *
 *
 *
 * </pre>
 *
 * Created by yore on 2018/12/11 13:49
 */
public class HotItems {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 为了打印到控制台的结果不乱序，我们配置全局并发为1，这里改变并发对结果正确性没有影响
        env.setParallelism(1);

        /**
         * 读取一个CSV文件，并将每一行转换成指定的POJO类型的输入器
         *
         * Created by yore on 2018/12/11 13:58
         */
        // UserBehavior.csv 的本地文件路径
        URL fileUrl = HotItems.class.getClassLoader().getResource("UserBehavior.csv");
        Path filePath = Path.fromLocalFile(new File(fileUrl.toURI()));

        // 抽取 UserBehavior 的 TypeInformation，是一个 PojoTypeInfo
        PojoTypeInfo<UserBehavior> pojoType = (PojoTypeInfo<UserBehavior>)
                TypeExtractor.createTypeInfo(UserBehavior.class);
        // 由于Java反射抽取出来的字段顺序是不正确的，需要显示指定下文件中字段的顺序
        String[] fieldOrder = new String[]{"userId", "itemId", "categoryId", "behavior", "timestamp"};
        // 创建PojoCsvInputFormat
        PojoCsvInputFormat<UserBehavior> csvInput = new PojoCsvInputFormat<>(filePath, pojoType, fieldOrder);


        // 用 PojoCsvInputFormat 创建输入源. 创建数据源，得到 UserBehavior 类型的 DataStream
        DataStream<UserBehavior> dataSource = env.createInput(csvInput, pojoType);
        // 告诉Flink现在的业务是按照EventTime模式进行处理。
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);


        // 这里我们用 AscendingTimestampExtractor 来实现时间戳的抽取和 Watermark 的生成
        // 真实业务场景一般是存在乱序的，所以一般使用BoundedOrdernessTimestampExtractor
        DataStream<UserBehavior> timeData = dataSource.assignTimestampsAndWatermarks(
                new AscendingTimestampExtractor<UserBehavior>() {
                    @Override
                    public long extractAscendingTimestamp(UserBehavior element) {
                        // 原始数据单位秒，将其转换成毫秒
                        return element.timestamp * 1000;
                    }
                }
        );

        timeData
                // 过滤点击事件 - 因为现在是每5分钟统计过去一小时的点击事件的Top N
                .filter(new FilterFunction<UserBehavior>() {
                    @Override
                    public boolean filter(UserBehavior userBehavior) throws Exception {
                        // 过滤出只有点击的数据
                        return "pv".equals(userBehavior.behavior) ;
                    }
                })
                .keyBy("itemId")
                .timeWindow(Time.minutes(60), Time.minutes(5))
                .aggregate(new CountAgg(), new WindowResultFunction())
                .keyBy("windowEnd")
                .process(new TopNHotItems(3))
                .print();

        env.execute("Hot Items Job");
    }


    /** 商品点击量(窗口操作的输出类型) */
    public static class ItemViewCount {
        public long itemId;     // 商品ID
        public long windowEnd;  // 窗口结束时间戳
        public long viewCount;  // 商品的点击量

        public static ItemViewCount of(long itemId, long windowEnd, long viewCount) {
            ItemViewCount result = new ItemViewCount();
            result.itemId = itemId;
            result.windowEnd = windowEnd;
            result.viewCount = viewCount;
            return result;
        }
    }

    public static class UserBehavior {
        public long userId; // 用户 ID
        public long itemId; // 商品 ID
        public int categoryId; // 商品类目 ID
        public String behavior; // 用户行为，包括（"pv", "buy", "cart", "fav"）
        public long timestamp;  // 行为发生的时间戳，单位秒

    }

}

