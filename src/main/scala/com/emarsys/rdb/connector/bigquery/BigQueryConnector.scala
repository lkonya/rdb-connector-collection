package com.emarsys.rdb.connector.bigquery

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.emarsys.rdb.connector.common.ConnectorResponse
import com.emarsys.rdb.connector.common.models.Errors.TableNotFound
import com.emarsys.rdb.connector.common.models._
import com.emarsys.rdb.connector.bigquery.BigQueryConnector.BigQueryConnectionConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class BigQueryConnector(protected val actorSystem: ActorSystem, val config: BigQueryConnectionConfig)
                       (implicit val executionContext: ExecutionContext)
  extends Connector
    with BigQueryWriter
    with BigQuerySimpleSelect {

  implicit val sys: ActorSystem = actorSystem
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(3.seconds)

  protected def handleNotExistingTable[T](table: String): PartialFunction[Throwable, ConnectorResponse[T]] = {
    case e: Exception if e.getMessage.contains("doesn't exist") =>
      Future.successful(Left(TableNotFound(table)))
  }

  override def close(): Future[Unit] = Future.successful()

  override def testConnection(): ConnectorResponse[Unit] = ???

  override def listTables(): ConnectorResponse[Seq[TableSchemaDescriptors.TableModel]] = ???

  override def listTablesWithFields(): ConnectorResponse[Seq[TableSchemaDescriptors.FullTableModel]] = ???

  override def listFields(table: String): ConnectorResponse[Seq[TableSchemaDescriptors.FieldModel]] = ???

  override def isOptimized(table: String, fields: Seq[String]): ConnectorResponse[Boolean] = ???

  override def rawSelect(rawSql: String, limit: Option[Int]): ConnectorResponse[Source[Seq[String], NotUsed]] = ???

  override def validateRawSelect(rawSql: String): ConnectorResponse[Unit] = ???

  override def analyzeRawSelect(rawSql: String): ConnectorResponse[Source[Seq[String], NotUsed]] = ???

  override def projectedRawSelect(rawSql: String, fields: Seq[String]): ConnectorResponse[Source[Seq[String], NotUsed]] = ???
}

object BigQueryConnector extends BigQueryConnectorTrait {

  case class BigQueryConnectionConfig(
                                       projectId: String,
                                       dataset: String,
                                       clientEmail: String,
                                       privateKey: String,
                                     ) extends ConnectionConfig

}

trait BigQueryConnectorTrait extends ConnectorCompanion {

  def apply(config: BigQueryConnectionConfig)(actorSystem: ActorSystem): ConnectorResponse[BigQueryConnector] = {
    Future.successful(Right(new BigQueryConnector(actorSystem, config)(actorSystem.dispatcher)))
  }

  override def meta() = MetaData("`", "'", "\\")
}
