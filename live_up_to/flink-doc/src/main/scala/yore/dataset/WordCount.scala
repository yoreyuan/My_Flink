package yore.dataset

import org.apache.flink.api.scala._

/**
  *
  * Created by yore on 2019/5/5 03:16
  */
object WordCount {

  def main(args: Array[String]): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment

    val text = env.fromElements(
      "Who's there?",
      "I think I hear them. Stand, ho! Who's there?")

    // read text file from local files system
//    val localLines = env.readTextFile("file:///path/to/my/textfile")

    // read text file from a HDFS running at nnHost:nnPort
//    val hdfsLines = env.readTextFile("hdfs://cdh3:8020/path/to/my/textfile")

    // read a CSV file with three fields
//    val csvInput = env.readCsvFile[(Int, String, Double)]("hdfs:///the/CSV/file")

    // read a CSV file with five fields, taking only two of them
//    val csvInput = env.readCsvFile[(String, Double)](
//      "hdfs:///the/CSV/file",
//      includedFields = Array(0, 3)) // take the first and the fourth field

    // CSV input can also be used with Case Classes
//    case class MyCaseClass(str: String, dbl: Double)
//    val csvInput = env.readCsvFile[MyCaseClass](
//      "hdfs:///the/CSV/file",
//      includedFields = Array(0, 3)) // take the first and the fourth field

    // read a CSV file with three fields into a POJO (Person) with corresponding fields
//    val csvInput = env.readCsvFile[Person](
//      "hdfs:///the/CSV/file",
//      pojoFields = Array("name", "age", "zipcode"))

    // generate a number sequence
//    val numbers = env.generateSequence(1, 10000000)




    val counts = text.flatMap { _.toLowerCase.split("\\W+") filter { _.nonEmpty } }
      .map { (_, 1) }
      .groupBy(0)
      .sum(1)

    counts.print()

  }

}
