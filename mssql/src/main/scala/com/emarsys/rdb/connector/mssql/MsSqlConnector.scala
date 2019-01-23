package com.emarsys.rdb.connector.mssql

import java.util.UUID

import cats.data.EitherT
import com.emarsys.rdb.connector.common.ConnectorResponse
import com.emarsys.rdb.connector.common.models.Errors.{ConnectionConfigError, ConnectorError}
import com.emarsys.rdb.connector.common.models.{CommonConnectionReadableData, ConnectionConfig, MetaData}
import com.emarsys.rdb.connector.common.models._
import com.emarsys.rdb.connector.mssql.CertificateUtil.createTrustStoreTempFile
import com.emarsys.rdb.connector.mssql.MsSqlConnector.{MsSqlConnectionConfig, MsSqlConnectorConfig}
import com.typesafe.config.ConfigValueFactory.fromAnyRef
import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.SQLServerProfile.api._

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MsSqlConnector(
    protected val db: Database,
    protected val connectorConfig: MsSqlConnectorConfig,
    protected val poolName: String
)(
    implicit val executionContext: ExecutionContext
) extends Connector
    with MsSqlErrorHandling
    with MsSqlTestConnection
    with MsSqlMetadata
    with MsSqlSimpleSelect
    with MsSqlRawSelect
    with MsSqlIsOptimized
    with MsSqlRawDataManipulation {

  override protected val fieldValueConverters = MsSqlFieldValueConverters

  override def close(): Future[Unit] = db.shutdown

  override def innerMetrics(): String = {
    import java.lang.management.ManagementFactory

    import com.zaxxer.hikari.HikariPoolMXBean
    import javax.management.{JMX, ObjectName}
    Try {
      val mBeanServer    = ManagementFactory.getPlatformMBeanServer
      val poolObjectName = new ObjectName(s"com.zaxxer.hikari:type=Pool ($poolName)")
      val poolProxy      = JMX.newMXBeanProxy(mBeanServer, poolObjectName, classOf[HikariPoolMXBean])

      s"""{
         |"activeConnections": ${poolProxy.getActiveConnections},
         |"idleConnections": ${poolProxy.getIdleConnections},
         |"threadAwaitingConnections": ${poolProxy.getThreadsAwaitingConnection},
         |"totalConnections": ${poolProxy.getTotalConnections}
         |}""".stripMargin
    }.getOrElse(super.innerMetrics())
  }

}

object MsSqlConnector extends MsSqlConnectorTrait {

  case class MsSqlConnectionConfig(
      host: String,
      port: Int,
      dbName: String,
      dbUser: String,
      dbPassword: String,
      certificate: String,
      connectionParams: String
  ) extends ConnectionConfig {

    override def toCommonFormat: CommonConnectionReadableData = {
      CommonConnectionReadableData("mssql", s"$host:$port", dbName, dbUser)
    }
  }

  case class MsSqlConnectorConfig(
      configPath: String,
      trustServerCertificate: Boolean
  )

}

trait MsSqlConnectorTrait extends ConnectorCompanion with MsSqlErrorHandling with MsSqlConnectorHelper {
  import cats.instances.future._
  import cats.syntax.functor._

  val defaultConfig = MsSqlConnectorConfig(
    configPath = "mssqldb",
    trustServerCertificate = true
  )

  override def meta(): MetaData = MetaData("\"", "'", "'")

  def create(
      config: MsSqlConnectionConfig,
      connectorConfig: MsSqlConnectorConfig = defaultConfig
  )(implicit e: ExecutionContext): ConnectorResponse[MsSqlConnector] = {
    if (isSslDisabledOrTamperedWith(config.connectionParams)) {
      Future.successful(Left(ConnectionConfigError("SSL Error")))
    } else {
      (for {
        trustStorePath <- createTrustStorePath(config.certificate)
        poolName = UUID.randomUUID.toString
        dbConfig = createDbConfig(config, poolName, connectorConfig, trustStorePath)
        database = Database.forConfig("", dbConfig)
        mySqlConnector <- createMsSqlConnector(connectorConfig, poolName, database)
      } yield mySqlConnector).value
    }
  }

  private def createTrustStorePath(cert: String)(implicit e: ExecutionContext): EitherT[Future, ConnectorError, String] = {
    EitherT
      .fromOption[Future](
        createTrustStoreTempFile(cert),
        ConnectionConfigError("Wrong SSL cert format"): ConnectorError
      )
  }

  private def createMsSqlConnector(connectorConfig: MsSqlConnectorConfig, poolName: String, db: Database)(
      implicit ec: ExecutionContext
  ): EitherT[Future, ConnectorError, MsSqlConnector] = {
    EitherT(
      checkConnection(db)
        .as(Right(new MsSqlConnector(db, connectorConfig, poolName)))
        .recover(eitherErrorHandler())
    ).leftMap { connectorError =>
      db.shutdown
      connectorError
    }
  }

  private def createDbConfig(
      config: MsSqlConnectionConfig,
      poolName: String,
      connectorConfig: MsSqlConnectorConfig,
      trustStorePath: String
  ): Config = {
    val jdbcUrl = createUrl(config.host, config.port, config.dbName, config.connectionParams)
    ConfigFactory
      .load()
      .getConfig(connectorConfig.configPath)
      .withValue("poolName", fromAnyRef(poolName))
      .withValue("registerMbeans", fromAnyRef(true))
      .withValue("properties.url", fromAnyRef(jdbcUrl))
      .withValue("properties.user", fromAnyRef(config.dbUser))
      .withValue("properties.password", fromAnyRef(config.dbPassword))
      .withValue("properties.driver", fromAnyRef("slick.jdbc.SQLServerProfile"))
      .withValue("properties.properties.encrypt", fromAnyRef(true))
      .withValue(
        "properties.properties.trustServerCertificate",
        fromAnyRef(connectorConfig.trustServerCertificate)
      )
      .withValue("properties.properties.trustStore", fromAnyRef(trustStorePath))
  }
}