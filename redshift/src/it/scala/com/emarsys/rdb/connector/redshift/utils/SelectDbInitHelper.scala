package com.emarsys.rdb.connector.redshift.utils

import com.emarsys.rdb.connector.common.models.Connector
import com.emarsys.rdb.connector.redshift.RedshiftConnector

import concurrent.duration._
import scala.concurrent.Await

trait SelectDbInitHelper {

  import scala.concurrent.ExecutionContext.Implicits.global

  val aTableName: String
  val bTableName: String

  val connector: Connector =
    Await.result(RedshiftConnector.create(TestHelper.TEST_CONNECTION_CONFIG), 5.seconds).right.get

  def initDb(): Unit = {
    val createATableSql =
      s"""CREATE TABLE "$aTableName" (
         |    A1 varchar(255) NOT NULL UNIQUE,
         |    A2 int,
         |    A3 boolean,
         |    PRIMARY KEY (A1)
         |);""".stripMargin

    val createBTableSql =
      s"""CREATE TABLE "$bTableName" (
         |    B1 varchar(255) NOT NULL,
         |    B2 varchar(255) NOT NULL,
         |    B3 varchar(255) NOT NULL,
         |    B4 varchar(255)
         |);""".stripMargin

    val insertADataSql =
      s"""INSERT INTO "$aTableName" (A1,A2,A3) VALUES
         |('v1', 1, 1),
         |('v2', 2, 0),
         |('v3', 3, 1),
         |('v4', -4, 0),
         |('v5', NULL, 0),
         |('v6', 6, NULL),
         |('v7', NULL, NULL)
         |;""".stripMargin

    val insertBDataSql =
      s"""INSERT INTO "$bTableName" (B1,B2,B3,B4) VALUES
         |('b,1', 'b.1', 'b:1', 'b"1'),
         |('b;2', 'b\\\\2', 'b\\'2', 'b=2'),
         |('b!3', 'b@3', 'b#3', NULL),
         |('b$$4', 'b%4', 'b 4', NULL)
         |;""".stripMargin

    Await.result(
      for {
        _ <- TestHelper.executeQuery(createATableSql)
        _ <- TestHelper.executeQuery(createBTableSql)
        _ <- TestHelper.executeQuery(insertADataSql)
        _ <- TestHelper.executeQuery(insertBDataSql)
      } yield (),
      15.seconds
    )
  }

  def cleanUpDb(): Unit = {
    val dropATableSql = s"""DROP TABLE "$aTableName";"""
    val dropBTableSql = s"""DROP TABLE "$bTableName";"""
    Await.result(for {
      _ <- TestHelper.executeQuery(dropATableSql)
      _ <- TestHelper.executeQuery(dropBTableSql)
    } yield (), 15.seconds)
  }
}
