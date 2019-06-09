package com.yore.wordcount

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._

/**
  *
  * Created by yore on 2018/12/10 16:12
  */
object WordCount {

  def main(args: Array[String]): Unit = {

    // checking input parameters
    val params = ParameterTool.fromArgs(args)

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    // get input data
    val text =
      if(params.has("input")){
        env.readTextFile(params.get("input"))
      }else{
        println("Executing WordCount example with default inputs data set.")
        println("Use --input to specify file input.")

        // get default test text data
        env.fromElements(WordCountData.WORDS : _*)
      }

    val counts : DataStream[(String, Int)] = text
      .flatMap(_.toLowerCase.split("\\W+"))
      .filter(_.nonEmpty)
      .map((_,1))
      .keyBy(0)
      .sum(1)

    if(params.has("output")){
      counts.writeAsText(params.get("output"))
    }else{
      println("Printing result to stdout. Use --output to specify output path.")
      counts.print().setParallelism(3)

    }

    env.execute("Streaming WordCount")


  }

  /*def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val input = env.fromElements(
      new WC("Hello", 1)
    )
    input.keyBy("word").minBy("frequency").print()

    env.execute("Streaming WordCount")
  }
  case class WC(str: String, i: Int)*/

}
