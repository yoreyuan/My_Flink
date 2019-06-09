package yore.table

import java.sql.Date
import java.util.Calendar

import org.apache.flink.table.functions.ScalarFunction

/**
  * 给定日期当月的最后一天
  *   tableEnv.registerFunction("last_day", new LastDay())
  *
  */
class LastDay extends ScalarFunction{
  def eval(d1: Date): Date = {
    val calendar = Calendar.getInstance
    calendar.setTime(d1)
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    new Date(calendar.getTimeInMillis)
  }
}
