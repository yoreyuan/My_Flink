package yore.log1

/**
  *
  * Created by yore on 2018/6/9 12:22
  */
case class Record(
                  bizName: String ,    // 业务名字
                  bizID: Int,         // 业务方id
                  timestamp: Long ,    // 日志生产时间
                  attr: Int,          // 属性
                  data: String       // 原始属性描述
                  )
