package com.emarsys.rdb.connector.redshift

import java.sql.Types

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.emarsys.rdb.connector.common.ConnectorResponse
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, PositionedResult}

import com.emarsys.rdb.connector.common._
import scala.concurrent.Future
import scala.concurrent.duration._

trait RedshiftStreamingQuery {
  self: RedshiftConnector =>

  protected def streamingQuery(
      timeout: FiniteDuration
  )(query: String): ConnectorResponse[Source[Seq[String], NotUsed]] = {
    val sql = sql"#$query"
      .as(resultConverter)
      .transactionally
      .withStatementParameters(
        fetchSize = connectorConfig.streamChunkSize,
        statementInit = _.setQueryTimeout(timeout.toSeconds.toInt)
      )

    val publisher = db.stream(sql)
    val dbSource = Source
      .fromPublisher(publisher)
      .statefulMapConcat { () =>
        var first = true
        (data: (Seq[String], Seq[String])) =>
          if (first) {
            first = false
            List(data._1, data._2)
          } else {
            List(data._2)
          }
      }
      .recoverWithRetries(1, streamErrorHandler)

    Future.successful(Right(dbSource))
  }

  private val resultConverter: GetResult[(Seq[String], Seq[String])] = GetResult[(Seq[String], Seq[String])] { r =>
    val rowData = getRowData(r)
    val headers = getHeaders(r)
    (headers, rowData)
  }

  private def getRowData(result: PositionedResult): Seq[String] = {
    val columnTypes = (1 to result.numColumns).map(result.rs.getMetaData.getColumnType(_))

    (0 until result.numColumns).map { i =>
      columnTypes(i) match {
        case Types.TIMESTAMP => parseDateTime(result.nextString())
        case Types.BIT       => parseBoolean(result.nextString())
        case _               => result.nextString()
      }
    }
  }

  private def getHeaders(r: PositionedResult): Seq[String] =
    (1 to r.numColumns).map(r.rs.getMetaData.getColumnName(_))

  private def parseDateTime(column: String): String = Option(column) match {
    case Some(s) => s.split('.').headOption.getOrElse("")
    case None    => null
  }

  private def parseBoolean(column: String): String = Option(column) match {
    case Some("true")  => "1"
    case Some("false") => "0"
    case _             => null
  }
}
