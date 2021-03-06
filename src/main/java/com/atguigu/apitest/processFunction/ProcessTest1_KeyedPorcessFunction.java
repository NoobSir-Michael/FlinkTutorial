package com.atguigu.apitest.processFunction;

import com.atguigu.apitest.beans.SensorReading;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class ProcessTest1_KeyedPorcessFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String filepath = "E:\\Java\\work_space\\FlinkTutorial\\src\\main\\resources\\hello.txt";
        DataStreamSource<String> inputStream = env.readTextFile(filepath);

        SingleOutputStreamOperator<SensorReading> dataStream = inputStream.map(line -> {
            String[] fields = line.split(",");
            return new SensorReading(fields[0], new Long(fields[1]), new Double(fields[2]));
        });

        dataStream.keyBy("id").process(new MyKeyedProcessFunction()).print();


        env.execute();
    }

    public static class MyKeyedProcessFunction extends KeyedProcessFunction<Tuple, SensorReading, Object>{

        private ValueState<Long> TimerState;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            TimerState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("timer-state", Long.class));
        }

        @Override
        public void processElement(
                SensorReading sensorReading,
                KeyedProcessFunction<Tuple, SensorReading, Object>.Context context,
                Collector<Object> collector) throws Exception {
            context.getCurrentKey();
//            context.output(); // ?????????????????????
            context.timerService().currentWatermark();// ?????????????????????
            TimerState.update(sensorReading.getTimestamp()*1000+10*1000);
            context.timerService().registerEventTimeTimer(TimerState.value()); // ???????????????
            context.timerService().deleteEventTimeTimer(TimerState.value()); // ????????????????????????????????????
        }

        @Override
        public void onTimer(long timestamp, KeyedProcessFunction<Tuple, SensorReading, Object>.OnTimerContext ctx, Collector<Object> out) throws Exception {
            super.onTimer(timestamp, ctx, out);
            // ??????????????????????????????????????????
        }
    }
}
