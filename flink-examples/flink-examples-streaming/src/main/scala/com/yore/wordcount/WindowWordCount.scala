package com.yore.wordcount

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

/**
  *
  * Created by yore on 2018/12/10 17:07
  */
object WindowWordCount {

  def main(args: Array[String]): Unit = {
    val params = ParameterTool.fromArgs(args)

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val text =
      if(params.has("input")){
        env.readTextFile(params.get("input"))
      }else{
        println("Executing WindowWordCount example with default input data set.")
        println("Use --input to specify file input.")
        // get default test text data
        env.fromElements(WordCountData.WORDS: _*)
      }

    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    val windowSize = params.getInt("window", 250)
    val slideSize = params.getInt("slide", 150)

    val counts: DataStream[(String, Int)] = text
      // split up the lines in pairs (2-tuple) containing: (word,1)
      .flatMap(_.toLowerCase.split("\\W+"))
      .filter(_.nonEmpty)
      .map((_, 1))
      .keyBy(0)
      // create windows of windowSize records slided every slideSize records
      /*.countWindow(windowSize, slideSize)*/
      .countWindow(250, 150)
      // group by the tuple field "0" and sum up tuple field "1"
      .sum(1)

    // emit result
    if (params.has("output")) {
      counts.writeAsText(params.get("output"))
    } else {
      println("Printing result to stdout. Use --output to specify output path.")
      counts.print()
    }

    // execute program
    env.execute("WindowWordCount")

  }

}
