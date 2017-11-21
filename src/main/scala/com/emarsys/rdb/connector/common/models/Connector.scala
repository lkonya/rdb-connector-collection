package com.emarsys.rdb.connector.common.models

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.emarsys.rdb.connector.common.ConnectorResponse
import com.emarsys.rdb.connector.common.models.TableSchemaDescriptors._

import scala.concurrent.Future

trait Connector {

  def close(): Future[Unit]

  def testConnection(): ConnectorResponse[Unit]

  def listTables(): ConnectorResponse[Seq[TableModel]]

  def listTablesWithFields(): ConnectorResponse[Seq[FullTableModel]]

  def listFields(table: String): ConnectorResponse[Seq[FieldModel]]

  def simpleSelect(select: SimpleSelect): ConnectorResponse[Source[Seq[String], NotUsed]]

  def isOptimized(table: String, fields: Seq[String]): ConnectorResponse[Boolean] = ???

}
